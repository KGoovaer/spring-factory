package be.ae.springfactory.log;

import be.ae.springfactory.program.Side;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The body of POST and PUT /api/sessions/{id}/log: {date, sets: [{exerciseId, reps, side,
 * holdSeconds, workSeconds, bellKg}]}. side (LEFT, RIGHT or EACH) may be left out; bellKg is left
 * out (or null) for a bodyweight Exercise; holdSeconds is given only for a hold Exercise and
 * workSeconds only for a timed one.
 */
record LogSessionRequest(@NotNull LocalDate date, @NotEmpty List<@NotNull @Valid SetRequest> sets) {

  List<LoggedSet> toLoggedSets() {
    return sets.stream().map(SetRequest::toLoggedSet).toList();
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
