package be.ae.springfactory.program;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * The Programs Lifters have created. Kept in memory and lost on restart. Ids are random UUIDs, for
 * the reasons docs/adr/0001-exercise-ids-are-uuids.md gives for Exercise ids; unlike seeded
 * Exercises, a Program's id is not fixed. Safe for concurrent use: two callers adding a Week to the
 * same Program never get the same Week number.
 */
@Component
public class Programs {

  private final Map<UUID, Program> programs = new ConcurrentHashMap<>();

  /** Creates a Program with no Weeks. The name must not be blank. */
  public Program create(String name) {
    if (name == null || name.isBlank()) {
      throw new BlankProgramNameException();
    }
    Program program = new Program(UUID.randomUUID(), name.strip(), List.of());
    programs.put(program.id(), program);
    return program;
  }

  /** The Program with its Weeks in order. */
  public Program get(UUID id) {
    Program program = programs.get(id);
    if (program == null) {
      throw new ProgramNotFoundException(id);
    }
    return program;
  }

  /** Appends the next Week to the Program, numbered one higher than the last, and returns it. */
  public Week addWeek(UUID id) {
    Program updated = programs.computeIfPresent(id, (key, program) -> program.withNextWeek());
    if (updated == null) {
      throw new ProgramNotFoundException(id);
    }
    return updated.lastWeek();
  }
}
