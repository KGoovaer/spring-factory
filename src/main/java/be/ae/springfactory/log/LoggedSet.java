package be.ae.springfactory.log;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One Set a Lifter actually did: {@code reps} Reps of an Exercise with a Bell of {@code bellKg}
 * kilograms. Reps are at least 1 and the Bell weight is {@code null} (no Bell, for a bodyweight
 * Exercise) or above 0; it keeps the scale it was given (10.50 stays 10.50).
 */
public record LoggedSet(UUID exerciseId, int reps, BigDecimal bellKg) {
  public LoggedSet {
    if (exerciseId == null) {
      throw new InvalidLoggedSessionException("A Set needs an Exercise");
    }
    if (reps < 1) {
      throw new InvalidLoggedSessionException("Reps must be at least 1, was " + reps);
    }
    if (bellKg != null && bellKg.signum() <= 0) {
      throw new InvalidLoggedSessionException("Bell weight must be above 0 kg, was " + bellKg);
    }
  }

  /** Volume of this Set: Reps × Bell weight, and 0 without a Bell. */
  public BigDecimal volume() {
    if (bellKg == null) {
      return BigDecimal.ZERO;
    }
    return bellKg.multiply(BigDecimal.valueOf(reps));
  }
}
