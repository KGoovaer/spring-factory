package be.ae.springfactory.exercise;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A named movement, such as a swing or a Turkish get-up. A kettlebell Exercise is done with a Bell;
 * a {@code bodyweight} Exercise, such as a push-up, is done without one, and may name the {@code
 * gear} it needs instead, such as a jump rope. A {@code hold} Exercise, such as the plank, is
 * measured in hold time: each Rep is held for a number of seconds. A {@code timed} Exercise, such as
 * rope skipping, is measured in work time: each Rep is worked for a number of seconds. No Exercise
 * is both a hold and timed (see docs/adr/0006-skipping-is-measured-in-work-time.md).
 */
public record Exercise(
    UUID id, String name, boolean bodyweight, boolean hold, boolean timed, String gear) {
  public Exercise {
    Objects.requireNonNull(id, "id");
    if (name == null || name.isBlank()) {
      throw new InvalidExerciseException("An Exercise needs a name");
    }
    name = name.strip();
    if (hold && timed) {
      throw new InvalidExerciseException(name + " cannot be both a hold and timed");
    }
    if (gear != null && gear.isBlank()) {
      gear = null;
    }
    if (gear != null && !bodyweight) {
      throw new InvalidExerciseException(
          name + " is done with a Bell and names no other gear, was " + gear);
    }
    gear = gear == null ? null : gear.strip();
  }

  /** An Exercise that needs no gear but a Bell, if it is not bodyweight. */
  public Exercise(UUID id, String name, boolean bodyweight, boolean hold) {
    this(id, name, bodyweight, hold, false, null);
  }

  /**
   * What this Exercise is done with, as a Session lists its Equipment: "Kettlebell" for a
   * kettlebell Exercise, its gear for a bodyweight Exercise that has some, else "Body weight".
   */
  public String equipment() {
    if (!bodyweight) {
      return "Kettlebell";
    }
    return gear == null ? "Body weight" : gear;
  }

  /**
   * Why a Set of this Exercise may not have this Bell weight, hold time and work time, for an error
   * message; empty when they fit. A kettlebell Exercise needs a Bell, a bodyweight Exercise takes
   * none ({@code null}). A hold Exercise needs a hold time and a timed Exercise a work time; any
   * other Exercise takes neither ({@code null}).
   */
  public Optional<String> mismatch(BigDecimal bellKg, Integer holdSeconds, Integer workSeconds) {
    if (bodyweight && bellKg != null) {
      return Optional.of(
          name + " is a bodyweight Exercise and takes no Bell, was " + bellKg + " kg");
    }
    if (!bodyweight && bellKg == null) {
      return Optional.of(name + " needs a Bell weight");
    }
    if (hold && holdSeconds == null) {
      return Optional.of(name + " is a hold and needs a hold time");
    }
    if (!hold && holdSeconds != null) {
      return Optional.of(name + " is not a hold and takes no hold time, was " + holdSeconds + " s");
    }
    if (timed && workSeconds == null) {
      return Optional.of(name + " is timed and needs a work time");
    }
    if (!timed && workSeconds != null) {
      return Optional.of(
          name + " is not timed and takes no work time, was " + workSeconds + " s");
    }
    return Optional.empty();
  }
}
