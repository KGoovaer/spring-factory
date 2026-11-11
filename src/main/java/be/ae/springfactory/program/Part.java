package be.ae.springfactory.program;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One section of a Session, done in one {@link PartFormat} (see
 * docs/adr/0005-a-session-is-made-of-parts.md):
 *
 * <ul>
 *   <li>{@code SETS}: its Prescriptions, each for its Sets. Takes no Rounds, minutes, rotation or
 *       rest.
 *   <li>{@code ROUNDS}: {@code rounds} (at least 1) Rounds; each Round does every Prescription for
 *       its Sets, in order.
 *   <li>{@code EMOM}: every minute on the minute for {@code minutes} minutes, cycling through a
 *       rotation of {@code rotation} minutes (both at least 1, the rotation at most the minutes).
 *       Each Prescription names the minute of the rotation it is done in; a minute without
 *       Prescriptions is a rest minute.
 *   <li>{@code AMRAP}: as many Rounds as possible in {@code minutes} (at least 1), the time cap.
 *       Its planned Volume counts one Round; what the Lifter did is logged as a Score of Rounds and
 *       extra Reps (see docs/adr/0008-an-amrap-is-logged-as-a-score.md).
 *   <li>{@code REST}: {@code restSeconds} (at least 1) of rest and no Prescriptions.
 * </ul>
 *
 * Only an EMOM Prescription has a minute, and neither an EMOM nor an AMRAP Prescription has rest
 * of its own. Immutable: adding a Prescription yields a new Part.
 */
public record Part(
    PartFormat format,
    Integer rounds,
    Integer minutes,
    Integer rotation,
    Integer restSeconds,
    List<Prescription> prescriptions) {
  public Part {
    if (format == null) {
      throw new InvalidPartException("A Part needs a format");
    }
    prescriptions = List.copyOf(prescriptions);
    takes(format == PartFormat.ROUNDS, "Rounds", rounds, format);
    takes(format == PartFormat.EMOM || format == PartFormat.AMRAP, "minutes", minutes, format);
    takes(format == PartFormat.EMOM, "rotation", rotation, format);
    takes(format == PartFormat.REST, "rest", restSeconds, format);
    if (format == PartFormat.EMOM && rotation > minutes) {
      throw new InvalidPartException(
          "An EMOM of " + minutes + " minutes cannot rotate through " + rotation + " minutes");
    }
    for (Prescription prescription : prescriptions) {
      fits(format, rotation, prescription);
    }
  }

  /** A Part of straight Sets, with no Prescriptions yet. */
  public static Part sets() {
    return new Part(PartFormat.SETS, null, null, null, null, List.of());
  }

  /** A Part of {@code rounds} Rounds, with no Prescriptions yet. */
  public static Part rounds(int rounds) {
    return new Part(PartFormat.ROUNDS, rounds, null, null, null, List.of());
  }

  /** An EMOM of {@code minutes} minutes through a rotation of {@code rotation} minutes. */
  public static Part emom(int minutes, int rotation) {
    return new Part(PartFormat.EMOM, null, minutes, rotation, null, List.of());
  }

  /** As many Rounds as possible in {@code minutes} minutes, with no Prescriptions yet. */
  public static Part amrap(int minutes) {
    return new Part(PartFormat.AMRAP, null, minutes, null, null, List.of());
  }

  /** {@code seconds} of rest between Parts. */
  public static Part rest(int seconds) {
    return new Part(PartFormat.REST, null, null, null, seconds, List.of());
  }

  /** Planned Volume of this Part: each Prescription's Volume times how often it is done. */
  public BigDecimal volume() {
    return prescriptions.stream()
        .map(p -> p.volume().multiply(BigDecimal.valueOf(timesDone(p))))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * The Reps of one Round of an AMRAP, the Prescriptions' Reps summed (twice on Each side, the low
   * end of a rep range). Extra Reps of a Score are fewer than this.
   */
  public long repsPerRound() {
    return prescriptions.stream().mapToLong(Prescription::repsDone).sum();
  }

  /**
   * Whether {@code extraReps} Reps into the next Round of an AMRAP stay short of a whole one, as
   * the extra Reps of a Score must. No extra Reps always do.
   */
  public boolean isShortOfARound(int extraReps) {
    return extraReps == 0 || extraReps < repsPerRound();
  }

  /**
   * Volume of an AMRAP Score: {@code rounds} whole Rounds, then {@code extraReps} Reps taken
   * through the Prescriptions in order, each at its own Bell. Throws {@link InvalidPartException}
   * for a Part that is not an AMRAP, negative Rounds or Reps, or extra Reps that make a whole Round.
   */
  public BigDecimal volume(int rounds, int extraReps) {
    if (format != PartFormat.AMRAP) {
      throw new InvalidPartException("Only an AMRAP Part has a Score, not a " + format + " Part");
    }
    if (rounds < 0 || extraReps < 0) {
      throw new InvalidPartException(
          "A Score cannot be negative, was " + rounds + " Rounds + " + extraReps + " Reps");
    }
    if (!isShortOfARound(extraReps)) {
      throw new InvalidPartException(
          extraReps
              + " extra Reps make a whole Round of "
              + repsPerRound()
              + " Reps: count it as a Round");
    }
    BigDecimal volume = volume().multiply(BigDecimal.valueOf(rounds));
    long left = extraReps;
    for (Prescription prescription : prescriptions) {
      long reps = Math.min(left, prescription.repsDone());
      volume = volume.add(prescription.volume(reps));
      left -= reps;
    }
    return volume;
  }

  /**
   * How often this Part does the Prescription: once in a Sets Part, once per Round in a Rounds
   * Part, and once per pass of the rotation that reaches its minute in an EMOM.
   */
  private int timesDone(Prescription prescription) {
    return switch (format) {
      case SETS, AMRAP -> 1;
      case ROUNDS -> rounds;
      case EMOM ->
          minutes / rotation + (prescription.minute() <= minutes % rotation ? 1 : 0);
      case REST -> 0;
    };
  }

  Part withPrescription(Prescription prescription) {
    List<Prescription> next = new ArrayList<>(prescriptions);
    next.add(prescription);
    return new Part(format, rounds, minutes, rotation, restSeconds, next);
  }

  private static void fits(PartFormat format, Integer rotation, Prescription prescription) {
    Objects.requireNonNull(prescription, "prescription");
    if (format == PartFormat.REST) {
      throw new InvalidPartException("A rest Part takes no Prescriptions");
    }
    if (format == PartFormat.EMOM) {
      if (prescription.minute() == null) {
        throw new InvalidPartException("A Prescription in an EMOM needs its minute");
      }
      if (prescription.minute() > rotation) {
        throw new InvalidPartException(
            "This EMOM rotates through "
                + rotation
                + " minutes, so it has no minute "
                + prescription.minute());
      }
      if (prescription.restSeconds() != null) {
        throw new InvalidPartException(
            "A Prescription in an EMOM takes no rest: the Lifter rests for what is left of the"
                + " minute");
      }
    } else if (format == PartFormat.AMRAP && prescription.restSeconds() != null) {
      throw new InvalidPartException(
          "A Prescription in an AMRAP takes no rest: the Lifter rests at will within the time cap");
    }
    if (format != PartFormat.EMOM && prescription.minute() != null) {
      throw new InvalidPartException("Only a Prescription in an EMOM has a minute");
    }
  }

  private static void takes(boolean needed, String what, Integer value, PartFormat format) {
    if (needed && value == null) {
      throw new InvalidPartException("A " + format + " Part needs its " + what);
    }
    if (needed && value < 1) {
      throw new InvalidPartException(what + " must be at least 1, was " + value);
    }
    if (!needed && value != null) {
      throw new InvalidPartException("A " + format + " Part takes no " + what + ", was " + value);
    }
  }
}
