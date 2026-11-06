package be.ae.springfactory.program;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One planned training day in a Week: its Prescriptions in the order they were added. Its id is a
 * random UUID, so a Logged Session can refer to it. Immutable: adding a Prescription yields a new
 * Session.
 */
public record Session(UUID id, List<Prescription> prescriptions) {
  public Session {
    Objects.requireNonNull(id, "id");
    prescriptions = List.copyOf(prescriptions);
  }

  Session withPrescription(Prescription prescription) {
    List<Prescription> next = new ArrayList<>(prescriptions);
    next.add(prescription);
    return new Session(id, next);
  }
}
