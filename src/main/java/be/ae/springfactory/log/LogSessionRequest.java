package be.ae.springfactory.log;

import be.ae.springfactory.program.Side;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The body of POST and PUT /api/sessions/{id}/log: {date, sets: [{exerciseId, reps, side,
 * holdSeconds, workSeconds, bellKg}], scores: [{part, rounds, extraReps}]}. sets or scores may be left out, but not both:
 * it holds the Score of each AMRAP Part done, by its number in the Session. side (LEFT, RIGHT or EACH) may be left out; bellKg is left
 * out (or null) for a bodyweight Exercise; holdSeconds is given only for a hold Exercise and
 * workSeconds only for a timed one.
 */
record LogSessionRequest(
    @NotNull LocalDate date,
    List<@NotNull @Valid SetRequest> sets,
    List<@NotNull @Valid ScoreRequest> scores) {

  List<LoggedSet> toLoggedSets() {
    return sets == null ? List.of() : sets.stream().map(SetRequest::toLoggedSet).toList();
  }

  List<Score> toScores() {
    return scores == null ? List.of() : scores.stream().map(ScoreRequest::toScore).toList();
  }

  record ScoreRequest(
      @NotNull @Min(1) Integer part,
      @NotNull @Min(0) Integer rounds,
      @NotNull @Min(0) Integer extraReps) {

    Score toScore() {
      return new Score(part, rounds, extraReps);
    }
  }

  record SetRequest(
      @NotNull UUID exerciseId,
      @NotNull @Min(1) Integer reps,
      Side side,
      @Min(1) Integer holdSeconds,
      @Min(1) Integer workSeconds,
      @Positive BigDecimal bellKg) {

    LoggedSet toLoggedSet() {
      return new LoggedSet(exerciseId, reps, side, holdSeconds, workSeconds, bellKg);
    }
  }
}
