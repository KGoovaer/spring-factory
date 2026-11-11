package be.ae.springfactory.exercise;

import java.util.UUID;

/**
 * Something that refers to Exercises by id, such as the Prescriptions of Programs or the Logged
 * Sets of Logged Sessions. {@link ExerciseCatalog} asks every one before it removes an Exercise.
 */
public interface ExerciseUsage {

  /** Whether anything here refers to the Exercise with this id. */
  boolean uses(UUID exerciseId);
}
