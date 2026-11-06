package be.ae.springfactory.program;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/** The body of POST /api/sessions/{id}/prescriptions. restSeconds may be left out. */
record AddPrescriptionRequest(
    @NotNull UUID exerciseId,
    @NotNull @Min(1) Integer sets,
    @NotNull @Min(1) Integer reps,
    @NotNull @Positive BigDecimal bellKg,
    @PositiveOrZero Integer restSeconds) {

  Prescription toPrescription() {
    return new Prescription(exerciseId, sets, reps, bellKg, restSeconds);
  }
}
