package be.ae.springfactory.exercise;

import jakarta.validation.constraints.NotBlank;

/**
 * The body of PUT /api/exercises/{id}: {name}. Only the name changes; a body with a classification
 * is refused as a field the API does not know.
 */
record RenameExerciseRequest(@NotBlank String name) {}
