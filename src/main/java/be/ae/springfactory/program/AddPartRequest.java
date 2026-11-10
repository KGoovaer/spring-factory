package be.ae.springfactory.program;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * The body of POST /api/sessions/{id}/parts: {format, rounds, minutes, rotation, restSeconds}. A
 * ROUNDS Part gives rounds, an EMOM gives minutes and rotation, a REST Part gives restSeconds, and
 * a SETS Part none of them.
 */
record AddPartRequest(
    @NotNull PartFormat format,
    @Min(1) Integer rounds,
    @Min(1) Integer minutes,
    @Min(1) Integer rotation,
    @Min(1) Integer restSeconds) {

  Part toPart() {
    return new Part(format, rounds, minutes, rotation, restSeconds, List.of());
  }
}
