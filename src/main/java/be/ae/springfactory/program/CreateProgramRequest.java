package be.ae.springfactory.program;

import jakarta.validation.constraints.NotBlank;

/** The body of POST /api/programs. */
record CreateProgramRequest(@NotBlank String name) {}
