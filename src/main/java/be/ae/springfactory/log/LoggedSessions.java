package be.ae.springfactory.log;

import be.ae.springfactory.exercise.Exercise;
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
 * What Lifters actually did for their planned Sessions: at most one Logged Session per Session,
 * which the Lifter may correct by replacing it. Kept in memory and lost on restart, like {@link
 * Programs}. Planned data is only read, never changed. Safe for concurrent use: when two callers
 * log the same Session at once, only one wins.
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
   * InvalidLoggedSessionException} for a missing date, no Sets, a kettlebell Exercise without a
   * Bell, a bodyweight Exercise with one, a hold Exercise without a hold time or another Exercise
   * with one, a timed Exercise without a work time or another Exercise with one, and {@link
   * SessionAlreadyLoggedException} when the Session has a Logged Session already.
   */
  public LoggedSession log(UUID sessionId, LocalDate date, List<LoggedSet> sets) {
    LoggedSession logged = validated(sessionId, date, sets);
    if (logs.putIfAbsent(sessionId, logged) != null) {
      throw new SessionAlreadyLoggedException(sessionId);
    }
    return logged;
  }

  /**
   * Corrects the Logged Session of this Session: replaces its date and all its Sets with these, and
   * returns the corrected Logged Session. The Sets follow the same rules as in {@link #log}, and a
   * rejected correction leaves the earlier Logged Session as it was. Throws what {@link #log}
   * throws, except that {@link LoggedSessionNotFoundException} replaces {@link
   * SessionAlreadyLoggedException}: only a Session that was logged can be corrected.
   */
  public LoggedSession correct(UUID sessionId, LocalDate date, List<LoggedSet> sets) {
    LoggedSession corrected = validated(sessionId, date, sets);
    if (logs.replace(sessionId, corrected) == null) {
      throw new LoggedSessionNotFoundException(sessionId);
    }
    return corrected;
  }

  /**
   * The Logged Session these values make, once the Session exists and every Set fits its Exercise in
   * the catalog; throws as {@link #log} documents otherwise.
   */
  private LoggedSession validated(UUID sessionId, LocalDate date, List<LoggedSet> sets) {
    programs.session(sessionId);
    LoggedSession logged = new LoggedSession(sessionId, date, sets);
    for (LoggedSet set : logged.sets()) {
      Exercise exercise = exercises.get(set.exerciseId());
      exercise
          .mismatch(set.bellKg(), set.holdSeconds(), set.workSeconds())
          .ifPresent(
              reason -> {
                throw new InvalidLoggedSessionException(reason);
              });
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
