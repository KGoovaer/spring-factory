package be.ae.springfactory.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.ae.springfactory.exercise.ExerciseCatalog;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ProgramsTest {

  private final Programs programs = new Programs(new ExerciseCatalog());

  @Test
  void aNewProgramHasItsNameAndNoWeeks() {
    Program program = programs.create("Simple & Sinister");

    assertThat(program.id()).isNotNull();
    assertThat(program.name()).isEqualTo("Simple & Sinister");
    assertThat(program.weeks()).isEmpty();
    assertThat(programs.get(program.id())).isEqualTo(program);
  }

  @Test
  void eachProgramGetsItsOwnId() {
    assertThat(programs.create("A").id()).isNotEqualTo(programs.create("B").id());
  }

  @Test
  void aProgramNeedsANonBlankName() {
    assertThatThrownBy(() -> programs.create("  ")).isInstanceOf(BlankProgramNameException.class);
    assertThatThrownBy(() -> programs.create(null))
        .isInstanceOf(BlankProgramNameException.class);
  }

  @Test
  void weeksAreNumberedFromOneInTheOrderTheyAreAdded() {
    UUID id = programs.create("Giant").id();

    assertThat(programs.addWeek(id).number()).isEqualTo(1);
    assertThat(programs.addWeek(id).number()).isEqualTo(2);
    assertThat(programs.addWeek(id).number()).isEqualTo(3);

    assertThat(programs.get(id).weeks()).extracting(Week::number).containsExactly(1, 2, 3);
  }

  @Test
  void weekNumbersArePerProgram() {
    UUID first = programs.create("First").id();
    UUID second = programs.create("Second").id();
    programs.addWeek(first);
    programs.addWeek(first);

    assertThat(programs.addWeek(second).number()).isEqualTo(1);
  }

  @Test
  void aProgramReadEarlierDoesNotChangeWhenAWeekIsAdded() {
    UUID id = programs.create("Giant").id();
    Program before = programs.get(id);

    programs.addWeek(id);

    assertThat(before.weeks()).isEmpty();
  }

  @Test
  void callersCannotChangeTheWeeksOfAProgram() {
    Program program = programs.create("Giant");

    assertThatThrownBy(() -> program.weeks().add(week(1)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void aProgramsWeeksMustBeNumberedFromOneInOrder() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> new Program(id, "Giant", List.of(week(2))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Program(id, "Giant", List.of(week(2), week(1))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(new Program(id, "Giant", List.of(week(1), week(2))).weeks()).hasSize(2);
  }

  @Test
  void noProgramHasABlankName() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> new Program(id, " ", List.of()))
        .isInstanceOf(BlankProgramNameException.class);
    assertThatThrownBy(() -> new Program(id, null, List.of()))
        .isInstanceOf(BlankProgramNameException.class);
  }

  @Test
  void anUnknownProgramIsNotFound() {
    UUID unknown = UUID.randomUUID();

    assertThatThrownBy(() -> programs.get(unknown)).isInstanceOf(ProgramNotFoundException.class);
    assertThatThrownBy(() -> programs.addWeek(unknown))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  void concurrentlyAddedWeeksNeverShareANumber() throws Exception {
    UUID id = programs.create("Busy").id();
    int count = 200;
    List<Callable<Week>> adds =
        IntStream.range(0, count).<Callable<Week>>mapToObj(i -> () -> programs.addWeek(id)).toList();

    ExecutorService pool = Executors.newFixedThreadPool(8);
    try {
      List<Integer> numbers =
          pool.invokeAll(adds).stream().map(ProgramsTest::await).map(Week::number).toList();
      assertThat(numbers).doesNotHaveDuplicates().hasSize(count);
    } finally {
      pool.shutdown();
    }
    assertThat(programs.get(id).weeks())
        .extracting(Week::number)
        .containsExactlyElementsOf(IntStream.rangeClosed(1, count).boxed().toList());
  }

  private static Week week(int number) {
    return new Week(UUID.randomUUID(), number, List.of());
  }

  private static Week await(Future<Week> future) {
    try {
      return future.get();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
