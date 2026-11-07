package be.ae.springfactory.stats;

import static org.assertj.core.api.Assertions.assertThat;

import be.ae.springfactory.log.LoggedSession;
import be.ae.springfactory.log.LoggedSet;
import be.ae.springfactory.program.Prescription;
import be.ae.springfactory.program.Session;
import be.ae.springfactory.program.Week;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The pure Volume and Adherence calculation, {@link WeekStats#of}. */
class WeekStatsTest {

  private static final UUID SWING = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01");
  private static final UUID SNATCH = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c05");
  private static final UUID PUSH_UP = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06");
  private static final LocalDate DAY = LocalDate.of(2026, 11, 7);

  @Test
  void aWeekWithoutSessionsHasNoVolumeAndNoAdherence() {
    WeekStats stats = WeekStats.of(week(), List.of());

    assertThat(stats.plannedVolume()).isEqualByComparingTo("0");
    assertThat(stats.loggedVolume()).isEqualByComparingTo("0");
    assertThat(stats.adherence()).isNull();
  }

  @Test
  void aWeekWithNothingLoggedHasZeroAdherence() {
    Session session = session(prescription(SWING, 10, 10, "24"));

    WeekStats stats = WeekStats.of(week(session), List.of());

    assertThat(stats.plannedVolume().toPlainString()).isEqualTo("2400");
    assertThat(stats.loggedVolume().toPlainString()).isEqualTo("0");
    assertThat(stats.adherence().toPlainString()).isEqualTo("0");
  }

  @Test
  void plannedVolumeIsSetsTimesRepsTimesBellWeightSummedOverTheWeek() {
    Session monday = session(prescription(SWING, 10, 10, "24"), prescription(SNATCH, 5, 5, "16"));
    Session friday = session(prescription(SWING, 3, 5, "10.50"));

    WeekStats stats = WeekStats.of(week(monday, friday), List.of());

    // 2400 + 400 + 157.50
    assertThat(stats.plannedVolume().toPlainString()).isEqualTo("2957.5");
  }

  @Test
  void loggedVolumeIsRepsTimesBellWeightAndCountsUnprescribedExercises() {
    Session session = session(prescription(SWING, 10, 10, "24"));
    LoggedSession log =
        logged(session, set(SWING, 10, "24"), set(SWING, 10, "28"), set(SNATCH, 5, "16"));

    WeekStats stats = WeekStats.of(week(session), List.of(log));

    assertThat(stats.loggedVolume().toPlainString()).isEqualTo("600");
  }

  @Test
  void bodyweightSetsAddNoVolumeButTheirSessionsCountForAdherence() {
    Session kettlebell = session(prescription(SWING, 10, 10, "24"));
    Session bodyweight = session(new Prescription(PUSH_UP, 5, 20, null, null));
    LoggedSession log = logged(bodyweight, new LoggedSet(PUSH_UP, 20, null), set(SWING, 10, "16"));

    WeekStats stats = WeekStats.of(week(kettlebell, bodyweight), List.of(log));

    assertThat(stats.plannedVolume().toPlainString()).isEqualTo("2400");
    assertThat(stats.loggedVolume().toPlainString()).isEqualTo("160");
    assertThat(stats.adherence().toPlainString()).isEqualTo("50");
    assertThat(stats.sessions().get(1).plannedVolume().toPlainString()).isEqualTo("0");
    assertThat(stats.sessions().get(1).logged()).isTrue();
  }

  @Test
  void partialLoggingGivesAdherenceAsAPercentageRoundedHalfUpToTwoDecimals() {
    Session a = session(prescription(SWING, 1, 1, "16"));
    Session b = session(prescription(SWING, 1, 1, "16"));
    Session c = session(prescription(SWING, 1, 1, "16"));

    assertThat(
            WeekStats.of(week(a, b, c), List.of(logged(a, set(SWING, 1, "16"))))
                .adherence()
                .toPlainString())
        .isEqualTo("33.33");
    assertThat(
            WeekStats.of(
                    week(a, b, c),
                    List.of(logged(a, set(SWING, 1, "16")), logged(b, set(SWING, 1, "16"))))
                .adherence()
                .toPlainString())
        .isEqualTo("66.67");
    assertThat(
            WeekStats.of(week(a, b), List.of(logged(a, set(SWING, 1, "16"))))
                .adherence()
                .toPlainString())
        .isEqualTo("50");
  }

  @Test
  void aFullyLoggedWeekIsAHundredPercentWrittenPlainly() {
    Session a = session(prescription(SWING, 10, 10, "20.00"));

    WeekStats stats = WeekStats.of(week(a), List.of(logged(a, set(SWING, 100, "20.00"))));

    assertThat(stats.adherence()).hasToString("100");
    assertThat(stats.plannedVolume()).hasToString("2000");
    assertThat(stats.loggedVolume()).hasToString("2000");
  }

  @Test
  void logsOfSessionsOutsideTheWeekAreIgnored() {
    Session inWeek = session(prescription(SWING, 1, 1, "16"));
    Session elsewhere = session(prescription(SWING, 1, 1, "16"));

    WeekStats stats =
        WeekStats.of(week(inWeek), List.of(logged(elsewhere, set(SWING, 10, "32"))));

    assertThat(stats.loggedVolume()).hasToString("0");
    assertThat(stats.adherence()).hasToString("0");
  }

  @Test
  void eachSessionHasItsPlannedVolumeAndWhetherItWasLogged() {
    Session logged = session(prescription(SWING, 10, 10, "24.00"));
    Session open = session(prescription(SWING, 3, 5, "10.50"), prescription(SNATCH, 1, 1, "16"));

    WeekStats stats =
        WeekStats.of(week(logged, open), List.of(logged(logged, set(SWING, 1, "16"))));

    assertThat(stats.number()).isEqualTo(1);
    assertThat(stats.sessions()).hasSize(2);
    assertThat(stats.sessions().get(0).sessionId()).isEqualTo(logged.id());
    assertThat(stats.sessions().get(0).plannedVolume()).hasToString("2400");
    assertThat(stats.sessions().get(0).logged()).isTrue();
    assertThat(stats.sessions().get(1).sessionId()).isEqualTo(open.id());
    assertThat(stats.sessions().get(1).plannedVolume()).hasToString("173.5");
    assertThat(stats.sessions().get(1).logged()).isFalse();
  }

  private static Week week(Session... sessions) {
    return new Week(UUID.randomUUID(), 1, List.of(sessions));
  }

  private static Session session(Prescription... prescriptions) {
    return new Session(UUID.randomUUID(), List.of(prescriptions));
  }

  private static Prescription prescription(UUID exercise, int sets, int reps, String bellKg) {
    return new Prescription(exercise, sets, reps, new BigDecimal(bellKg), null);
  }

  private static LoggedSession logged(Session session, LoggedSet... sets) {
    return new LoggedSession(session.id(), DAY, List.of(sets));
  }

  private static LoggedSet set(UUID exercise, int reps, String bellKg) {
    return new LoggedSet(exercise, reps, new BigDecimal(bellKg));
  }
}
