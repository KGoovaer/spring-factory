package be.ae.springfactory.exercise;

import jakarta.validation.constraints.NotBlank;

/**
 * The body of POST /api/exercises: {name, bodyweight, hold, timed, gear}. The flags default to
 * false; gear is left out except for a bodyweight Exercise done with something, such as a jump rope.
 */
record AddExerciseRequest(
    @NotBlank String name, Boolean bodyweight, Boolean hold, Boolean timed, String gear) {

  Exercise addTo(ExerciseCatalog catalog) {
    return catalog.add(
        name,
        Boolean.TRUE.equals(bodyweight),
        Boolean.TRUE.equals(hold),
        Boolean.TRUE.equals(timed),
        gear);
  }
}
