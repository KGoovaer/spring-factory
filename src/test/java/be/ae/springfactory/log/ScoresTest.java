package be.ae.springfactory.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.ae.springfactory.exercise.ExerciseCatalog;
import be.ae.springfactory.exercise.ExerciseCatalogs;
import be.ae.springfactory.program.Part;
import be.ae.springfactory.program.Prescription;
import be.ae.springfactory.program.Programs;
import be.ae.springfactory.program.Side;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The Score of an AMRAP Part in a Logged Session, through {@link LoggedSessions}. */
class ScoresTest {

  private static final UUID SWING = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01");
  private static final UUID PUSH_UP = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06");
  private static final UUID WAITER_PRESS =
      UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c09");
  private static final BigDecimal BELL = new BigDecimal("16");
  private static final LocalDate DAY = LocalDate.of(2026, 11, 11);

  private final ExerciseCatalog catalog = ExerciseCatalogs.seeded();
  private final Programs programs = new Programs(catalog);
  private final LoggedSessions loggedSessions = new LoggedSessions(programs, catalog);

  @Test
  void anAmrapIsLoggedAsRoundsAndExtraRepsAndCountsTheirVolume() {
    UUID sessionId = amrapSession();

    LoggedSession logged =
        loggedSessions.log(sessionId, DAY, List.of(), List.of(new Score(1, 4, 13)));

    assertThat(loggedSessions.get(sessionId).scores()).containsExactly(new Score(1, 4, 13));
    // A Round is 10 swings and 5+5 waiter presses at 16 kg and 5 push-ups: 320 kg.
    // 4 Rounds is 1280 kg; 13 extra Reps are 10 swings and 3 waiter press Reps: 208 kg.
    assertThat(logged.volume(programs.session(sessionId))).isEqualByComparingTo("1488");
  }

  @Test
  void aPlannedAmrapCountsOneRound() {
    assertThat(programs.session(amrapSession()).volume()).isEqualByComparingTo("320");
  }

  @Test
  void aScoreAddsToTheVolumeOfTheSetsAndIsCorrectedWithThem() {
    UUID sessionId = amrapSession();
    loggedSessions.log(sessionId, DAY, List.of(), List.of(new Score(1, 1, 0)));

    LoggedSession corrected =
        loggedSessions.correct(
            sessionId, DAY, List.of(new LoggedSet(SWING, 10, BELL)), List.of(new Score(1, 2, 0)));

    assertThat(corrected.scores()).containsExactly(new Score(1, 2, 0));
    assertThat(corrected.volume(programs.session(sessionId))).isEqualByComparingTo("800");
  }

  @Test
  void extraRepsThatMakeAWholeRoundAreRejected() {
    UUID sessionId = amrapSession();

    // A Round has 10 + 5 + 5 + 5 = 25 Reps.
    assertThatThrownBy(
            () -> loggedSessions.log(sessionId, DAY, List.of(), List.of(new Score(1, 3, 25))))
        .isInstanceOf(InvalidLoggedSessionException.class)
        .hasMessageContaining("count it as a Round");
    assertThat(loggedSessions.find(sessionId)).isEmpty();
  }

  @Test
  void onlyAnAmrapPartTakesAScoreAndOnlyOnce() {
    UUID sessionId = amrapSession();
    programs.addPart(sessionId, Part.rounds(3));

    for (List<Score> scores :
        List.of(
            List.of(new Score(2, 3, 0)),
            List.of(new Score(3, 1, 0)),
            List.of(new Score(1, 1, 0), new Score(1, 2, 0)))) {
      assertThatThrownBy(() -> loggedSessions.log(sessionId, DAY, List.of(), scores))
          .isInstanceOf(InvalidLoggedSessionException.class);
    }
    assertThatThrownBy(() -> new Score(1, -1, 0))
        .isInstanceOf(InvalidLoggedSessionException.class);
    assertThatThrownBy(() -> loggedSessions.log(sessionId, DAY, List.of(), List.of()))
        .isInstanceOf(InvalidLoggedSessionException.class)
        .hasMessageContaining("at least one Set or Score");
  }

  private UUID amrapSession() {
    UUID sessionId =
        programs.addSession(programs.addWeek(programs.create("AMRAP").id()).id()).id();
    programs.addPart(sessionId, Part.amrap(12));
    programs.addPrescription(sessionId, 1, new Prescription(SWING, 1, 10, BELL, null));
    programs.addPrescription(
        sessionId, 1, new Prescription(WAITER_PRESS, 1, 5, Side.EACH, null, null, BELL, null, null));
    programs.addPrescription(sessionId, 1, new Prescription(PUSH_UP, 1, 5, null, null));
    return sessionId;
  }
}
