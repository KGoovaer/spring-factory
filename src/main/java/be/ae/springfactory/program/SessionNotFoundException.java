package be.ae.springfactory.program;

import java.util.UUID;

/** No Session in any Week has the given id. */
public class SessionNotFoundException extends RuntimeException {
  SessionNotFoundException(UUID id) {
    super("No Session with id " + id);
  }
}
