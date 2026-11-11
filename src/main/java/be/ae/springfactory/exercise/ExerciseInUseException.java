package be.ae.springfactory.exercise;

/** An Exercise that is planned or logged cannot be removed from the catalog. */
public class ExerciseInUseException extends RuntimeException {
  ExerciseInUseException(Exercise exercise) {
    super(exercise.name() + " is planned or logged and cannot be removed");
  }
}
