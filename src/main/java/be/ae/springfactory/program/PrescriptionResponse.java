package be.ae.springfactory.program;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The JSON shape of a Prescription: {exerciseId, sets, reps, holdSeconds, bellKg, restSeconds}.
 * holdSeconds is null for an Exercise that is not a hold; restSeconds is null when no rest was
 * prescribed.
 */
record PrescriptionResponse(
    UUID exerciseId,
    int sets,
    int reps,
    Integer holdSeconds,
    BigDecimal bellKg,
    Integer restSeconds) {

  static PrescriptionResponse of(Prescription prescription) {
    return new PrescriptionResponse(
        prescription.exerciseId(),
        prescription.sets(),
        prescription.reps(),
        prescription.holdSeconds(),
        prescription.bellKg(),
        prescription.restSeconds());
  }
}
