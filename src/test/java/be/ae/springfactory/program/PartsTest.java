package be.ae.springfactory.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.ae.springfactory.exercise.ExerciseCatalog;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

/** Parts, Sides, work time, notes and Equipment of a Session, through {@link Programs}. */
class PartsTest {

  private static final UUID SWING = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01");
  private static final UUID SNATCH = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c05");
  private static final UUID PUSH_UP = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06");
  private static final UUID WAITER_PRESS =
      UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c09");
  private static final UUID ROW = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0a");
  private static final UUID WALKING_PLANK =
      UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0b");
  private static final UUID SKIPPING = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0c");
  private static final BigDecimal BELL = new BigDecimal("16");

  private final Programs programs = new Programs(new ExerciseCatalog());

  @Test
  void holdsTheWorkoutOfIssue20AsAnEmomARestAndRounds() {
    UUID sessionId = newSessionId();

    programs.addPart(sessionId, Part.emom(20, 4));
    for (Side side : new Side[] {Side.RIGHT, Side.LEFT}) {
      int minute = side == Side.RIGHT ? 1 : 2;
      programs.addPrescription(sessionId, 1, inMinute(SWING, side, BELL, minute));
      programs.addPrescription(sessionId, 1, inMinute(SNATCH, side, BELL, minute));
      programs.addPrescription(sessionId, 1, inMinute(PUSH_UP, null, null, minute));
    }
    programs.addPrescription(
        sessionId, 1, new Prescription(SKIPPING, 1, 1, null, null, 45, null, null, 3));
    programs.addPart(sessionId, Part.rest(60));
    programs.addPart(sessionId, Part.rounds(3));
    programs.addPrescription(sessionId, 3, eachSide(WAITER_PRESS, BELL));
    programs.addPrescription(sessionId, 3, eachSide(ROW, BELL));
    programs.addPrescription(sessionId, 3, eachSide(WALKING_PLANK, null));

    Session session = programs.session(sessionId);

    assertThat(session.parts())
        .extracting(Part::format)
        .containsExactly(PartFormat.EMOM, PartFormat.REST, PartFormat.ROUNDS);
    assertThat(session.parts().get(0).prescriptions()).hasSize(7);
    assertThat(programs.equipment(session))
        .containsExactly("Body weight", "Jump rope", "Kettlebell");
    // EMOM: 20 minutes through 4 is 5 passes; minutes 1 and 2 each do 5 swings and 5 snatches
    // at 16 kg: 2 × 5 × 10 × 16 = 1600 kg. Rounds: 3 × 2 Exercises × (8 + 8) × 16 = 1536 kg.
    assertThat(session.volume()).isEqualByComparingTo("3136");
  }

  @Test
  void anEmomThatDoesNotDivideEvenlyDoesItsFirstMinutesOnceMore() {
    UUID sessionId = newSessionId();
    programs.addPart(sessionId, Part.emom(10, 4));
    programs.addPrescription(
        sessionId, 1, new Prescription(SWING, 1, 5, null, null, null, BELL, null, 1));
    programs.addPrescription(
        sessionId, 1, new Prescription(SWING, 1, 1, null, null, null, BELL, null, 3));

    // Minute 1 is done 3 times, minute 3 twice: 3 × 5 × 16 + 2 × 1 × 16
    assertThat(programs.session(sessionId).volume()).isEqualByComparingTo("272");
  }

  @Test
  void eachSideCountsItsRepsTwiceAndOneSideOnce() {
    assertThat(eachSide(WAITER_PRESS, BELL).volume()).isEqualByComparingTo("256");
    assertThat(new Prescription(SWING, 1, 8, Side.LEFT, null, null, BELL, null, null).volume())
        .isEqualByComparingTo("128");
  }

  @Test
  void aPrescriptionWithoutAPartStartsOrExtendsAPartOfStraightSets() {
    UUID sessionId = newSessionId();
    Prescription swings = new Prescription(SWING, 5, 10, BELL, 60);

    programs.addPrescription(sessionId, swings);
    programs.addPrescription(sessionId, swings);
    programs.addPart(sessionId, Part.rest(120));
    programs.addPrescription(sessionId, swings);

    assertThat(programs.session(sessionId).parts())
        .extracting(Part::format, part -> part.prescriptions().size())
        .containsExactly(
            Tuple.tuple(PartFormat.SETS, 2),
            Tuple.tuple(PartFormat.REST, 0),
            Tuple.tuple(PartFormat.SETS, 1));
  }

  @Test
  void aPartTakesOnlyWhatItsFormatNeeds() {
    assertThatThrownBy(() -> new Part(PartFormat.ROUNDS, null, null, null, null, List.of()))
        .isInstanceOf(InvalidPartException.class)
        .hasMessage("A ROUNDS Part needs its Rounds");
    assertThatThrownBy(() -> Part.rounds(0))
        .isInstanceOf(InvalidPartException.class)
        .hasMessage("Rounds must be at least 1, was 0");
    assertThatThrownBy(() -> new Part(PartFormat.SETS, 3, null, null, null, List.of()))
        .isInstanceOf(InvalidPartException.class)
        .hasMessage("A SETS Part takes no Rounds, was 3");
    assertThatThrownBy(() -> Part.emom(3, 4))
        .isInstanceOf(InvalidPartException.class)
        .hasMessage("An EMOM of 3 minutes cannot rotate through 4 minutes");
    assertThatThrownBy(() -> new Part(PartFormat.REST, null, 10, null, 60, List.of()))
        .isInstanceOf(InvalidPartException.class)
        .hasMessage("A REST Part takes no minutes, was 10");
  }

  @Test
  void aPrescriptionMustFitItsPart() {
    UUID sessionId = newSessionId();
    programs.addPart(sessionId, Part.emom(20, 4));
    programs.addPart(sessionId, Part.rest(60));
    programs.addPart(sessionId, Part.rounds(3));

    assertThatThrownBy(
            () -> programs.addPrescription(sessionId, 1, new Prescription(SWING, 1, 5, BELL, null)))
        .isInstanceOf(InvalidPartException.class)
        .hasMessage("A Prescription in an EMOM needs its minute");
    assertThatThrownBy(() -> programs.addPrescription(sessionId, 1, inMinute(SWING, null, BELL, 5)))
        .isInstanceOf(InvalidPartException.class)
        .hasMessage("This EMOM rotates through 4 minutes, so it has no minute 5");
    assertThatThrownBy(
            () ->
                programs.addPrescription(
                    sessionId, 1, new Prescription(SWING, 1, 5, null, null, null, BELL, 90, 1)))
        .isInstanceOf(InvalidPartException.class)
        .hasMessageStartingWith("A Prescription in an EMOM takes no rest");
    assertThatThrownBy(
            () -> programs.addPrescription(sessionId, 2, new Prescription(SWING, 1, 5, BELL, null)))
        .isInstanceOf(InvalidPartException.class)
        .hasMessage("A rest Part takes no Prescriptions");
    assertThatThrownBy(() -> programs.addPrescription(sessionId, 3, inMinute(SWING, null, BELL, 1)))
        .isInstanceOf(InvalidPartException.class)
        .hasMessage("Only a Prescription in an EMOM has a minute");
    assertThatThrownBy(
            () -> programs.addPrescription(sessionId, 4, new Prescription(SWING, 1, 5, BELL, null)))
        .isInstanceOf(PartNotFoundException.class);
    assertThat(programs.session(sessionId).prescriptions()).isEmpty();
  }

  @Test
  void aTimedExerciseNeedsAWorkTimeAndNoOtherTakesOne() {
    UUID sessionId = newSessionId();

    assertThatThrownBy(
            () ->
                programs.addPrescription(
                    sessionId, new Prescription(SKIPPING, 1, 1, null, null, null)))
        .isInstanceOf(InvalidPrescriptionException.class)
        .hasMessage("Skipping is timed and needs a work time");
    assertThatThrownBy(
            () ->
                programs.addPrescription(
                    sessionId, new Prescription(SWING, 1, 1, null, null, 30, BELL, null, null)))
        .isInstanceOf(InvalidPrescriptionException.class)
        .hasMessage("Swing is not timed and takes no work time, was 30 s");
    assertThatThrownBy(() -> new Prescription(SKIPPING, 1, 1, null, null, 0, null, null, null))
        .isInstanceOf(InvalidPrescriptionException.class)
        .hasMessage("Work time must be at least 1 s, was 0");
  }

  @Test
  void anAmrapHasATimeCapAndItsPrescriptionsHaveNoRestOrMinute() {
    UUID sessionId = newSessionId();
    programs.addPart(sessionId, Part.amrap(12));

    assertThatThrownBy(() -> new Part(PartFormat.AMRAP, null, null, null, null, List.of()))
        .isInstanceOf(InvalidPartException.class)
        .hasMessage("A AMRAP Part needs its minutes");
    assertThatThrownBy(() -> new Part(PartFormat.AMRAP, null, 12, 4, null, List.of()))
        .isInstanceOf(InvalidPartException.class)
        .hasMessage("A AMRAP Part takes no rotation, was 4");
    assertThatThrownBy(
            () -> programs.addPrescription(sessionId, 1, new Prescription(SWING, 1, 5, BELL, 30)))
        .isInstanceOf(InvalidPartException.class)
        .hasMessageStartingWith("A Prescription in an AMRAP takes no rest");
    assertThatThrownBy(() -> programs.addPrescription(sessionId, 1, inMinute(SWING, null, BELL, 1)))
        .isInstanceOf(InvalidPartException.class)
        .hasMessage("Only a Prescription in an EMOM has a minute");

    programs.addPrescription(sessionId, 1, eachSide(WAITER_PRESS, BELL));
    Part amrap = programs.session(sessionId).parts().getFirst();
    assertThat(amrap.repsPerRound()).isEqualTo(16);
    // 2 Rounds of 8+8 at 16 kg, then 9 Reps: 2 × 256 + 9 × 16
    assertThat(amrap.volume(2, 9)).isEqualByComparingTo("656");
    assertThatThrownBy(() -> Part.rounds(3).volume(1, 0))
        .isInstanceOf(InvalidPartException.class)
        .hasMessage("Only an AMRAP Part has a Score, not a ROUNDS Part");
  }

  @Test
  void aRepRangeCountsItsLowEndInPlannedVolume() {
    UUID sessionId = newSessionId();
    Prescription threeToFour =
        new Prescription(SWING, 2, 3, 4, null, null, null, BELL, null, null);

    programs.addPrescription(sessionId, threeToFour);

    assertThat(programs.session(sessionId).prescriptions()).containsExactly(threeToFour);
    assertThat(programs.session(sessionId).volume()).isEqualByComparingTo("96");
    assertThatThrownBy(() -> new Prescription(SWING, 1, 4, 4, null, null, null, BELL, null, null))
        .isInstanceOf(InvalidPrescriptionException.class)
        .hasMessage("A rep range must end above its 4 Reps, was 4-4");
  }

  @Test
  void notesAreStrippedAndBlankNotesClearThem() {
    UUID sessionId = newSessionId();

    assertThat(programs.changeNotes(sessionId, "  Keep intensity at 70-80%. ").notes())
        .isEqualTo("Keep intensity at 70-80%.");
    assertThat(programs.changeNotes(sessionId, " ").notes()).isNull();
    assertThatThrownBy(() -> programs.changeNotes(UUID.randomUUID(), "x"))
        .isInstanceOf(SessionNotFoundException.class);
  }

  private UUID newSessionId() {
    UUID programId = programs.create("Kettlebell EMOM").id();
    return programs.addSession(programs.addWeek(programId).id()).id();
  }

  private static Prescription inMinute(UUID exercise, Side side, BigDecimal bell, int minute) {
    return new Prescription(exercise, 1, 5, side, null, null, bell, null, minute);
  }

  private static Prescription eachSide(UUID exercise, BigDecimal bell) {
    return new Prescription(exercise, 1, 8, Side.EACH, null, null, bell, null, null);
  }
}
