package be.ae.springfactory.log;

import be.ae.springfactory.exercise.ExerciseCatalog;
import be.ae.springfactory.program.Programs;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * What Lifters actually did for their planned Sessions: at most one Logged Session per Session.
 * Kept in memory and lost on restart, like {@link Programs}. Planned data is only read, never
 * changed. Safe for concurrent use: when two callers log the same Session at once, only one wins.
 */
@Component
public class LoggedSessions {

  private final Map<UUID, LoggedSession> logs = new ConcurrentHashMap<>();
  private final Programs programs;
  private final ExerciseCatalog exercises;

  public LoggedSessions(Programs programs, ExerciseCatalog exercises) {
    this.programs = programs;
    this.exercises = exercises;
  }

  /**
   * Logs the Session as performed on {@code date} with these Sets, and returns the Logged Session.
   * The Exercises need not be the prescribed ones, but each must be in the catalog. Throws {@link
   * be.ae.springfactory.program.SessionNotFoundException} for an unknown Session, {@link
   * be.ae.springfactory.exercise.ExerciseNotFoundException} for an unknown Exercise, {@link
   * InvalidLoggedSessionException} for a missing date or no Sets, and {@link
   * SessionAlreadyLoggedException} when the Session has a Logged Session already.
   */
  public LoggedSession log(UUID sessionId, LocalDate date, List<LoggedSet> sets) {
    programs.session(sessionId);
    LoggedSession logged = new LoggedSession(sessionId, date, sets);
    logged.sets().forEach(set -> exercises.get(set.exerciseId()));
    if (logs.putIfAbsent(sessionId, logged) != null) {
      throw new SessionAlreadyLoggedException(sessionId);
    }
    return logged;
  }

  /**
   * The Logged Session of this Session. Throws {@link
   * be.ae.springfactory.program.SessionNotFoundException} for an unknown Session and {@link
   * LoggedSessionNotFoundException} when the Session has not been logged.
   */
  public LoggedSession get(UUID sessionId) {
    return find(sessionId).orElseThrow(() -> new LoggedSessionNotFoundException(sessionId));
  }

  /**
   * The Logged Session of this Session, or empty when the Session has not been logged. Throws {@link
   * be.ae.springfactory.program.SessionNotFoundException} for an unknown Session.
   */
  public Optional<LoggedSession> find(UUID sessionId) {
    programs.session(sessionId);
    return Optional.ofNullable(logs.get(sessionId));
  }
}
