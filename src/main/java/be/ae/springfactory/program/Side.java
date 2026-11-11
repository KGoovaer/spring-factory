package be.ae.springfactory.program;

/**
 * Which side of the body a Prescription or Set works: the {@code LEFT} or {@code RIGHT} side only,
 * or {@code EACH} side in turn, written 8+8: the Reps on the left, then the same Reps on the right.
 * A Prescription or Set without a Side is done with both hands or needs no side.
 */
public enum Side {
  LEFT,
  RIGHT,
  EACH;

  /** How many times each Rep is done: twice for {@code EACH}, else once. */
  public int repsFactor() {
    return this == EACH ? 2 : 1;
  }

  /** The Reps factor of an optional Side: 1 when there is none. */
  public static int repsFactor(Side side) {
    return side == null ? 1 : side.repsFactor();
  }
}
