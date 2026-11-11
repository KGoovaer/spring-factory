package be.ae.springfactory.exercise;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The Exercises a Lifter can choose from, kept in the database (see
 * docs/adr/0009-exercises-live-in-a-database.md): the seeded ones, kettlebell Exercises first,
 * then bodyweight ones (see docs/adr/0003-bodyweight-exercises-have-no-bell.md), followed by the
 * ones Lifters added, in the order they were added. Seeded ids are fixed UUIDs, written by the
 * seed migration (see docs/adr/0001-exercise-ids-are-uuids.md); added Exercises get random ones.
 * Names of listed Exercises are unique, ignoring case.
 *
 * <p>A Lifter changes an Exercise or removes it. A removed Exercise is no longer listed and frees
 * its name, but {@link #get} still finds it, so Prescriptions and Logged Sets that refer to it
 * keep resolving. Every row is rebuilt through {@link Exercise}, so its rules hold for what is
 * read back as much as for what is written. Safe for concurrent use.
 */
@Component
public class ExerciseCatalog {

  private static final String COLUMNS = "id, name, bodyweight, hold, timed, gear";

  private final JdbcClient jdbc;

  ExerciseCatalog(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** The listed Exercises, in catalog order. The returned list is an unmodifiable snapshot. */
  public List<Exercise> all() {
    return List.copyOf(
        jdbc.sql("SELECT " + COLUMNS + " FROM exercise WHERE NOT removed ORDER BY position")
            .query(ExerciseCatalog::exercise)
            .list());
  }

  /**
   * The Exercise with this id, listed or removed. Throws {@link ExerciseNotFoundException} when
   * there is none.
   */
  public Exercise get(UUID id) {
    return jdbc.sql("SELECT " + COLUMNS + " FROM exercise WHERE id = ?")
        .param(id)
        .query(ExerciseCatalog::exercise)
        .optional()
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
    try {
      jdbc.sql(
              """
              INSERT INTO exercise (id, name, name_key, bodyweight, hold, timed, gear, position)
              SELECT :id, :name, :nameKey, :bodyweight, :hold, :timed, :gear,
                     COALESCE(MAX(position), 0) + 1
              FROM exercise
              """)
          .paramSource(row(exercise))
          .update();
    } catch (DuplicateKeyException e) {
      throw new ExerciseNameTakenException(exercise.name());
    }
    return exercise;
  }

  /**
   * Changes the name and classification of the listed Exercise with this id, keeping its id and
   * place in the catalog, and returns it. Prescriptions and Logged Sets that refer to it keep what
   * they were planned or logged with; the new classification applies to the ones added after.
   * Throws {@link ExerciseNotFoundException} when no listed Exercise has this id, {@link
   * InvalidExerciseException} when the change breaks a rule of {@link Exercise}, and {@link
   * ExerciseNameTakenException} when another listed Exercise has this name, ignoring case.
   */
  public synchronized Exercise change(
      UUID id, String name, boolean bodyweight, boolean hold, boolean timed, String gear) {
    Exercise exercise = new Exercise(id, name, bodyweight, hold, timed, gear);
    int changed;
    try {
      changed =
          jdbc.sql(
                  """
                  UPDATE exercise
                  SET name = :name, name_key = :nameKey, bodyweight = :bodyweight,
                      hold = :hold, timed = :timed, gear = :gear
                  WHERE id = :id AND NOT removed
                  """)
              .paramSource(row(exercise))
              .update();
    } catch (DuplicateKeyException e) {
      throw new ExerciseNameTakenException(exercise.name());
    }
    if (changed == 0) {
      throw new ExerciseNotFoundException(id);
    }
    return exercise;
  }

  /**
   * Removes the listed Exercise with this id from the catalog and frees its name; {@link #get}
   * still finds it. Throws {@link ExerciseNotFoundException} when no listed Exercise has this id.
   */
  public synchronized void remove(UUID id) {
    int removed =
        jdbc.sql(
                "UPDATE exercise SET removed = TRUE, name_key = NULL WHERE id = ? AND NOT removed")
            .param(id)
            .update();
    if (removed == 0) {
      throw new ExerciseNotFoundException(id);
    }
  }

  private static Row row(Exercise exercise) {
    return new Row(
        exercise.id(),
        exercise.name(),
        exercise.name().toLowerCase(Locale.ROOT),
        exercise.bodyweight(),
        exercise.hold(),
        exercise.timed(),
        exercise.gear());
  }

  private static Exercise exercise(ResultSet rs, int rowNum) throws SQLException {
    return new Exercise(
        rs.getObject("id", UUID.class),
        rs.getString("name"),
        rs.getBoolean("bodyweight"),
        rs.getBoolean("hold"),
        rs.getBoolean("timed"),
        rs.getString("gear"));
  }

  /** The named parameters of an Exercise's row. */
  private record Row(
      UUID id,
      String name,
      String nameKey,
      boolean bodyweight,
      boolean hold,
      boolean timed,
      String gear) {}
}
