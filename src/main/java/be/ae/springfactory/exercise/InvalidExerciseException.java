package be.ae.springfactory.exercise;

/**
 * An Exercise broke one of its rules: a blank name, both a hold and timed, or gear on a kettlebell
 * Exercise.
 */
public class InvalidExerciseException extends RuntimeException {
  InvalidExerciseException(String message) {
    super(message);
  }
}
