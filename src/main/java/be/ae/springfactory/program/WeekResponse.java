package be.ae.springfactory.program;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** The JSON shape of a Week: {id, number, sessions: [...]}. */
record WeekResponse(UUID id, int number, List<SessionResponse> sessions) {

  static WeekResponse of(Week week, Function<Session, List<String>> equipment) {
    return new WeekResponse(
        week.id(),
        week.number(),
        week.sessions().stream().map(session -> SessionResponse.of(session, equipment)).toList());
  }
}
