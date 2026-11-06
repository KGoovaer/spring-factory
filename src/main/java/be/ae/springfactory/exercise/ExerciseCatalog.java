package be.ae.springfactory.exercise;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The catalog of Exercises a Lifter can choose from. Entry point of the exercise module.
 *
 * <p>Read-only and seeded in memory: nothing about Exercises is stored yet.
 */
@Component
public class ExerciseCatalog {

  private static final List<Exercise> SEED =
      List.of(
          new Exercise("swing", "Swing"),
          new Exercise("goblet-squat", "Goblet squat"),
          new Exercise("clean-and-press", "Clean and press"),
          new Exercise("turkish-get-up", "Turkish get-up"),
          new Exercise("snatch", "Snatch"));

  /** All Exercises, in catalog order. The list is immutable. */
  public List<Exercise> all() {
    return SEED;
  }
}
