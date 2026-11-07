package be.ae.springfactory.program;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Adds Sessions to a Week and Prescriptions to a Session. */
@RestController
class SessionController {

  private final Programs programs;

  SessionController(Programs programs) {
    this.programs = programs;
  }

  @PostMapping("/api/weeks/{id}/sessions")
  ResponseEntity<SessionResponse> addSession(@PathVariable UUID id) {
    Session session = programs.addSession(id);
    return ResponseEntity.status(HttpStatus.CREATED).body(SessionResponse.of(session));
  }

  @PostMapping("/api/sessions/{id}/prescriptions")
  ResponseEntity<PrescriptionResponse> addPrescription(
      @PathVariable UUID id, @Valid @RequestBody AddPrescriptionRequest request) {
    Prescription prescription = programs.addPrescription(id, request.toPrescription());
    return ResponseEntity.status(HttpStatus.CREATED).body(PrescriptionResponse.of(prescription));
  }
}
