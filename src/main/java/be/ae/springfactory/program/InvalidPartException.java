package be.ae.springfactory.program;

/**
 * A Part broke one of its rules: a Rounds, minutes, rotation or rest that its format needs is
 * missing or below 1, one it does not take is given, or a Prescription does not fit it.
 */
public class InvalidPartException extends RuntimeException {
  InvalidPartException(String message) {
    super(message);
  }
}
