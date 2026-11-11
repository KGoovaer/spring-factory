package be.ae.springfactory.program;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The JSON shape of a Prescription: {exerciseId, sets, reps, repsMax, side, holdSeconds, workSeconds,
 * bellKg, restSeconds, minute}. repsMax is null unless the Reps are a range, side is null when none was given, holdSeconds for an Exercise that
 * is not a hold, workSeconds for one that is not timed, restSeconds when no rest was prescribed,
 * and minute outside an EMOM.
 */
record PrescriptionResponse(
    UUID exerciseId,
    int sets,
    int reps,
    Integer repsMax,
    Side side,
    Integer holdSeconds,
    Integer workSeconds,
    BigDecimal bellKg,
    Integer restSeconds,
    Integer minute) {

  static PrescriptionResponse of(Prescription prescription) {
    return new PrescriptionResponse(
        prescription.exerciseId(),
        prescription.sets(),
        prescription.reps(),
        prescription.repsMax(),
        prescription.side(),
        prescription.holdSeconds(),
        prescription.workSeconds(),
        prescription.bellKg(),
        prescription.restSeconds(),
        prescription.minute());
  }
}
