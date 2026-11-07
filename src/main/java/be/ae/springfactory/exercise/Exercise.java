package be.ae.springfactory.exercise;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A named movement, such as a swing or a Turkish get-up. A kettlebell Exercise is done with a Bell;
 * a {@code bodyweight} Exercise, such as a push-up, is done without one.
 */
public record Exercise(UUID id, String name, boolean bodyweight) {
  public Exercise {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
  }

  /**
   * Whether a Set of this Exercise may have this Bell weight: a kettlebell Exercise needs a Bell, a
   * bodyweight Exercise takes none ({@code null}).
   */
  public boolean fits(BigDecimal bellKg) {
    return bodyweight == (bellKg == null);
  }

  /** Why {@code bellKg} does not {@link #fits fit} this Exercise, for an error message. */
  public String bellMismatch(BigDecimal bellKg) {
    return bodyweight
        ? name + " is a bodyweight Exercise and takes no Bell, was " + bellKg + " kg"
        : name + " needs a Bell weight";
  }
}
