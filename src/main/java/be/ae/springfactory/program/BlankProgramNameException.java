package be.ae.springfactory.program;

/** A Program was given a missing or blank name. */
public class BlankProgramNameException extends RuntimeException {
  BlankProgramNameException() {
    super("A Program needs a name");
  }
}
