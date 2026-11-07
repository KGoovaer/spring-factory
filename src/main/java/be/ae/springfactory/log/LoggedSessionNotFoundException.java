package be.ae.springfactory.log;

import java.util.UUID;

/** The Session exists but has not been logged. */
public class LoggedSessionNotFoundException extends RuntimeException {
  LoggedSessionNotFoundException(UUID sessionId) {
    super("Session " + sessionId + " has no Logged Session");
  }
}
