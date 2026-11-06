package be.ae.springfactory.exercise;

import java.util.UUID;

/** No Exercise in the catalog has the given id. */
public class ExerciseNotFoundException extends RuntimeException {
  ExerciseNotFoundException(UUID id) {
    super("No Exercise with id " + id);
  }
}
