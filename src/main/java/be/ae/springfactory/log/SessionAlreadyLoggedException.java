package be.ae.springfactory.log;

import java.util.UUID;

/**
 * The Session has a Logged Session already; a Session is logged at most once, and a Lifter corrects
 * that Logged Session instead of logging it again.
 */
public class SessionAlreadyLoggedException extends RuntimeException {
  SessionAlreadyLoggedException(UUID sessionId) {
    super("Session " + sessionId + " has a Logged Session already");
  }
}
