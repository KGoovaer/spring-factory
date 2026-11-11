package be.ae.springfactory.program;

/**
 * How a Part is done. {@code SETS}: each Prescription for its Sets, one after the other. {@code
 * ROUNDS}: a number of Rounds through all its Prescriptions in order. {@code EMOM}: every minute on
 * the minute, for a number of minutes, through a rotation of minutes. {@code REST}: only rest,
 * between the other Parts.
 */
public enum PartFormat {
  SETS,
  ROUNDS,
  EMOM,
  REST
}
