package be.ae.springfactory.program;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One planned training day in a Week: its Parts in order, and the Lifter's notes for it (free text
 * such as intensity, warm-up and scale-down options, or {@code null}). Its id is a random UUID, so
 * a Logged Session can refer to it. Immutable: adding a Part or Prescription yields a new Session.
 */
public record Session(UUID id, String notes, List<Part> parts) {
  public Session {
    Objects.requireNonNull(id, "id");
    notes = notes == null || notes.isBlank() ? null : notes.strip();
    parts = List.copyOf(parts);
  }

  /** A Session without notes whose Prescriptions, if any, form one Part of straight Sets. */
  public Session(UUID id, List<Prescription> prescriptions) {
    this(id, null, prescriptions.isEmpty() ? List.of() : List.of(setsOf(prescriptions)));
  }

  /** All Prescriptions of all Parts, in order. */
  public List<Prescription> prescriptions() {
    return parts.stream().flatMap(part -> part.prescriptions().stream()).toList();
  }

  /** Planned Volume of this Session: the Volume of its Parts, summed. */
  public BigDecimal volume() {
    return parts.stream().map(Part::volume).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Part {@code number} (from 1) when this Session has it and it is an AMRAP, else empty. */
  public Optional<Part> amrap(int number) {
    if (number < 1 || number > parts.size()) {
      return Optional.empty();
    }
    Part part = parts.get(number - 1);
    return part.format() == PartFormat.AMRAP ? Optional.of(part) : Optional.empty();
  }

  Session withPart(Part part) {
    List<Part> next = new ArrayList<>(parts);
    next.add(part);
    return new Session(id, notes, next);
  }

  /** Adds the Prescription to Part {@code number} (from 1). */
  Session withPrescription(int number, Prescription prescription) {
    if (number < 1 || number > parts.size()) {
      throw new PartNotFoundException(id, number);
    }
    List<Part> next = new ArrayList<>(parts);
    next.set(number - 1, parts.get(number - 1).withPrescription(prescription));
    return new Session(id, notes, next);
  }

  /** Adds the Prescription to the last Part when it is one of straight Sets, else to a new one. */
  Session withPrescription(Prescription prescription) {
    Session session =
        parts.isEmpty() || parts.getLast().format() != PartFormat.SETS ? withPart(Part.sets()) : this;
    return session.withPrescription(session.parts().size(), prescription);
  }

  Session withNotes(String notes) {
    return new Session(id, notes, parts);
  }

  private static Part setsOf(List<Prescription> prescriptions) {
    return new Part(PartFormat.SETS, null, null, null, null, prescriptions);
  }
}
