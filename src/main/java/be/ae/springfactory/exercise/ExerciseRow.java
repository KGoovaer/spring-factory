package be.ae.springfactory.exercise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.UUID;

/**
 * An Exercise as the exercise table keeps it (see V1__create_exercise.sql), with its place in the
 * catalog. Built only from an {@link Exercise} and read back only through one, so the Exercise's
 * rules hold for every row. {@code nameKey} is the lower-cased name, so names are unique ignoring
 * case.
 */
@Entity
@Table(name = "exercise")
class ExerciseRow {

  @Id private UUID id;

  @Column(nullable = false, length = Exercise.MAX_LENGTH)
  private String name;

  @Column(nullable = false, unique = true, length = Exercise.MAX_LENGTH)
  private String nameKey;

  private boolean bodyweight;
  private boolean hold;
  private boolean timed;

  @Column(length = Exercise.MAX_LENGTH)
  private String gear;

  private int position;

  /** For JPA. */
  protected ExerciseRow() {}

  ExerciseRow(Exercise exercise, int position) {
    this.id = exercise.id();
    this.position = position;
    this.bodyweight = exercise.bodyweight();
    this.hold = exercise.hold();
    this.timed = exercise.timed();
    this.gear = exercise.gear();
    rename(exercise.name());
  }

  /** The lower-cased name that no two Exercises share. */
  static String nameKey(String name) {
    return name.toLowerCase(Locale.ROOT);
  }

  Exercise exercise() {
    return new Exercise(id, name, bodyweight, hold, timed, gear);
  }

  int position() {
    return position;
  }

  /** Takes this name, which an {@link Exercise} has checked. */
  void rename(String name) {
    this.name = name;
    this.nameKey = nameKey(name);
  }
}
