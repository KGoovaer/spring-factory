package be.ae.springfactory.exercise;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The Exercises a Lifter can choose from, kept in the database (see
 * docs/adr/0009-exercises-live-in-a-database.md): the seeded ones, kettlebell Exercises first,
 * then bodyweight ones (see docs/adr/0003-bodyweight-exercises-have-no-bell.md), followed by the
 * ones Lifters added, in the order they were added. Seeded ids are fixed UUIDs, written by the
 * seed migration (see docs/adr/0001-exercise-ids-are-uuids.md); added Exercises get random ones.
 * Names of listed Exercises are unique, ignoring case.
 *
 * <p>A Lifter renames an Exercise or removes it; its classification never changes, so the
 * Equipment and Volume of what was planned or logged with it stay as they were. A removed Exercise is no longer listed and frees
 * its name, but {@link #get} still finds it, so Prescriptions and Logged Sets that refer to it
 * keep resolving. Every row is rebuilt through {@link Exercise}, so its rules hold for what is
 * read back as much as for what is written. Safe for concurrent use: the app is the only one
 * using its database, and it changes the catalog one call at a time.
 */
@Component
public class ExerciseCatalog {

  private final ExerciseRows rows;

  ExerciseCatalog(ExerciseRows rows) {
    this.rows = rows;
  }

  /** The listed Exercises, in catalog order. The returned list is an unmodifiable snapshot. */
  public List<Exercise> all() {
    return rows.findByRemovedFalseOrderByPosition().stream().map(ExerciseRow::exercise).toList();
  }

  /**
   * The Exercise with this id, listed or removed. Throws {@link ExerciseNotFoundException} when
   * there is none.
   */
  public Exercise get(UUID id) {
    return rows.findById(id)
        .map(ExerciseRow::exercise)
        .orElseThrow(() -> new ExerciseNotFoundException(id));
  }

  /**
   * Adds an Exercise with a random id at the end of the catalog and returns it. Throws {@link
   * InvalidExerciseException} when it breaks a rule of {@link Exercise}, and {@link
   * ExerciseNameTakenException} when a listed Exercise has this name already, ignoring case.
   */
  public synchronized Exercise add(
      String name, boolean bodyweight, boolean hold, boolean timed, String gear) {
    Exercise exercise = new Exercise(UUID.randomUUID(), name, bodyweight, hold, timed, gear);
    refuseTakenName(exercise);
    int last = rows.findTopByOrderByPositionDesc().map(ExerciseRow::position).orElse(0);
    rows.save(new ExerciseRow(exercise, last + 1));
    return exercise;
  }

  /**
   * Renames the listed Exercise with this id, keeping its id, classification and place in the
   * catalog, and returns it. Throws {@link ExerciseNotFoundException} when no listed Exercise has
   * this id, {@link InvalidExerciseException} when the name is blank or too long, and {@link
   * ExerciseNameTakenException} when another listed Exercise has this name, ignoring case.
   */
  public synchronized Exercise rename(UUID id, String name) {
    ExerciseRow row = listed(id);
    Exercise current = row.exercise();
    Exercise renamed =
        new Exercise(
            id, name, current.bodyweight(), current.hold(), current.timed(), current.gear());
    refuseTakenName(renamed);
    row.rename(renamed.name());
    rows.save(row);
    return renamed;
  }

  /**
   * Removes the listed Exercise with this id from the catalog and frees its name; {@link #get}
   * still finds it. Throws {@link ExerciseNotFoundException} when no listed Exercise has this id.
   */
  public synchronized void remove(UUID id) {
    ExerciseRow row = listed(id);
    row.remove();
    rows.save(row);
  }

  private ExerciseRow listed(UUID id) {
    return rows.findById(id)
        .filter(row -> !row.removed())
        .orElseThrow(() -> new ExerciseNotFoundException(id));
  }

  private void refuseTakenName(Exercise exercise) {
    if (rows.existsByNameKeyAndIdNot(ExerciseRow.nameKey(exercise.name()), exercise.id())) {
      throw new ExerciseNameTakenException(exercise.name());
    }
  }
}
