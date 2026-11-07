package be.ae.springfactory.exercise;

import java.util.UUID;

/** The JSON shape of one Exercise at GET /api/exercises. */
record ExerciseResponse(UUID id, String name, boolean bodyweight) {

  static ExerciseResponse of(Exercise exercise) {
    return new ExerciseResponse(exercise.id(), exercise.name(), exercise.bodyweight());
  }
}
