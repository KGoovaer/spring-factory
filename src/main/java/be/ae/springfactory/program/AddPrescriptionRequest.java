package be.ae.springfactory.program;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * The body of POST /api/sessions/{id}/prescriptions and /api/sessions/{id}/parts/{number}/
 * prescriptions. repsMax, given for a rep range such as 3-4, ends it above reps; side (LEFT, RIGHT or EACH) may be left out; bellKg is left out (or null) for a
 * bodyweight Exercise; holdSeconds is given only for a hold Exercise and workSeconds only for a
 * timed one; minute (at most 120, the longest EMOM) only in an EMOM Part; restSeconds may be left
 * out.
 */
record AddPrescriptionRequest(
    @NotNull UUID exerciseId,
    @NotNull @Min(1) Integer sets,
    @NotNull @Min(1) Integer reps,
    @Min(2) Integer repsMax,
    Side side,
    @Min(1) Integer holdSeconds,
    @Min(1) Integer workSeconds,
    @Positive BigDecimal bellKg,
    @PositiveOrZero Integer restSeconds,
    @Min(1) @Max(120) Integer minute) {

  Prescription toPrescription() {
    return new Prescription(
        exerciseId, sets, reps, repsMax, side, holdSeconds, workSeconds, bellKg, restSeconds, minute);
  }
}
