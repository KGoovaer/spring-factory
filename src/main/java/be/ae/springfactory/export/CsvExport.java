package be.ae.springfactory.export;

import be.ae.springfactory.exercise.Exercise;
import be.ae.springfactory.exercise.ExerciseCatalog;
import be.ae.springfactory.log.LoggedSession;
import be.ae.springfactory.log.LoggedSessions;
import be.ae.springfactory.log.LoggedSet;
import be.ae.springfactory.log.Score;
import be.ae.springfactory.program.Part;
import be.ae.springfactory.program.Prescription;
import be.ae.springfactory.program.Program;
import be.ae.springfactory.program.Programs;
import be.ae.springfactory.program.Session;
import be.ae.springfactory.program.Side;
import be.ae.springfactory.program.Week;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Everything the Lifters planned and did, as CSV for a spreadsheet: every Program, in the order of
 * {@link Programs#all()}, with its Weeks and Sessions in order. A Session is named by its number in
 * its Week, from 1. Planned and logged data are never mixed, so each has its own document.
 *
 * <p>Columns hold one value each, so a spreadsheet can sum and filter them: a Rep range is a Reps
 * column and a Reps max column (3-4 is Reps 3, Reps max 4), a Score is its Rounds and its Extra
 * reps (5 + 7 is Rounds 5, Extra reps 7), a Side is Left, Right or Each (Each is 8+8: the Reps are
 * done on each side), Bell weights are in kilograms and times in whole seconds. An empty cell means
 * none. Volume and Adherence are derived, so they are left out. See {@link Csv} for the format.
 */
@Component
public class CsvExport {

  private static final List<String> PLANNED_HEADINGS =
      List.of(
          "Program",
          "Week",
          "Session",
          "Notes",
          "Part",
          "Format",
          "Rounds",
          "Minutes",
          "Rotation (min)",
          "Part rest (s)",
          "Exercise",
          "Sets",
          "Reps",
          "Reps max",
          "Side",
          "Hold time (s)",
          "Work time (s)",
          "Bell (kg)",
          "Rest (s)",
          "Minute");

  private static final List<String> LOGGED_HEADINGS =
      List.of(
          "Program",
          "Week",
          "Session",
          "Date",
          "Session time (s)",
          "Exercise",
          "Reps",
          "Side",
          "Hold time (s)",
          "Work time (s)",
          "Bell (kg)",
          "Part",
          "Rounds",
          "Extra reps");

  private final Programs programs;
  private final LoggedSessions loggedSessions;
  private final ExerciseCatalog exercises;

  public CsvExport(Programs programs, LoggedSessions loggedSessions, ExerciseCatalog exercises) {
    this.programs = programs;
    this.loggedSessions = loggedSessions;
    this.exercises = exercises;
  }

  /**
   * Every planned Prescription, one line each, under the Program, Week, Session (with its notes)
   * and Part it is in. A Part without Prescriptions (such as rest), a Session without Parts, a Week
   * without Sessions and a Program without Weeks get one line of their own, with the cells below
   * them empty, so nothing planned is missing.
   */
  public String planned() {
    Map<UUID, String> names = exerciseNames();
    Csv csv = new Csv(PLANNED_HEADINGS);
    for (Program program : programs.all()) {
      if (program.weeks().isEmpty()) {
        csv.line(Arrays.asList(program.name()));
      }
      for (Week week : program.weeks()) {
        if (week.sessions().isEmpty()) {
          csv.line(Arrays.asList(program.name(), week.number()));
        }
        for (int s = 0; s < week.sessions().size(); s++) {
          Session session = week.sessions().get(s);
          List<Object> sessionCells =
              Arrays.asList(program.name(), week.number(), s + 1, session.notes());
          if (session.parts().isEmpty()) {
            csv.line(sessionCells);
          }
          for (int p = 0; p < session.parts().size(); p++) {
            plannedPart(csv, sessionCells, p + 1, session.parts().get(p), names);
          }
        }
      }
    }
    return csv.toString();
  }

  /**
   * Every Logged Session, under the Program, Week and Session it performed, with its date and
   * Session time: one line per Set, in the order done, then one line per Score with its Part
   * number. A Logged Session is listed in the order of its planned Session, not by date.
   */
  public String logged() {
    Map<UUID, String> names = exerciseNames();
    Csv csv = new Csv(LOGGED_HEADINGS);
    for (Program program : programs.all()) {
      for (Week week : program.weeks()) {
        for (int s = 0; s < week.sessions().size(); s++) {
          int number = s + 1;
          loggedSessions
              .find(week.sessions().get(s).id())
              .ifPresent(logged -> loggedLines(csv, program, week, number, logged, names));
        }
      }
    }
    return csv.toString();
  }

  private static void plannedPart(
      Csv csv, List<Object> sessionCells, int number, Part part, Map<UUID, String> names) {
    List<Object> partCells =
        concat(
            sessionCells,
            Arrays.asList(
                number,
                title(part.format().name()),
                part.rounds(),
                part.minutes(),
                part.rotation(),
                part.restSeconds()));
    if (part.prescriptions().isEmpty()) {
      csv.line(partCells);
    }
    for (Prescription prescription : part.prescriptions()) {
      csv.line(
          concat(
              partCells,
              Arrays.asList(
                  names.get(prescription.exerciseId()),
                  prescription.sets(),
                  prescription.reps(),
                  prescription.repsMax(),
                  side(prescription.side()),
                  prescription.holdSeconds(),
                  prescription.workSeconds(),
                  prescription.bellKg(),
                  prescription.restSeconds(),
                  prescription.minute())));
    }
  }

  private static void loggedLines(
      Csv csv,
      Program program,
      Week week,
      int sessionNumber,
      LoggedSession logged,
      Map<UUID, String> names) {
    List<Object> sessionCells =
        Arrays.asList(
            program.name(), week.number(), sessionNumber, logged.date(), logged.sessionSeconds());
    for (LoggedSet set : logged.sets()) {
      csv.line(
          concat(
              sessionCells,
              Arrays.asList(
                  names.get(set.exerciseId()),
                  set.reps(),
                  side(set.side()),
                  set.holdSeconds(),
                  set.workSeconds(),
                  set.bellKg())));
    }
    for (Score score : logged.scores()) {
      csv.line(
          concat(
              sessionCells,
              Arrays.asList(
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  score.part(),
                  score.rounds(),
                  score.extraReps())));
    }
  }

  private Map<UUID, String> exerciseNames() {
    return exercises.all().stream().collect(Collectors.toMap(Exercise::id, Exercise::name));
  }

  private static String side(Side side) {
    return side == null ? null : title(side.name());
  }

  /** LEFT as Left, EMOM as EMOM: an abbreviation keeps its capitals. */
  private static String title(String name) {
    return switch (name) {
      case "EMOM", "AMRAP" -> name;
      default -> name.charAt(0) + name.substring(1).toLowerCase();
    };
  }

  private static List<Object> concat(List<Object> first, List<Object> second) {
    return Stream.of(first, second)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }
}
