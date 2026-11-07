package be.ae.springfactory.program;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * An ordered plan of Weeks that a Lifter follows. Immutable: adding a Week yields a new Program. Its
 * name is never blank, and its Weeks are always numbered 1, 2, 3... in order.
 */
public record Program(UUID id, String name, List<Week> weeks) {
  public Program {
    Objects.requireNonNull(id, "id");
    if (name == null || name.isBlank()) {
      throw new BlankProgramNameException();
    }
    name = name.strip();
    weeks = List.copyOf(weeks);
    for (int i = 0; i < weeks.size(); i++) {
      if (weeks.get(i).number() != i + 1) {
        throw new IllegalArgumentException("Weeks must be numbered 1, 2, 3... in order: " + weeks);
      }
    }
  }

  Program withNextWeek() {
    List<Week> next = new ArrayList<>(weeks);
    next.add(new Week(UUID.randomUUID(), weeks.size() + 1, List.of()));
    return new Program(id, name, next);
  }

  Program withWeekChanged(UUID weekId, UnaryOperator<Week> change) {
    return new Program(
        id,
        name,
        weeks.stream().map(week -> week.id().equals(weekId) ? change.apply(week) : week).toList());
  }

  Program withSessionChanged(UUID sessionId, UnaryOperator<Session> change) {
    return new Program(
        id,
        name,
        weeks.stream().map(week -> week.withSessionChanged(sessionId, change)).toList());
  }

  Optional<Session> session(UUID sessionId) {
    return weeks.stream()
        .flatMap(week -> week.sessions().stream())
        .filter(session -> session.id().equals(sessionId))
        .findFirst();
  }

  Week lastWeek() {
    return weeks.getLast();
  }
}
