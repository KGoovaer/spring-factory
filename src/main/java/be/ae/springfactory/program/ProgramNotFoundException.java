package be.ae.springfactory.program;

import java.util.UUID;

/** No Program exists with the given id. */
public class ProgramNotFoundException extends RuntimeException {
  public ProgramNotFoundException(UUID id) {
    super("No Program with id " + id);
  }
}
