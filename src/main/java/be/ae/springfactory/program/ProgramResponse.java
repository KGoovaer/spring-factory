package be.ae.springfactory.program;

import java.util.List;
import java.util.UUID;

/** The JSON shape of a Program: {id, name, weeks: [{number}]}. */
record ProgramResponse(UUID id, String name, List<WeekResponse> weeks) {

  static ProgramResponse of(Program program) {
    return new ProgramResponse(
        program.id(), program.name(), program.weeks().stream().map(WeekResponse::of).toList());
  }
}
