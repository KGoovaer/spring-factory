package be.ae.springfactory.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.ae.springfactory.exercise.ExerciseCatalog;
import be.ae.springfactory.log.LoggedSessions;
import be.ae.springfactory.log.LoggedSet;
import be.ae.springfactory.program.Prescription;
import be.ae.springfactory.program.Programs;
import be.ae.springfactory.program.WeekNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Week stats read from real {@link Programs} and {@link LoggedSessions}. */
class WeekStatisticsTest {

  private static final UUID SWING = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01");

  private final ExerciseCatalog catalog = new ExerciseCatalog();
  private final Programs programs = new Programs(catalog);
  private final LoggedSessions loggedSessions = new LoggedSessions(programs, catalog);
  private final WeekStatistics statistics = new WeekStatistics(programs, loggedSessions);

  @Test
  void readsPlannedAndLoggedDataOfTheWeek() {
    UUID programId = programs.create("Giant").id();
    UUID weekId = programs.addWeek(programId).id();
    UUID logged = programs.addSession(weekId).id();
    UUID skipped = programs.addSession(weekId).id();
    programs.addPrescription(logged, new Prescription(SWING, 10, 10, new BigDecimal("24"), null));
    programs.addPrescription(skipped, new Prescription(SWING, 5, 10, new BigDecimal("24"), null));
    loggedSessions.log(
        logged, LocalDate.of(2026, 11, 7), List.of(new LoggedSet(SWING, 50, new BigDecimal("24"))));
    UUID otherWeek = programs.addWeek(programId).id();
    programs.addSession(otherWeek);

    WeekStats stats = statistics.of(weekId);

    assertThat(stats.weekId()).isEqualTo(weekId);
    assertThat(stats.plannedVolume()).hasToString("3600");
    assertThat(stats.loggedVolume()).hasToString("1200");
    assertThat(stats.adherence()).hasToString("50");
    assertThat(statistics.of(otherWeek).adherence()).hasToString("0");
  }

  @Test
  void anUnknownWeekIsNotFound() {
    assertThatThrownBy(() -> statistics.of(UUID.randomUUID()))
        .isInstanceOf(WeekNotFoundException.class);
  }
}
