package be.ae.springfactory.program;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * A numbered block of a Program that holds its planned Sessions. Its id is a random UUID, unique
 * across Programs; its number (1, 2, 3...) gives its place within the Program. Immutable: adding a
 * Session yields a new Week.
 */
public record Week(UUID id, int number, List<Session> sessions) {
  public Week {
    Objects.requireNonNull(id, "id");
    if (number < 1) {
      throw new IllegalArgumentException("Week number starts at 1, was " + number);
    }
    sessions = List.copyOf(sessions);
  }

  Week withSession(Session session) {
    List<Session> next = new ArrayList<>(sessions);
    next.add(session);
    return new Week(id, number, next);
  }

  boolean hasSession(UUID sessionId) {
    return sessions.stream().anyMatch(session -> session.id().equals(sessionId));
  }

  Week withSessionChanged(UUID sessionId, UnaryOperator<Session> change) {
    return new Week(
        id,
        number,
        sessions.stream()
            .map(session -> session.id().equals(sessionId) ? change.apply(session) : session)
            .toList());
  }
}
