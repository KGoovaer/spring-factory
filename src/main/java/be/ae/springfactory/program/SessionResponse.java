package be.ae.springfactory.program;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

/** The JSON shape of a Session: {id, notes, equipment: ["Kettlebell", ...], parts: [...]}. */
record SessionResponse(UUID id, String notes, List<String> equipment, List<PartResponse> parts) {

  static SessionResponse of(Session session, Function<Session, List<String>> equipment) {
    List<Part> parts = session.parts();
    return new SessionResponse(
        session.id(),
        session.notes(),
        equipment.apply(session),
        IntStream.range(0, parts.size()).mapToObj(i -> PartResponse.of(i + 1, parts.get(i))).toList());
  }
}
