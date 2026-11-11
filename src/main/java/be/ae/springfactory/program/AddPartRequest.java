package be.ae.springfactory.program;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * The body of POST /api/sessions/{id}/parts: {format, rounds, minutes, rotation, restSeconds}. A
 * ROUNDS Part gives rounds, an EMOM gives minutes and rotation, a REST Part gives restSeconds, and
 * a SETS Part none of them. A Part has at most 100 Rounds and an EMOM at most 120 minutes.
 */
record AddPartRequest(
    @NotNull PartFormat format,
    @Min(1) @Max(100) Integer rounds,
    @Min(1) @Max(120) Integer minutes,
    @Min(1) @Max(120) Integer rotation,
    @Min(1) Integer restSeconds) {

  Part toPart() {
    return new Part(format, rounds, minutes, rotation, restSeconds, List.of());
  }
}
