package be.ae.springfactory.program;

import java.util.UUID;

/** No Week in any Program has the given id. */
public class WeekNotFoundException extends RuntimeException {
  WeekNotFoundException(UUID id) {
    super("No Week with id " + id);
  }
}
