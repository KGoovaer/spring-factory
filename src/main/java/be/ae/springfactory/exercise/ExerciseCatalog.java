package be.ae.springfactory.exercise;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * The Exercises a Lifter can choose from: the seeded ones, kettlebell Exercises first, then
 * bodyweight ones (see docs/adr/0003-bodyweight-exercises-have-no-bell.md), followed by the ones
 * Lifters added, in the order they were added. Seeded ids are fixed UUIDs so they stay the same
 * across restarts (see docs/adr/0001-exercise-ids-are-uuids.md); added Exercises get random ones
 * and, like Programs, live in memory only (see docs/adr/0007-lifters-add-exercises.md). Names are
 * unique, ignoring case. Safe for concurrent use.
 */
@Component
public class ExerciseCatalog {

  private static final List<Exercise> SEEDS =
      List.of(
          kettlebell("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01", "Swing"),
          kettlebell("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c02", "Goblet squat"),
          kettlebell("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c03", "Clean and press"),
          kettlebell("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c04", "Turkish get-up"),
          kettlebell("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c05", "Snatch"),
          kettlebell("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c09", "Clean to waiter press"),
          kettlebell("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0a", "Split stance row"),
          bodyweight("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06", "Push-up"),
          new Exercise(
              UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c07"), "Plank", true, true),
          bodyweight("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c08", "Sit-up"),
          bodyweight("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0b", "Walking plank"),
          new Exercise(
              UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0c"),
              "Skipping",
              true,
              false,
              true,
              "Jump rope"));

  private final List<Exercise> exercises = new CopyOnWriteArrayList<>(SEEDS);

  /** All Exercises, in catalog order. The returned list is an unmodifiable snapshot. */
  public List<Exercise> all() {
    return List.copyOf(exercises);
  }

  /** The Exercise with this id. Throws {@link ExerciseNotFoundException} when there is none. */
  public Exercise get(UUID id) {
    return exercises.stream()
        .filter(exercise -> exercise.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new ExerciseNotFoundException(id));
  }

  /**
   * Adds an Exercise with a random id at the end of the catalog and returns it. Throws {@link
   * InvalidExerciseException} when it breaks a rule of {@link Exercise}, and {@link
   * ExerciseNameTakenException} when an Exercise has this name already, ignoring case.
   */
  public synchronized Exercise add(
      String name, boolean bodyweight, boolean hold, boolean timed, String gear) {
    Exercise exercise = new Exercise(UUID.randomUUID(), name, bodyweight, hold, timed, gear);
    String key = exercise.name().toLowerCase(Locale.ROOT);
    if (exercises.stream().anyMatch(other -> other.name().toLowerCase(Locale.ROOT).equals(key))) {
      throw new ExerciseNameTakenException(exercise.name());
    }
    exercises.add(exercise);
    return exercise;
  }

  private static Exercise kettlebell(String id, String name) {
    return new Exercise(UUID.fromString(id), name, false, false);
  }

  private static Exercise bodyweight(String id, String name) {
    return new Exercise(UUID.fromString(id), name, true, false);
  }
}
