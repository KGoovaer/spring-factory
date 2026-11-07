package be.ae.springfactory.log;

/**
 * A Logged Session or one of its Sets broke a rule: no date, no Sets, Reps or Bell weight too low,
 * or a Bell that does not fit the Exercise.
 */
public class InvalidLoggedSessionException extends RuntimeException {
  InvalidLoggedSessionException(String message) {
    super(message);
  }
}
