package be.ae.springfactory.log;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A Session as actually performed on a date: the Sets the Lifter really did, in order. It refers to
 * the planned Session by id and never changes it. It has a date (any date, also one in the future)
 * and at least one Set. Immutable.
 */
public record LoggedSession(UUID sessionId, LocalDate date, List<LoggedSet> sets) {
  public LoggedSession {
    if (sessionId == null) {
      throw new InvalidLoggedSessionException("A Logged Session needs a Session");
    }
    if (date == null) {
      throw new InvalidLoggedSessionException("A Logged Session needs a date");
    }
    if (sets == null || sets.isEmpty()) {
      throw new InvalidLoggedSessionException("A Logged Session needs at least one Set");
    }
    if (sets.stream().anyMatch(Objects::isNull)) {
      throw new InvalidLoggedSessionException("A Logged Session cannot hold a missing Set");
    }
    sets = List.copyOf(sets);
  }

  /** Logged Volume of this Logged Session: the Volume of its Sets, summed. */
  public BigDecimal volume() {
    return sets.stream().map(LoggedSet::volume).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
