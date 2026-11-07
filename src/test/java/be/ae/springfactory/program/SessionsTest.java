package be.ae.springfactory.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.ae.springfactory.exercise.ExerciseCatalog;
import be.ae.springfactory.exercise.ExerciseNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Sessions in a Week and Prescriptions in a Session, through {@link Programs}. */
class SessionsTest {

  private static final UUID SWING = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01");
  private static final UUID GET_UP = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c04");

  private final Programs programs = new Programs(new ExerciseCatalog());

  @Test
  void everyWeekGetsItsOwnIdAndStartsWithNoSessions() {
    UUID id = programs.create("Giant").id();
    Week first = programs.addWeek(id);
    Week second = programs.addWeek(programs.create("Other").id());

    assertThat(first.id()).isNotNull().isNotEqualTo(second.id());
    assertThat(first.sessions()).isEmpty();
  }

  @Test
  void sessionsAreAddedToTheirWeekInOrder() {
    UUID id = programs.create("Giant").id();
    Week week1 = programs.addWeek(id);
    Week week2 = programs.addWeek(id);

    Session monday = programs.addSession(week1.id());
    Session wednesday = programs.addSession(week1.id());
    Session other = programs.addSession(week2.id());

    assertThat(monday.id()).isNotEqualTo(wednesday.id());
    assertThat(monday.prescriptions()).isEmpty();
    List<Week> weeks = programs.get(id).weeks();
    assertThat(weeks.get(0).sessions()).containsExactly(monday, wednesday);
    assertThat(weeks.get(1).sessions()).containsExactly(other);
  }

  @Test
  void prescriptionsAreAddedToTheirSessionInOrder() {
    UUID id = programs.create("Simple & Sinister").id();
    UUID weekId = programs.addWeek(id).id();
    Session session = programs.addSession(weekId);
    Session untouched = programs.addSession(weekId);
    Prescription swings = new Prescription(SWING, 10, 10, new BigDecimal("24"), 60);
    Prescription getUps = new Prescription(GET_UP, 5, 1, new BigDecimal("10.50"), null);

    assertThat(programs.addPrescription(session.id(), swings)).isEqualTo(swings);
    programs.addPrescription(session.id(), getUps);

    List<Session> sessions = programs.get(id).weeks().getFirst().sessions();
    assertThat(sessions.get(0).prescriptions()).containsExactly(swings, getUps);
    assertThat(sessions.get(1)).isEqualTo(untouched);
  }

  @Test
  void theBellWeightKeepsTheScaleItWasGiven() {
    Session session = programs.addSession(programs.addWeek(programs.create("P").id()).id());

    Prescription added =
        programs.addPrescription(
            session.id(), new Prescription(SWING, 1, 1, new BigDecimal("10.50"), null));

    assertThat(added.bellKg().toPlainString()).isEqualTo("10.50");
  }

  @Test
  void anUnknownWeekOrSessionIsNotFound() {
    Prescription swings = new Prescription(SWING, 1, 1, BigDecimal.ONE, null);

    assertThatThrownBy(() -> programs.addSession(UUID.randomUUID()))
        .isInstanceOf(WeekNotFoundException.class);
    assertThatThrownBy(() -> programs.addPrescription(UUID.randomUUID(), swings))
        .isInstanceOf(SessionNotFoundException.class);
  }

  @Test
  void aPrescriptionForAnUnknownExerciseIsRejectedAndNotStored() {
    UUID id = programs.create("P").id();
    Session session = programs.addSession(programs.addWeek(id).id());
    Prescription unknown = new Prescription(UUID.randomUUID(), 1, 1, BigDecimal.ONE, null);

    assertThatThrownBy(() -> programs.addPrescription(session.id(), unknown))
        .isInstanceOf(ExerciseNotFoundException.class);
    assertThat(programs.get(id).weeks().getFirst().sessions().getFirst().prescriptions()).isEmpty();
  }

  @Test
  void aPrescriptionKeepsItsLimits() {
    BigDecimal kg = new BigDecimal("16");

    assertThatThrownBy(() -> new Prescription(SWING, 0, 1, kg, null))
        .isInstanceOf(InvalidPrescriptionException.class);
    assertThatThrownBy(() -> new Prescription(SWING, 1, 0, kg, null))
        .isInstanceOf(InvalidPrescriptionException.class);
    assertThatThrownBy(() -> new Prescription(SWING, 1, 1, BigDecimal.ZERO, null))
        .isInstanceOf(InvalidPrescriptionException.class);
    assertThatThrownBy(() -> new Prescription(SWING, 1, 1, null, null))
        .isInstanceOf(InvalidPrescriptionException.class);
    assertThatThrownBy(() -> new Prescription(SWING, 1, 1, kg, -1))
        .isInstanceOf(InvalidPrescriptionException.class);
    assertThatThrownBy(() -> new Prescription(null, 1, 1, kg, null))
        .isInstanceOf(InvalidPrescriptionException.class);
    assertThat(new Prescription(SWING, 1, 1, kg, 0).restSeconds()).isZero();
  }

  @Test
  void callersCannotChangeSessionsOrPrescriptions() {
    UUID id = programs.create("P").id();
    Session session = programs.addSession(programs.addWeek(id).id());
    Week week = programs.get(id).weeks().getFirst();

    assertThatThrownBy(() -> week.sessions().add(session))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> session.prescriptions().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void concurrentlyAddedPrescriptionsAreAllKept() throws Exception {
    UUID id = programs.create("Busy").id();
    UUID weekId = programs.addWeek(id).id();
    UUID first = programs.addSession(weekId).id();
    UUID second = programs.addSession(weekId).id();
    int count = 200;
    List<Callable<Object>> adds =
        IntStream.range(0, count)
            .<Callable<Object>>mapToObj(
                i ->
                    () ->
                        programs.addPrescription(
                            i % 2 == 0 ? first : second,
                            new Prescription(SWING, 1, i + 1, BigDecimal.ONE, null)))
            .toList();

    ExecutorService pool = Executors.newFixedThreadPool(8);
    try {
      for (var future : pool.invokeAll(adds)) {
        future.get();
      }
    } finally {
      pool.shutdown();
    }
    List<Session> sessions = programs.get(id).weeks().getFirst().sessions();
    assertThat(sessions.get(0).prescriptions()).hasSize(count / 2);
    assertThat(sessions.get(1).prescriptions()).hasSize(count / 2);
  }
}
