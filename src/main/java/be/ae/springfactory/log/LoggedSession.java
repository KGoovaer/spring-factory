package be.ae.springfactory.log;

import be.ae.springfactory.program.Session;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A Session as actually performed on a date: the Sets the Lifter really did, in order, and the
 * Score of each AMRAP Part done. It refers to the planned Session by id and never changes it. It
 * has a date (any date, also one in the future) and at least one Set or Score, and at most one
 * Score per Part. Its Session time, how long the Lifter trained in whole seconds, is null when the
 * Lifter did not time the Session, and never below 0. Immutable.
 */
public record LoggedSession(
    UUID sessionId,
    LocalDate date,
    List<LoggedSet> sets,
    List<Score> scores,
    Integer sessionSeconds) {
  public LoggedSession {
    if (sessionId == null) {
      throw new InvalidLoggedSessionException("A Logged Session needs a Session");
    }
    if (date == null) {
      throw new InvalidLoggedSessionException("A Logged Session needs a date");
    }
    sets = sets == null ? List.of() : sets;
    scores = scores == null ? List.of() : scores;
    if (sets.isEmpty() && scores.isEmpty()) {
      throw new InvalidLoggedSessionException("A Logged Session needs at least one Set or Score");
    }
    if (sets.stream().anyMatch(Objects::isNull)) {
      throw new InvalidLoggedSessionException("A Logged Session cannot hold a missing Set");
    }
    if (scores.stream().anyMatch(Objects::isNull)) {
      throw new InvalidLoggedSessionException("A Logged Session cannot hold a missing Score");
    }
    if (scores.stream().map(Score::part).distinct().count() < scores.size()) {
      throw new InvalidLoggedSessionException("A Part has at most one Score");
    }
    if (sessionSeconds != null && sessionSeconds < 0) {
      throw new InvalidLoggedSessionException("A Session time is never below 0 seconds");
    }
    sets = List.copyOf(sets);
    scores = List.copyOf(scores);
  }

  /** A Logged Session without a Session time. */
  public LoggedSession(UUID sessionId, LocalDate date, List<LoggedSet> sets, List<Score> scores) {
    this(sessionId, date, sets, scores, null);
  }

  /** A Logged Session of Sets only, without Scores. */
  public LoggedSession(UUID sessionId, LocalDate date, List<LoggedSet> sets) {
    this(sessionId, date, sets, List.of());
  }

  /**
   * Logged Volume of this Logged Session of the {@code planned} Session: the Volume of its Sets,
   * plus that of each Score as its AMRAP Part counts it (whole Rounds, then the extra Reps through
   * the Prescriptions in order). A Score of a Part the Session no longer has, no longer as an
   * AMRAP, or whose extra Reps no longer stay short of its Round, adds 0.
   */
  public BigDecimal volume(Session planned) {
    BigDecimal sets =
        this.sets.stream().map(LoggedSet::volume).reduce(BigDecimal.ZERO, BigDecimal::add);
    return scores.stream()
        .map(score -> scoreVolume(planned, score))
        .reduce(sets, BigDecimal::add);
  }

  private static BigDecimal scoreVolume(Session planned, Score score) {
    return planned
        .amrap(score.part())
        .filter(part -> part.isShortOfARound(score.extraReps()))
        .map(part -> part.volume(score.rounds(), score.extraReps()))
        .orElse(BigDecimal.ZERO);
  }
}
