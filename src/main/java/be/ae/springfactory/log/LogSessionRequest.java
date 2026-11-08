package be.ae.springfactory.log;

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
 * The body of POST /api/sessions/{id}/log: {date, sets: [{exerciseId, reps, holdSeconds,
 * bellKg}]}. bellKg is left out (or null) for a bodyweight Exercise; holdSeconds is given only for
 * a hold Exercise.
 */
record LogSessionRequest(@NotNull LocalDate date, @NotEmpty List<@NotNull @Valid SetRequest> sets) {

  List<LoggedSet> toLoggedSets() {
    return sets.stream().map(SetRequest::toLoggedSet).toList();
  }

  record SetRequest(
      @NotNull UUID exerciseId,
      @NotNull @Min(1) Integer reps,
      @Min(1) Integer holdSeconds,
      @Positive BigDecimal bellKg) {

    LoggedSet toLoggedSet() {
      return new LoggedSet(exerciseId, reps, holdSeconds, bellKg);
    }
  }
}
