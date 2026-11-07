package be.ae.springfactory.program;

import be.ae.springfactory.exercise.ExerciseCatalog;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Component;

/**
 * The Programs Lifters have created, with their Weeks, Sessions and Prescriptions. Kept in memory
 * and lost on restart. Program, Week and Session ids are random UUIDs, for the reasons
 * docs/adr/0001-exercise-ids-are-uuids.md gives for Exercise ids; unlike seeded Exercises, they are
 * not fixed. Safe for concurrent use: two callers adding a Week to the same Program never get the
 * same Week number, and no added Session or Prescription is lost.
 */
@Component
public class Programs {

  private final Map<UUID, Program> programs = new ConcurrentHashMap<>();
  private final Map<UUID, UUID> programOfWeek = new ConcurrentHashMap<>();
  private final Map<UUID, UUID> programOfSession = new ConcurrentHashMap<>();
  private final ExerciseCatalog exercises;

  public Programs(ExerciseCatalog exercises) {
    this.exercises = exercises;
  }

  /** Creates a Program with no Weeks. The name must not be blank. */
  public Program create(String name) {
    Program program = new Program(UUID.randomUUID(), name, List.of());
    programs.put(program.id(), program);
    return program;
  }

  /** The Program with its Weeks in order, each with its Sessions and their Prescriptions. */
  public Program get(UUID id) {
    Program program = programs.get(id);
    if (program == null) {
      throw new ProgramNotFoundException(id);
    }
    return program;
  }

  /** The Session with its Prescriptions. Throws {@link SessionNotFoundException} for an unknown id. */
  public Session session(UUID id) {
    UUID programId = programOfSession.get(id);
    Program program = programId == null ? null : programs.get(programId);
    if (program == null) {
      throw new SessionNotFoundException(id);
    }
    return program.session(id).orElseThrow(() -> new SessionNotFoundException(id));
  }

  /** Appends the next Week to the Program, numbered one higher than the last, and returns it. */
  public Week addWeek(UUID id) {
    Program updated = programs.computeIfPresent(id, (key, program) -> program.withNextWeek());
    if (updated == null) {
      throw new ProgramNotFoundException(id);
    }
    Week week = updated.lastWeek();
    programOfWeek.put(week.id(), id);
    return week;
  }

  /** Appends an empty Session to the Week and returns it. */
  public Session addSession(UUID weekId) {
    UUID programId = programOfWeek.get(weekId);
    if (programId == null) {
      throw new WeekNotFoundException(weekId);
    }
    Session session = new Session(UUID.randomUUID(), List.of());
    update(
        programId,
        program -> program.withWeekChanged(weekId, week -> week.withSession(session)),
        () -> new WeekNotFoundException(weekId));
    programOfSession.put(session.id(), programId);
    return session;
  }

  /**
   * Appends the Prescription to the Session and returns it. Throws {@link SessionNotFoundException}
   * for an unknown Session and {@link be.ae.springfactory.exercise.ExerciseNotFoundException} for
   * an Exercise that is not in the catalog.
   */
  public Prescription addPrescription(UUID sessionId, Prescription prescription) {
    UUID programId = programOfSession.get(sessionId);
    if (programId == null) {
      throw new SessionNotFoundException(sessionId);
    }
    exercises.get(prescription.exerciseId());
    update(
        programId,
        program ->
            program.withSessionChanged(
                sessionId, session -> session.withPrescription(prescription)),
        () -> new SessionNotFoundException(sessionId));
    return prescription;
  }

  /** Replaces the Program atomically with the changed one, or throws when it has gone. */
  private void update(
      UUID programId, UnaryOperator<Program> change, Supplier<RuntimeException> notFound) {
    if (programs.computeIfPresent(programId, (key, program) -> change.apply(program)) == null) {
      throw notFound.get();
    }
  }
}
