package be.ae.springfactory.exercise;

import java.util.Objects;
import java.util.UUID;

/** A named movement, such as a swing or a Turkish get-up. */
public record Exercise(UUID id, String name) {
  public Exercise {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
  }
}
