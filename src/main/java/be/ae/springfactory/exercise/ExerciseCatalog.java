package be.ae.springfactory.exercise;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The Exercises a Lifter can choose from: kettlebell Exercises first, then bodyweight ones (see
 * docs/adr/0003-bodyweight-exercises-have-no-bell.md). Read-only and seeded in memory; ids are
 * fixed UUIDs so they stay the same across restarts (see docs/adr/0001-exercise-ids-are-uuids.md).
 */
@Component
public class ExerciseCatalog {

  private static final List<Exercise> EXERCISES =
      List.of(
          exercise("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01", "Swing"),
          exercise("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c02", "Goblet squat"),
          exercise("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c03", "Clean and press"),
          exercise("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c04", "Turkish get-up"),
          exercise("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c05", "Snatch"),
          bodyweight("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06", "Push-up"),
          bodyweightHold("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c07", "Plank"),
          bodyweight("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c08", "Sit-up"));

  /** All Exercises, in catalog order. The returned list is unmodifiable. */
  public List<Exercise> all() {
    return EXERCISES;
  }

  /** The Exercise with this id. Throws {@link ExerciseNotFoundException} when there is none. */
  public Exercise get(UUID id) {
    return EXERCISES.stream()
        .filter(exercise -> exercise.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new ExerciseNotFoundException(id));
  }

  private static Exercise exercise(String id, String name) {
    return new Exercise(UUID.fromString(id), name, false, false);
  }

  private static Exercise bodyweight(String id, String name) {
    return new Exercise(UUID.fromString(id), name, true, false);
  }

  private static Exercise bodyweightHold(String id, String name) {
    return new Exercise(UUID.fromString(id), name, true, true);
  }
}
