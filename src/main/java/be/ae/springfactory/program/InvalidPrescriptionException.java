package be.ae.springfactory.program;

/** A Prescription broke one of its rules: Sets, Reps or Bell weight too low, or negative rest. */
public class InvalidPrescriptionException extends RuntimeException {
  InvalidPrescriptionException(String message) {
    super(message);
  }
}
