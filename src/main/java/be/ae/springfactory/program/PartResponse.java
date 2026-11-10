package be.ae.springfactory.program;

import java.util.List;

/**
 * The JSON shape of a Part: {number, format, rounds, minutes, rotation, restSeconds,
 * prescriptions}. number counts from 1 within the Session; the fields its format does not take are
 * null.
 */
record PartResponse(
    int number,
    PartFormat format,
    Integer rounds,
    Integer minutes,
    Integer rotation,
    Integer restSeconds,
    List<PrescriptionResponse> prescriptions) {

  static PartResponse of(int number, Part part) {
    return new PartResponse(
        number,
        part.format(),
        part.rounds(),
        part.minutes(),
        part.rotation(),
        part.restSeconds(),
        part.prescriptions().stream().map(PrescriptionResponse::of).toList());
  }
}
