package be.ae.springfactory.program;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * An ordered plan of Weeks that a Lifter follows. Immutable: adding a Week yields a new Program. Its
 * Weeks are always numbered 1, 2, 3... in order.
 */
public record Program(UUID id, String name, List<Week> weeks) {
  public Program {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    weeks = List.copyOf(weeks);
    for (int i = 0; i < weeks.size(); i++) {
      if (weeks.get(i).number() != i + 1) {
        throw new IllegalArgumentException("Weeks must be numbered 1, 2, 3... in order: " + weeks);
      }
    }
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
