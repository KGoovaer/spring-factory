package be.ae.springfactory.program;

/** The JSON shape of a Week: {number}. */
record WeekResponse(int number) {

  static WeekResponse of(Week week) {
    return new WeekResponse(week.number());
  }
}
