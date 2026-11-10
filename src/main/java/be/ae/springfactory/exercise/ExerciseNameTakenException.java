package be.ae.springfactory.exercise;

/** The catalog has an Exercise with this name already; names are unique, ignoring case. */
public class ExerciseNameTakenException extends RuntimeException {
  ExerciseNameTakenException(String name) {
    super("There is an Exercise named " + name + " already");
  }
}
