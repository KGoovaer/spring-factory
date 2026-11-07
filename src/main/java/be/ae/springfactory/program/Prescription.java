package be.ae.springfactory.program;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * What a Session asks for one Exercise: Sets of Reps with a Bell of {@code bellKg} kilograms, and
 * optionally {@code restSeconds} of rest. Sets and Reps are at least 1, the Bell weight is above 0
 * and rest, when given, is not negative. The Bell weight keeps the scale it was given (10.50 stays
 * 10.50).
 */
public record Prescription(
    UUID exerciseId, int sets, int reps, BigDecimal bellKg, Integer restSeconds) {
  public Prescription {
    if (exerciseId == null) {
      throw new InvalidPrescriptionException("A Prescription needs an Exercise");
    }
    if (sets < 1) {
      throw new InvalidPrescriptionException("Sets must be at least 1, was " + sets);
    }
    if (reps < 1) {
      throw new InvalidPrescriptionException("Reps must be at least 1, was " + reps);
    }
    if (bellKg == null || bellKg.signum() <= 0) {
      throw new InvalidPrescriptionException("Bell weight must be above 0 kg, was " + bellKg);
    }
    if (restSeconds != null && restSeconds < 0) {
      throw new InvalidPrescriptionException("Rest must not be negative, was " + restSeconds);
    }
  }
}
