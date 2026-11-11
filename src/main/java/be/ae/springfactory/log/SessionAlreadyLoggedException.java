package be.ae.springfactory.log;

/**
 * The Session has a Logged Session already; a Session is logged at most once, and a Lifter corrects
 * that Logged Session instead of logging it again. The message is shown to the Lifter, so it names
 * no id.
 */
public class SessionAlreadyLoggedException extends RuntimeException {
  SessionAlreadyLoggedException() {
    super("This Session has a Logged Session already");
  }
}
