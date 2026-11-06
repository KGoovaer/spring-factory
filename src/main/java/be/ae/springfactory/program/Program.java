package be.ae.springfactory.program;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** An ordered plan of Weeks that a Lifter follows. Immutable: adding a Week yields a new Program. */
public record Program(UUID id, String name, List<Week> weeks) {
  public Program {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    weeks = List.copyOf(weeks);
  }

  Program withNextWeek() {
    List<Week> next = new ArrayList<>(weeks);
    next.add(new Week(weeks.size() + 1));
    return new Program(id, name, next);
  }

  Week lastWeek() {
    return weeks.getLast();
  }
}
