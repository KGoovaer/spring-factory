package be.ae.springfactory.log;

/**
 * What a Lifter did in an AMRAP Part: {@code rounds} whole Rounds, then {@code extraReps} Reps into
 * the next one. {@code part} is the number of the AMRAP Part in the Session, from 1. All three are
 * at least 0, the Part at least 1. Whether the extra Reps stay short of a whole Round depends on
 * the planned Part, so {@link LoggedSessions} checks that.
 */
public record Score(int part, int rounds, int extraReps) {
  public Score {
    if (part < 1) {
      throw new InvalidLoggedSessionException("A Score needs the number of its Part, was " + part);
    }
    if (rounds < 0) {
      throw new InvalidLoggedSessionException("Rounds must not be negative, was " + rounds);
    }
    if (extraReps < 0) {
      throw new InvalidLoggedSessionException("Extra Reps must not be negative, was " + extraReps);
    }
  }
}
