package be.ae.springfactory.program;

import java.util.List;
import java.util.UUID;

/** The JSON shape of a Week: {id, number, sessions: [...]}. */
record WeekResponse(UUID id, int number, List<SessionResponse> sessions) {

  static WeekResponse of(Week week) {
    return new WeekResponse(
        week.id(), week.number(), week.sessions().stream().map(SessionResponse::of).toList());
  }
}
