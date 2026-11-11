package be.ae.springfactory.log;

import be.ae.springfactory.program.Side;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The JSON shape of a Logged Session: {sessionId, date, sets: [{exerciseId, reps, side,
 * holdSeconds, workSeconds, bellKg}], scores: [{part, rounds, extraReps}]}.
 */
record LoggedSessionResponse(
    UUID sessionId, LocalDate date, List<SetResponse> sets, List<Score> scores) {

  static LoggedSessionResponse of(LoggedSession logged) {
    return new LoggedSessionResponse(
        logged.sessionId(),
        logged.date(),
        logged.sets().stream().map(SetResponse::of).toList(),
        logged.scores());
  }

  record SetResponse(
      UUID exerciseId,
      int reps,
      Side side,
      Integer holdSeconds,
      Integer workSeconds,
      BigDecimal bellKg) {

    static SetResponse of(LoggedSet set) {
      return new SetResponse(
          set.exerciseId(),
          set.reps(),
          set.side(),
          set.holdSeconds(),
          set.workSeconds(),
          set.bellKg());
    }
  }
}
