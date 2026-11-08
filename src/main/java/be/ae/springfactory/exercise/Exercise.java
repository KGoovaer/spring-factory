package be.ae.springfactory.exercise;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A named movement, such as a swing or a Turkish get-up. A kettlebell Exercise is done with a Bell;
 * a {@code bodyweight} Exercise, such as a push-up, is done without one. A {@code hold} Exercise,
 * such as the plank, is measured in hold time: each Rep is held for a number of seconds.
 */
public record Exercise(UUID id, String name, boolean bodyweight, boolean hold) {
  public Exercise {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
  }

  /**
   * Why a Set of this Exercise may not have this Bell weight and hold time, for an error message;
   * empty when they fit. A kettlebell Exercise needs a Bell, a bodyweight Exercise takes none
   * ({@code null}). A hold Exercise needs a hold time, any other Exercise takes none ({@code
   * null}).
   */
  public Optional<String> mismatch(BigDecimal bellKg, Integer holdSeconds) {
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
    return Optional.empty();
  }
}
