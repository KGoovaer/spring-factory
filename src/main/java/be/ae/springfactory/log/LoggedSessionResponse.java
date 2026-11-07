package be.ae.springfactory.log;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** The JSON shape of a Logged Session: {sessionId, date, sets: [{exerciseId, reps, bellKg}]}. */
record LoggedSessionResponse(UUID sessionId, LocalDate date, List<SetResponse> sets) {

  static LoggedSessionResponse of(LoggedSession logged) {
    return new LoggedSessionResponse(
        logged.sessionId(),
        logged.date(),
        logged.sets().stream().map(SetResponse::of).toList());
  }

  record SetResponse(UUID exerciseId, int reps, BigDecimal bellKg) {

    static SetResponse of(LoggedSet set) {
      return new SetResponse(set.exerciseId(), set.reps(), set.bellKg());
    }
  }
}
