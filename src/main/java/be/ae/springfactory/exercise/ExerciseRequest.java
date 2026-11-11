package be.ae.springfactory.exercise;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * The body of POST /api/exercises and PUT /api/exercises/{id}: {name, bodyweight, hold, timed,
 * gear}. The flags default to false; gear is left out except for a bodyweight Exercise done with
 * something, such as a jump rope.
 */
record ExerciseRequest(
    @NotBlank String name, Boolean bodyweight, Boolean hold, Boolean timed, String gear) {

  Exercise addTo(ExerciseCatalog catalog) {
    return catalog.add(name, is(bodyweight), is(hold), is(timed), gear);
  }

  Exercise changeIn(ExerciseCatalog catalog, UUID id) {
    return catalog.change(id, name, is(bodyweight), is(hold), is(timed), gear);
  }

  private static boolean is(Boolean flag) {
    return Boolean.TRUE.equals(flag);
  }
}
