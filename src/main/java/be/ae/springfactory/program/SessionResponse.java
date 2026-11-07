package be.ae.springfactory.program;

import java.util.List;
import java.util.UUID;

/** The JSON shape of a Session: {id, prescriptions: [...]}. */
record SessionResponse(UUID id, List<PrescriptionResponse> prescriptions) {

  static SessionResponse of(Session session) {
    return new SessionResponse(
        session.id(), session.prescriptions().stream().map(PrescriptionResponse::of).toList());
  }
}
