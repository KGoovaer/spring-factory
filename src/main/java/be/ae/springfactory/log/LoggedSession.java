package be.ae.springfactory.log;

import be.ae.springfactory.program.Part;
import be.ae.springfactory.program.PartFormat;
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
 * Score per Part. Immutable.
 */
public record LoggedSession(
    UUID sessionId, LocalDate date, List<LoggedSet> sets, List<Score> scores) {
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
    sets = List.copyOf(sets);
    scores = List.copyOf(scores);
  }

  /** A Logged Session of Sets only, without Scores. */
  public LoggedSession(UUID sessionId, LocalDate date, List<LoggedSet> sets) {
    this(sessionId, date, sets, List.of());
  }

  /**
   * Logged Volume of this Logged Session of the {@code planned} Session: the Volume of its Sets,
   * plus that of each Score as its AMRAP Part counts it (whole Rounds, then the extra Reps through
   * the Prescriptions in order). A Score of a Part the Session no longer has, or no longer as an
   * AMRAP, adds 0.
   */
  public BigDecimal volume(Session planned) {
    BigDecimal sets =
        this.sets.stream().map(LoggedSet::volume).reduce(BigDecimal.ZERO, BigDecimal::add);
    return scores.stream()
        .map(score -> scoreVolume(planned, score))
        .reduce(sets, BigDecimal::add);
  }

  private static BigDecimal scoreVolume(Session planned, Score score) {
    if (score.part() > planned.parts().size()) {
      return BigDecimal.ZERO;
    }
    Part part = planned.parts().get(score.part() - 1);
    return part.format() == PartFormat.AMRAP
        ? part.volume(score.rounds(), score.extraReps())
        : BigDecimal.ZERO;
  }
}
