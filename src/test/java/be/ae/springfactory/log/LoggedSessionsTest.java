package be.ae.springfactory.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.ae.springfactory.exercise.ExerciseCatalog;
import be.ae.springfactory.exercise.ExerciseNotFoundException;
import be.ae.springfactory.program.Prescription;
import be.ae.springfactory.program.Programs;
import be.ae.springfactory.program.Session;
import be.ae.springfactory.program.SessionNotFoundException;
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
  private static final LocalDate DAY = LocalDate.of(2026, 11, 7);

  private final ExerciseCatalog catalog = new ExerciseCatalog();
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
    assertThatThrownBy(() -> new LoggedSet(SWING, 1, null))
        .isInstanceOf(InvalidLoggedSessionException.class);
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
