package be.ae.springfactory.program;

import java.util.UUID;

/** The Session has no Part with this number. */
public class PartNotFoundException extends RuntimeException {
  PartNotFoundException(UUID sessionId, int number) {
    super("Session " + sessionId + " has no Part " + number);
  }
}
