package be.ae.springfactory.stats;

import be.ae.springfactory.log.LoggedSession;
import be.ae.springfactory.log.LoggedSessions;
import be.ae.springfactory.program.Program;
import be.ae.springfactory.program.Programs;
import be.ae.springfactory.program.Session;
import be.ae.springfactory.program.Week;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Derives {@link WeekStats} for a Week, or for every Week of a Program, from what {@link Programs} plans and {@link LoggedSessions}
 * records. Reads both, changes neither.
 */
@Component
public class WeekStatistics {

  private final Programs programs;
  private final LoggedSessions loggedSessions;

  public WeekStatistics(Programs programs, LoggedSessions loggedSessions) {
    this.programs = programs;
    this.loggedSessions = loggedSessions;
  }

  /**
   * Volume and Adherence of the Week. Throws {@link be.ae.springfactory.program.WeekNotFoundException}
   * for an unknown Week.
   */
  public WeekStats of(UUID weekId) {
    return of(programs.week(weekId));
  }

  /**
   * Volume and Adherence of every Week of the Program, in Week order, each with the planned Volume
   * and logged state of its Sessions. Throws {@link
   * be.ae.springfactory.program.ProgramNotFoundException} for an unknown Program.
   */
  public ProgramStats ofProgram(UUID programId) {
    Program program = programs.get(programId);
    return new ProgramStats(program.id(), program.weeks().stream().map(this::of).toList());
  }

  private WeekStats of(Week week) {
    List<LoggedSession> logged =
        week.sessions().stream()
            .map(Session::id)
            .map(loggedSessions::find)
            .flatMap(Optional::stream)
            .toList();
    return WeekStats.of(week, logged);
  }
}
