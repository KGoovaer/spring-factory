package be.ae.springfactory.export;

import static org.assertj.core.api.Assertions.assertThat;

import be.ae.springfactory.exercise.ExerciseCatalog;
import be.ae.springfactory.exercise.ExerciseCatalogs;
import be.ae.springfactory.log.LoggedSessions;
import be.ae.springfactory.log.LoggedSet;
import be.ae.springfactory.log.Score;
import be.ae.springfactory.program.Part;
import be.ae.springfactory.program.Prescription;
import be.ae.springfactory.program.Programs;
import be.ae.springfactory.program.Side;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Exporting what was planned and logged as CSV, through {@link CsvExport}. */
class CsvExportTest {

  private static final UUID SWING = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01");
  private static final UUID SNATCH = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c05");
  private static final UUID PUSH_UP = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06");
  private static final UUID PLANK = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c07");
  private static final UUID SKIPPING = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0c");
  private static final String PLANNED_HEADINGS =
      "Program,Week,Session,Notes,Part,Format,Rounds,Minutes,Rotation (min),Part rest (s),"
          + "Exercise,Sets,Reps,Reps max,Side,Hold time (s),Work time (s),Bell (kg),Rest (s),Minute";
  private static final String LOGGED_HEADINGS =
      "Program,Week,Session,Date,Session time (s),Exercise,Reps,Side,Hold time (s),Work time (s),"
          + "Bell (kg),Part,Rounds,Extra reps";

  /**
   * A line of the planned CSV: these cells, then empty ones up to its 20 columns. Counts the commas
   * of {@code cells}, so a cell must not hold one.
   */
  private static String planned(String cells) {
    return cells + ",".repeat(19 - (int) cells.chars().filter(c -> c == ',').count());
  }

  private final ExerciseCatalog catalog = ExerciseCatalogs.seeded();
  private final Programs programs = new Programs(catalog);
  private final LoggedSessions loggedSessions = new LoggedSessions(programs, catalog);
  private final CsvExport export = new CsvExport(programs, loggedSessions, catalog);

  @Test
  void withNothingPlannedOrLoggedOnlyTheHeadingsAreExported() {
    assertThat(export.planned()).isEqualTo(PLANNED_HEADINGS + "\r\n");
    assertThat(export.logged()).isEqualTo(LOGGED_HEADINGS + "\r\n");
  }

  @Test
  void eachPlannedPrescriptionIsALineUnderItsProgramWeekSessionAndPart() {
    UUID programId = programs.create("Giant").id();
    UUID weekId = programs.addWeek(programId).id();
    programs.addSession(weekId);
    UUID sessionId = programs.addSession(weekId).id();
    programs.addPrescription(
        sessionId,
        new Prescription(SWING, 5, 10, 12, Side.EACH, null, null, new BigDecimal("24.50"), 60, null));
    programs.addPart(sessionId, Part.rest(90));
    programs.addPart(sessionId, Part.emom(10, 2));
    programs.addPrescription(
        sessionId, 3, new Prescription(SKIPPING, 1, 1, null, null, 30, null, null, 2));
    programs.addPart(sessionId, Part.rounds(3));
    programs.addPrescription(
        sessionId, 4, new Prescription(PLANK, 1, 1, null, 45, null, null, null, null));
    programs.addWeek(programId);

    assertThat(export.planned().split("\r\n"))
        .containsExactly(
            PLANNED_HEADINGS,
            planned("Giant,1,1"),
            "Giant,1,2,,1,Sets,,,,,Swing,5,10,12,Each,,,24.50,60,",
            planned("Giant,1,2,,2,Rest,,,,90"),
            "Giant,1,2,,3,EMOM,,10,2,,Skipping,1,1,,,,30,,,2",
            "Giant,1,2,,4,Rounds,3,,,,Plank,1,1,,,45,,,,",
            planned("Giant,2"));
  }

  @Test
  void programsAreExportedByNameAndAProgramWithoutWeeksStillShows() {
    programs.create("simple");
    programs.create("Armour building");

    assertThat(export.planned().split("\r\n"))
        .containsExactly(PLANNED_HEADINGS, planned("Armour building"), planned("simple"));
  }

  @Test
  void eachLoggedSetIsALineWithItsDateAndSessionTimeThenEachScore() {
    UUID programId = programs.create("Giant").id();
    UUID weekId = programs.addWeek(programId).id();
    UUID unlogged = programs.addSession(weekId).id();
    programs.addPrescription(unlogged, new Prescription(SWING, 1, 10, new BigDecimal("24"), null));
    UUID sessionId = programs.addSession(weekId).id();
    programs.addPart(sessionId, Part.amrap(10));
    programs.addPrescription(
        sessionId, 1, new Prescription(SNATCH, 1, 5, new BigDecimal("16"), null));
    programs.addPrescription(sessionId, 1, new Prescription(PUSH_UP, 1, 10, null, null));
    loggedSessions.log(
        sessionId,
        LocalDate.of(2026, 11, 7),
        List.of(
            new LoggedSet(SNATCH, 5, Side.LEFT, null, null, new BigDecimal("16")),
            new LoggedSet(PUSH_UP, 10, null)),
        List.of(new Score(1, 5, 7)),
        754);

    assertThat(export.logged().split("\r\n"))
        .containsExactly(
            LOGGED_HEADINGS,
            "Giant,1,2,2026-11-07,754,Snatch,5,Left,,,16,,,",
            "Giant,1,2,2026-11-07,754,Push-up,10,,,,,,,",
            "Giant,1,2,2026-11-07,754,,,,,,,1,5,7");
  }

  @Test
  void aNameIsQuotedWhenItHoldsACommaOrQuoteAndKeptAsTextWhenItLooksLikeAFormula() {
    programs.create("Strength, \"heavy\"");
    programs.create("=HYPERLINK(\"x\")");
    programs.create("-5 kg");

    assertThat(export.planned().split("\r\n"))
        .containsExactly(
            PLANNED_HEADINGS,
            planned("'-5 kg"),
            planned("\"'=HYPERLINK(\"\"x\"\")\""),
            "\"Strength, \"\"heavy\"\"\"" + ",".repeat(19));
  }

  @Test
  void theNotesOfASessionAreExportedOnEachOfItsLines() {
    UUID programId = programs.create("Giant").id();
    UUID sessionId = programs.addSession(programs.addWeek(programId).id()).id();
    programs.changeNotes(sessionId, "Heavy, \"slow\"\nScale down: 16 kg");
    programs.addPrescription(sessionId, new Prescription(SWING, 1, 10, new BigDecimal("24"), null));

    assertThat(export.planned())
        .isEqualTo(
            PLANNED_HEADINGS
                + "\r\nGiant,1,1,\"Heavy, \"\"slow\"\"\nScale down: 16 kg\","
                + "1,Sets,,,,,Swing,1,10,,,,,24,,\r\n");
  }

  @Test
  void aRenamedExerciseIsExportedByItsNewName() {
    UUID programId = programs.create("Giant").id();
    UUID sessionId = programs.addSession(programs.addWeek(programId).id()).id();
    programs.addPrescription(sessionId, new Prescription(SWING, 1, 10, new BigDecimal("24"), null));
    catalog.rename(SWING, "Two-hand swing");

    assertThat(export.planned()).contains(",Two-hand swing,");
  }
}
