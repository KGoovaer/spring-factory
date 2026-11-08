package be.ae.springfactory.program;

/**
 * A Prescription broke one of its rules: Sets, Reps, hold time or Bell weight too low, negative
 * rest, or a Bell or hold time that does not fit the Exercise.
 */
public class InvalidPrescriptionException extends RuntimeException {
  InvalidPrescriptionException(String message) {
    super(message);
  }
}
