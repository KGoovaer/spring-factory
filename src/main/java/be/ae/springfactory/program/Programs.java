package be.ae.springfactory.program;

import be.ae.springfactory.exercise.Exercise;
import be.ae.springfactory.exercise.ExerciseCatalog;
import be.ae.springfactory.exercise.ExerciseUsage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Component;

/**
 * The Programs Lifters have created, with their Weeks, Sessions, Parts and Prescriptions. Kept in memory
 * and lost on restart. Program, Week and Session ids are random UUIDs, for the reasons
 * docs/adr/0001-exercise-ids-are-uuids.md gives for Exercise ids; unlike seeded Exercises, they are
 * not fixed. Safe for concurrent use: two callers adding a Week to the same Program never get the
 * same Week number, and no added Session or Prescription is lost.
 */
@Component
public class Programs implements ExerciseUsage {

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
    return programOf(programOfSession, id)
        .flatMap(program -> program.session(id))
        .orElseThrow(() -> new SessionNotFoundException(id));
  }

  /** The Week with its Sessions. Throws {@link WeekNotFoundException} for an unknown id. */
  public Week week(UUID id) {
    return programOf(programOfWeek, id)
        .flatMap(program -> program.week(id))
        .orElseThrow(() -> new WeekNotFoundException(id));
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
    Session session = new Session(UUID.randomUUID(), null, List.of());
    update(
        programId,
        program -> program.withWeekChanged(weekId, week -> week.withSession(session)),
        () -> new WeekNotFoundException(weekId));
    programOfSession.put(session.id(), programId);
    return session;
  }

  /**
   * Appends the Prescription to the last Part of the Session when that Part is one of straight
   * Sets, else to a new Part of straight Sets, and returns it. Throws {@link
   * SessionNotFoundException} for an unknown Session and {@link
   * be.ae.springfactory.exercise.ExerciseNotFoundException} for an Exercise that is not in the
   * catalog. Throws {@link InvalidPrescriptionException} when the Prescription does not fit its
   * Exercise: a kettlebell Exercise without a Bell or a bodyweight Exercise with one, a hold
   * Exercise without a hold time or another Exercise with one, a timed Exercise without a work time
   * or another Exercise with one. Throws {@link InvalidPartException} when it has a minute.
   */
  public Prescription addPrescription(UUID sessionId, Prescription prescription) {
    fitsExercise(prescription);
    updateSession(sessionId, session -> session.withPrescription(prescription));
    return prescription;
  }

  /**
   * Appends the Prescription to Part {@code partNumber} (from 1) of the Session and returns it.
   * Throws what {@link #addPrescription(UUID, Prescription)} throws, {@link PartNotFoundException}
   * when the Session has no such Part, and {@link InvalidPartException} when the Prescription does
   * not fit the Part: a rest Part, an EMOM Prescription without a minute of its rotation, or a
   * minute outside an EMOM.
   */
  public Prescription addPrescription(UUID sessionId, int partNumber, Prescription prescription) {
    fitsExercise(prescription);
    updateSession(sessionId, session -> session.withPrescription(partNumber, prescription));
    return prescription;
  }

  /**
   * Appends the Part to the Session and returns the changed Session. Throws {@link
   * SessionNotFoundException} for an unknown Session.
   */
  public Session addPart(UUID sessionId, Part part) {
    return updateSession(sessionId, session -> session.withPart(part));
  }

  /**
   * Replaces the notes of the Session (blank clears them) and returns the changed Session. Throws
   * {@link SessionNotFoundException} for an unknown Session.
   */
  public Session changeNotes(UUID sessionId, String notes) {
    return updateSession(sessionId, session -> session.withNotes(notes));
  }

  /**
   * The Equipment the Session needs, derived from its Exercises and sorted by name: "Kettlebell"
   * for a kettlebell Exercise, the gear of a bodyweight Exercise that has some, and "Body weight"
   * for one that has none.
   */
  public List<String> equipment(Session session) {
    return session.prescriptions().stream()
        .map(prescription -> exercises.get(prescription.exerciseId()).equipment())
        .distinct()
        .sorted()
        .toList();
  }

  /** Whether a Prescription of any Program is of the Exercise with this id. */
  @Override
  public boolean uses(UUID exerciseId) {
    return programs.values().stream()
        .flatMap(program -> program.weeks().stream())
        .flatMap(week -> week.sessions().stream())
        .flatMap(session -> session.prescriptions().stream())
        .anyMatch(prescription -> prescription.exerciseId().equals(exerciseId));
  }

  private void fitsExercise(Prescription prescription) {
    Exercise exercise = exercises.get(prescription.exerciseId());
    exercise
        .mismatch(prescription.bellKg(), prescription.holdSeconds(), prescription.workSeconds())
        .ifPresent(
            reason -> {
              throw new InvalidPrescriptionException(reason);
            });
  }

  /** Changes the Session atomically within its Program and returns the changed Session. */
  private Session updateSession(UUID sessionId, UnaryOperator<Session> change) {
    UUID programId = programOfSession.get(sessionId);
    if (programId == null) {
      throw new SessionNotFoundException(sessionId);
    }
    AtomicReference<Session> changed = new AtomicReference<>();
    update(
        programId,
        program ->
            program.withSessionChanged(
                sessionId, session -> changed.updateAndGet(ignored -> change.apply(session))),
        () -> new SessionNotFoundException(sessionId));
    return changed.get();
  }

  /** Replaces the Program atomically with the changed one, or throws when it has gone. */
  private void update(
      UUID programId, UnaryOperator<Program> change, Supplier<RuntimeException> notFound) {
    if (programs.computeIfPresent(programId, (key, program) -> change.apply(program)) == null) {
      throw notFound.get();
    }
  }

  /** The Program that this index says holds the Week or Session {@code id}, if any. */
  private Optional<Program> programOf(Map<UUID, UUID> index, UUID id) {
    return Optional.ofNullable(index.get(id)).map(programs::get);
  }
}
