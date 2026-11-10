package be.ae.springfactory.program;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** The JSON shape of a Program: {id, name, weeks: [...]}. */
record ProgramResponse(UUID id, String name, List<WeekResponse> weeks) {

  static ProgramResponse of(Program program, Function<Session, List<String>> equipment) {
    return new ProgramResponse(
        program.id(),
        program.name(),
        program.weeks().stream().map(week -> WeekResponse.of(week, equipment)).toList());
  }
}
