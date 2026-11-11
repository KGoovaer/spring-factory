package be.ae.springfactory.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.ae.springfactory.exercise.ExerciseCatalog;
import be.ae.springfactory.exercise.ExerciseCatalogs;
import be.ae.springfactory.exercise.ExerciseNotFoundException;
import be.ae.springfactory.program.Prescription;
import be.ae.springfactory.program.Programs;
import be.ae.springfactory.program.Session;
import be.ae.springfactory.program.SessionNotFoundException;
import be.ae.springfactory.program.Side;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Logging planned Sessions as performed, through {@link LoggedSessions}. */
class LoggedSessionsTest {

  private static final UUID SWING = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01");
  private static final UUID SNATCH = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c05");
  private static final UUID PLANK = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c07");
  private static final UUID SIT_UP = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c08");
  private static final LocalDate DAY = LocalDate.of(2026, 11, 7);

  private final ExerciseCatalog catalog = ExerciseCatalogs.seeded();
  private final Programs programs = new Programs(catalog);
  private final LoggedSessions loggedSessions = new LoggedSessions(programs, catalog);

  @Test
  void aLoggedSessionKeepsItsDateAndSetsInOrder() {
    UUID sessionId = newSession().id();
    List<LoggedSet> sets =
        List.of(
            new LoggedSet(SWING, 10, new BigDecimal("24")),
            new LoggedSet(SWING, 8, new BigDecimal("10.50")));

    LoggedSession logged = loggedSessions.log(sessionId, DAY, sets);

    assertThat(logged).isEqualTo(new LoggedSession(sessionId, DAY, sets));
    assertThat(loggedSessions.get(sessionId)).isEqualTo(logged);
    assertThat(loggedSessions.get(sessionId).sets().get(1).bellKg().toPlainString())
        .isEqualTo("10.50");
  }

  @Test
  void loggedSessionsUseTheExercisesOfTheirSets() {
    loggedSessions.log(newSession().id(), DAY, oneSet());

    assertThat(loggedSessions.uses(SWING)).isTrue();
    assertThat(loggedSessions.uses(SNATCH)).isFalse();
  }

  @Test
  void aSetOnEachSideCountsItsRepsTwiceInVolume() {
    LoggedSession logged =
        loggedSessions.log(
            newSession().id(),
            DAY,
            List.of(
                new LoggedSet(SWING, 8, Side.EACH, null, null, new BigDecimal("16")),
                new LoggedSet(SWING, 8, Side.LEFT, null, null, new BigDecimal("16"))));

    assertThat(logged.volume(programs.session(logged.sessionId()))).isEqualByComparingTo("384");
  }

  @Test
  void findingALogOfAnUnloggedSessionGivesNothingInsteadOfThrowing() {
    UUID sessionId = newSession().id();

    assertThat(loggedSessions.find(sessionId)).isEmpty();

    LoggedSession logged =
        loggedSessions.log(sessionId, DAY, List.of(new LoggedSet(SWING, 10, new BigDecimal("24"))));
    assertThat(loggedSessions.find(sessionId)).contains(logged);
    assertThatThrownBy(() -> loggedSessions.find(UUID.randomUUID()))
        .isInstanceOf(SessionNotFoundException.class);
  }

  @Test
  void loggingLeavesThePlannedSessionAsItWas() {
    Session session = newSession();
    Prescription swings = new Prescription(SWING, 10, 10, new BigDecimal("24"), 60);
    programs.addPrescription(session.id(), swings);
    Session planned = programs.session(session.id());

    loggedSessions.log(session.id(), DAY, List.of(new LoggedSet(SNATCH, 5, new BigDecimal("16"))));

    assertThat(programs.session(session.id())).isEqualTo(planned);
    assertThat(planned.prescriptions()).containsExactly(swings);
  }

  @Test
  void aLifterMayLogExercisesTheSessionDidNotPrescribe() {
    Session session = newSession();
    programs.addPrescription(
        session.id(), new Prescription(SWING, 10, 10, new BigDecimal("24"), null));

    LoggedSession logged =
        loggedSessions.log(
            session.id(), DAY, List.of(new LoggedSet(SNATCH, 5, new BigDecimal("16"))));

    assertThat(logged.sets().getFirst().exerciseId()).isEqualTo(SNATCH);
  }

  @Test
  void anyDateIsAllowedIncludingTheFuture() {
    UUID sessionId = newSession().id();
    LocalDate future = LocalDate.now().plusYears(1);

    assertThat(loggedSessions.log(sessionId, future, oneSet()).date()).isEqualTo(future);
  }

  @Test
  void aSessionIsLoggedAtMostOnce() {
    UUID sessionId = newSession().id();
    LoggedSession first = loggedSessions.log(sessionId, DAY, oneSet());

    assertThatThrownBy(() -> loggedSessions.log(sessionId, DAY.plusDays(1), oneSet()))
        .isInstanceOf(SessionAlreadyLoggedException.class);
    assertThat(loggedSessions.get(sessionId)).isEqualTo(first);
  }

  @Test
  void aLifterCorrectsALoggedSessionByReplacingItsDateAndSets() {
    UUID sessionId = newSession().id();
    loggedSessions.log(sessionId, DAY, oneSet());
    List<LoggedSet> fixed =
        List.of(
            new LoggedSet(SWING, 8, new BigDecimal("24")),
            new LoggedSet(SIT_UP, 12, null));

    LoggedSession corrected = loggedSessions.correct(sessionId, DAY.minusDays(1), fixed);

    assertThat(corrected).isEqualTo(new LoggedSession(sessionId, DAY.minusDays(1), fixed));
    assertThat(loggedSessions.get(sessionId)).isEqualTo(corrected);
  }

  @Test
  void aLoggedSessionKeepsTheSessionTimeTheLifterTimedAndACorrectionReplacesIt() {
    UUID sessionId = newSession().id();

    LoggedSession logged = loggedSessions.log(sessionId, DAY, oneSet(), List.of(), 2_710);

    assertThat(logged.sessionSeconds()).isEqualTo(2_710);
    assertThat(loggedSessions.get(sessionId).sessionSeconds()).isEqualTo(2_710);
    assertThat(loggedSessions.correct(sessionId, DAY, oneSet(), List.of(), 2_800).sessionSeconds())
        .isEqualTo(2_800);
    assertThat(loggedSessions.correct(sessionId, DAY, oneSet()).sessionSeconds()).isNull();
  }

  @Test
  void aSessionTimeIsNeverBelowZeroAndARejectedOneStoresNothing() {
    UUID sessionId = newSession().id();

    assertThatThrownBy(() -> loggedSessions.log(sessionId, DAY, oneSet(), List.of(), -1))
        .isInstanceOf(InvalidLoggedSessionException.class)
        .hasMessage("A Session time is never below 0 seconds");
    assertThat(loggedSessions.find(sessionId)).isEmpty();
    assertThat(loggedSessions.log(sessionId, DAY, oneSet(), List.of(), 0).sessionSeconds())
        .isZero();
  }

  @Test
  void onlyALoggedSessionCanBeCorrected() {
    UUID sessionId = newSession().id();

    assertThatThrownBy(() -> loggedSessions.correct(sessionId, DAY, oneSet()))
        .isInstanceOf(LoggedSessionNotFoundException.class);
    assertThat(loggedSessions.find(sessionId)).isEmpty();
    assertThatThrownBy(() -> loggedSessions.correct(UUID.randomUUID(), DAY, oneSet()))
        .isInstanceOf(SessionNotFoundException.class);
  }

  @Test
  void aRejectedCorrectionKeepsTheEarlierLoggedSession() {
    UUID sessionId = newSession().id();
    LoggedSession first = loggedSessions.log(sessionId, DAY, oneSet());

    assertThatThrownBy(
            () ->
                loggedSessions.correct(
                    sessionId, DAY, List.of(new LoggedSet(SWING, 10, null))))
        .isInstanceOf(InvalidLoggedSessionException.class)
        .hasMessage("Swing needs a Bell weight");
    assertThatThrownBy(
            () ->
                loggedSessions.correct(
                    sessionId, DAY, List.of(new LoggedSet(UUID.randomUUID(), 1, BigDecimal.ONE))))
        .isInstanceOf(ExerciseNotFoundException.class);
    assertThat(loggedSessions.get(sessionId)).isEqualTo(first);
  }

  @Test
  void anUnknownSessionCannotBeLoggedOrRead() {
    UUID unknown = UUID.randomUUID();

    assertThatThrownBy(() -> loggedSessions.log(unknown, DAY, oneSet()))
        .isInstanceOf(SessionNotFoundException.class);
    assertThatThrownBy(() -> loggedSessions.get(unknown))
        .isInstanceOf(SessionNotFoundException.class);
  }

  @Test
  void aSessionThatWasNotLoggedHasNoLoggedSession() {
    UUID sessionId = newSession().id();

    assertThatThrownBy(() -> loggedSessions.get(sessionId))
        .isInstanceOf(LoggedSessionNotFoundException.class);
  }

  @Test
  void aSetWithAnUnknownExerciseIsRejectedAndNothingIsStored() {
    UUID sessionId = newSession().id();
    List<LoggedSet> sets =
        List.of(
            new LoggedSet(SWING, 10, BigDecimal.TEN),
            new LoggedSet(UUID.randomUUID(), 10, BigDecimal.TEN));

    assertThatThrownBy(() -> loggedSessions.log(sessionId, DAY, sets))
        .isInstanceOf(ExerciseNotFoundException.class);
    assertThatThrownBy(() -> loggedSessions.get(sessionId))
        .isInstanceOf(LoggedSessionNotFoundException.class);
  }

  @Test
  void bodyweightSetsAreLoggedWithoutABellAndAddNoVolume() {
    UUID sessionId = newSession().id();
    List<LoggedSet> sets =
        List.of(
            new LoggedSet(SWING, 10, new BigDecimal("24")),
            new LoggedSet(SIT_UP, 20, null),
            new LoggedSet(PLANK, 1, 60, null));

    LoggedSession logged = loggedSessions.log(sessionId, DAY, sets);

    assertThat(loggedSessions.get(sessionId).sets()).isEqualTo(sets);
    assertThat(logged.sets().get(1).volume()).isZero();
    assertThat(logged.volume(programs.session(logged.sessionId()))).isEqualByComparingTo("240");
  }

  @Test
  void aSetWhoseBellDoesNotFitTheExerciseIsRejectedAndNothingIsStored() {
    UUID sessionId = newSession().id();

    assertThatThrownBy(
            () -> loggedSessions.log(sessionId, DAY, List.of(new LoggedSet(SWING, 10, null))))
        .isInstanceOf(InvalidLoggedSessionException.class)
        .hasMessage("Swing needs a Bell weight");
    List<LoggedSet> plankWithBell =
        List.of(new LoggedSet(SWING, 10, BigDecimal.TEN), new LoggedSet(PLANK, 1, BigDecimal.TEN));
    assertThatThrownBy(() -> loggedSessions.log(sessionId, DAY, plankWithBell))
        .isInstanceOf(InvalidLoggedSessionException.class)
        .hasMessage("Plank is a bodyweight Exercise and takes no Bell, was 10 kg");
    assertThat(loggedSessions.find(sessionId)).isEmpty();
  }

  @Test
  void aPlankIsLoggedWithAHoldTimeAndOnlyAPlankHasOne() {
    UUID sessionId = newSession().id();

    assertThatThrownBy(
            () -> loggedSessions.log(sessionId, DAY, List.of(new LoggedSet(PLANK, 1, null))))
        .isInstanceOf(InvalidLoggedSessionException.class)
        .hasMessage("Plank is a hold and needs a hold time");
    assertThatThrownBy(
            () ->
                loggedSessions.log(
                    sessionId, DAY, List.of(new LoggedSet(SWING, 10, 30, BigDecimal.TEN))))
        .isInstanceOf(InvalidLoggedSessionException.class)
        .hasMessage("Swing is not a hold and takes no hold time, was 30 s");
    assertThat(loggedSessions.find(sessionId)).isEmpty();

    LoggedSession logged =
        loggedSessions.log(sessionId, DAY, List.of(new LoggedSet(PLANK, 2, 45, null)));

    assertThat(logged.sets().getFirst().holdSeconds()).isEqualTo(45);
    assertThat(logged.volume(programs.session(logged.sessionId()))).isZero();
  }

  @Test
  void aLoggedSessionNeedsADateAndAtLeastOneSet() {
    UUID sessionId = newSession().id();
    List<LoggedSet> withNull = new ArrayList<>();
    withNull.add(null);

    assertThatThrownBy(() -> loggedSessions.log(sessionId, null, oneSet()))
        .isInstanceOf(InvalidLoggedSessionException.class);
    assertThatThrownBy(() -> loggedSessions.log(sessionId, DAY, List.of()))
        .isInstanceOf(InvalidLoggedSessionException.class);
    assertThatThrownBy(() -> loggedSessions.log(sessionId, DAY, null))
        .isInstanceOf(InvalidLoggedSessionException.class);
    assertThatThrownBy(() -> loggedSessions.log(sessionId, DAY, withNull))
        .isInstanceOf(InvalidLoggedSessionException.class);
    assertThatThrownBy(() -> loggedSessions.get(sessionId))
        .isInstanceOf(LoggedSessionNotFoundException.class);
  }

  @Test
  void aLoggedSetKeepsItsLimits() {
    assertThatThrownBy(() -> new LoggedSet(null, 1, BigDecimal.ONE))
        .isInstanceOf(InvalidLoggedSessionException.class);
    assertThatThrownBy(() -> new LoggedSet(SWING, 0, BigDecimal.ONE))
        .isInstanceOf(InvalidLoggedSessionException.class);
    assertThatThrownBy(() -> new LoggedSet(SWING, 1, BigDecimal.ZERO))
        .isInstanceOf(InvalidLoggedSessionException.class);
    assertThatThrownBy(() -> new LoggedSet(SWING, 1, new BigDecimal("-4")))
        .isInstanceOf(InvalidLoggedSessionException.class);
    assertThatThrownBy(() -> new LoggedSet(PLANK, 1, 0, null))
        .isInstanceOf(InvalidLoggedSessionException.class)
        .hasMessage("Hold time must be at least 1 s, was 0");
  }

  @Test
  void callersCannotChangeTheLoggedSets() {
    UUID sessionId = newSession().id();
    List<LoggedSet> sets = new ArrayList<>(oneSet());
    LoggedSession logged = loggedSessions.log(sessionId, DAY, sets);
    sets.clear();

    assertThat(loggedSessions.get(sessionId).sets()).hasSize(1);
    assertThatThrownBy(() -> logged.sets().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void whenASessionIsLoggedConcurrentlyOnlyOneWins() throws Exception {
    UUID sessionId = newSession().id();
    List<Callable<LoggedSession>> logs =
        IntStream.range(0, 50)
            .<Callable<LoggedSession>>mapToObj(
                i -> () -> loggedSessions.log(sessionId, DAY.plusDays(i), oneSet()))
            .toList();

    int wins = 0;
    int conflicts = 0;
    ExecutorService pool = Executors.newFixedThreadPool(8);
    try {
      for (Future<LoggedSession> future : pool.invokeAll(logs)) {
        try {
          future.get();
          wins++;
        } catch (ExecutionException e) {
          assertThat(e.getCause()).isInstanceOf(SessionAlreadyLoggedException.class);
          conflicts++;
        }
      }
    } finally {
      pool.shutdown();
    }
    assertThat(wins).isEqualTo(1);
    assertThat(conflicts).isEqualTo(49);
  }

  private Session newSession() {
    return programs.addSession(programs.addWeek(programs.create("Giant").id()).id());
  }

  private static List<LoggedSet> oneSet() {
    return List.of(new LoggedSet(SWING, 10, new BigDecimal("24")));
  }
}
