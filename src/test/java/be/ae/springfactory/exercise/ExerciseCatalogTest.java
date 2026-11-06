package be.ae.springfactory.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ExerciseCatalogTest {

  private final ExerciseCatalog catalog = new ExerciseCatalog();

  @Test
  void listsTheSeededExercisesInOrder() {
    assertThat(catalog.all())
        .containsExactly(
            new Exercise("swing", "Swing"),
            new Exercise("goblet-squat", "Goblet squat"),
            new Exercise("clean-and-press", "Clean and press"),
            new Exercise("turkish-get-up", "Turkish get-up"),
            new Exercise("snatch", "Snatch"));
  }

  @Test
  void exerciseIdsAreUnique() {
    List<String> ids = catalog.all().stream().map(Exercise::id).toList();
    assertThat(ids).doesNotHaveDuplicates();
  }

  @Test
  void callersCannotChangeTheCatalog() {
    List<Exercise> before = List.copyOf(catalog.all());

    assertThatThrownBy(() -> catalog.all().add(new Exercise("press", "Press")))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(catalog.all()).isEqualTo(before);
  }
}
