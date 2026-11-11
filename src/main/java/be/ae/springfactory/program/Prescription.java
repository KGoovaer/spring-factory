package be.ae.springfactory.program;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * What a Session asks for one Exercise: Sets of Reps (or of {@code reps} to {@code repsMax} Reps,
 * a rep range such as 3-4, where {@code repsMax} is above {@code reps}), on a {@code side} or none, each Rep held
 * {@code holdSeconds} for a hold Exercise or worked {@code workSeconds} for a timed Exercise, with a
 * Bell of {@code bellKg} kilograms, and optionally {@code restSeconds} of rest. In an EMOM Part,
 * {@code minute} says in which minute of the rotation it is done; elsewhere it is {@code null}.
 * Sets and Reps are at least 1, the hold and work times are {@code null} or at least 1, the Bell
 * weight is {@code null} (no Bell, for a bodyweight Exercise) or above 0, rest, when given, is not
 * negative, and the minute, when given, is at least 1. The Bell weight keeps the scale it was given
 * (10.50 stays 10.50).
 */
public record Prescription(
    UUID exerciseId,
    int sets,
    int reps,
    Integer repsMax,
    Side side,
    Integer holdSeconds,
    Integer workSeconds,
    BigDecimal bellKg,
    Integer restSeconds,
    Integer minute) {
  public Prescription {
    if (exerciseId == null) {
      throw new InvalidPrescriptionException("A Prescription needs an Exercise");
    }
    if (sets < 1) {
      throw new InvalidPrescriptionException("Sets must be at least 1, was " + sets);
    }
    if (reps < 1) {
      throw new InvalidPrescriptionException("Reps must be at least 1, was " + reps);
    }
    if (repsMax != null && repsMax <= reps) {
      throw new InvalidPrescriptionException(
          "A rep range must end above its " + reps + " Reps, was " + reps + "-" + repsMax);
    }
    if (holdSeconds != null && holdSeconds < 1) {
      throw new InvalidPrescriptionException("Hold time must be at least 1 s, was " + holdSeconds);
    }
    if (workSeconds != null && workSeconds < 1) {
      throw new InvalidPrescriptionException("Work time must be at least 1 s, was " + workSeconds);
    }
    if (bellKg != null && bellKg.signum() <= 0) {
      throw new InvalidPrescriptionException("Bell weight must be above 0 kg, was " + bellKg);
    }
    if (restSeconds != null && restSeconds < 0) {
      throw new InvalidPrescriptionException("Rest must not be negative, was " + restSeconds);
    }
    if (minute != null && minute < 1) {
      throw new InvalidPrescriptionException("The minute must be at least 1, was " + minute);
    }
  }

  /** A Prescription of an exact number of Reps, not a range. */
  public Prescription(
      UUID exerciseId,
      int sets,
      int reps,
      Side side,
      Integer holdSeconds,
      Integer workSeconds,
      BigDecimal bellKg,
      Integer restSeconds,
      Integer minute) {
    this(
        exerciseId, sets, reps, null, side, holdSeconds, workSeconds, bellKg, restSeconds, minute);
  }

  /** A Prescription without a Side, work time or minute. */
  public Prescription(
      UUID exerciseId,
      int sets,
      int reps,
      Integer holdSeconds,
      BigDecimal bellKg,
      Integer restSeconds) {
    this(exerciseId, sets, reps, null, holdSeconds, null, bellKg, restSeconds, null);
  }

  /** A Prescription for an Exercise that is neither a hold nor timed, without a Side or minute. */
  public Prescription(
      UUID exerciseId, int sets, int reps, BigDecimal bellKg, Integer restSeconds) {
    this(exerciseId, sets, reps, null, bellKg, restSeconds);
  }

  /**
   * Volume of this Prescription done once: Sets × Reps × Bell weight, Reps counted twice on {@code
   * EACH} side, and 0 without a Bell. A rep range counts its low end: what the Lifter commits to.
   */
  public BigDecimal volume() {
    return volume(repsDone());
  }

  /** The Reps done once through this Prescription: Sets × Reps, twice on {@code EACH} side. */
  long repsDone() {
    return (long) sets * reps * Side.repsFactor(side);
  }

  /** Volume of {@code reps} Reps of this Prescription, as counted by {@link #repsDone()}. */
  BigDecimal volume(long reps) {
    return bellKg == null ? BigDecimal.ZERO : bellKg.multiply(BigDecimal.valueOf(reps));
  }
}
