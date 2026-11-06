package be.ae.springfactory.program;

/** A numbered block of a Program. Its number (1, 2, 3...) identifies it within the Program. */
public record Week(int number) {
  public Week {
    if (number < 1) {
      throw new IllegalArgumentException("Week number starts at 1, was " + number);
    }
  }
}
