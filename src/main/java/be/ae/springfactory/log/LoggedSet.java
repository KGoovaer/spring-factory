package be.ae.springfactory.log;

import be.ae.springfactory.program.Side;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * One Set a Lifter actually did: {@code reps} Reps of an Exercise, on a {@code side} or none, each
 * held {@code holdSeconds} for a hold Exercise or worked {@code workSeconds} for a timed Exercise,
 * with a Bell of {@code bellKg} kilograms. Reps are at least 1, the hold and work times are {@code
 * null} or at least 1, and the Bell weight is {@code null} (no Bell, for a bodyweight Exercise) or
 * above 0; it keeps the scale it was given (10.50 stays 10.50).
 */
public record LoggedSet(
    UUID exerciseId,
    int reps,
    Side side,
    Integer holdSeconds,
    Integer workSeconds,
    BigDecimal bellKg) {
  public LoggedSet {
    if (exerciseId == null) {
      throw new InvalidLoggedSessionException("A Set needs an Exercise");
    }
    if (reps < 1) {
      throw new InvalidLoggedSessionException("Reps must be at least 1, was " + reps);
    }
    if (holdSeconds != null && holdSeconds < 1) {
      throw new InvalidLoggedSessionException("Hold time must be at least 1 s, was " + holdSeconds);
    }
    if (workSeconds != null && workSeconds < 1) {
      throw new InvalidLoggedSessionException("Work time must be at least 1 s, was " + workSeconds);
    }
    if (bellKg != null && bellKg.signum() <= 0) {
      throw new InvalidLoggedSessionException("Bell weight must be above 0 kg, was " + bellKg);
    }
  }

  /** A Set without a Side or work time. */
  public LoggedSet(UUID exerciseId, int reps, Integer holdSeconds, BigDecimal bellKg) {
    this(exerciseId, reps, null, holdSeconds, null, bellKg);
  }

  /** A Set of an Exercise that is neither a hold nor timed, without a Side. */
  public LoggedSet(UUID exerciseId, int reps, BigDecimal bellKg) {
    this(exerciseId, reps, null, bellKg);
  }

  /** Volume of this Set: Reps × Bell weight, Reps counted twice on EACH side, 0 without a Bell. */
  public BigDecimal volume() {
    if (bellKg == null) {
      return BigDecimal.ZERO;
    }
    return bellKg.multiply(BigDecimal.valueOf((long) reps * Side.repsFactor(side)));
  }
}
