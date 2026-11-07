package be.ae.springfactory.log;

import java.util.UUID;

/** The Session has a Logged Session already; a Session is logged at most once. */
public class SessionAlreadyLoggedException extends RuntimeException {
  SessionAlreadyLoggedException(UUID sessionId) {
    super("Session " + sessionId + " has a Logged Session already");
  }
}
