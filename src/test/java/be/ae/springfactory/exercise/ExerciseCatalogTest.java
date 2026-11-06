package be.ae.springfactory.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExerciseCatalogTest {

  private final ExerciseCatalog catalog = new ExerciseCatalog();

  @Test
  void seedsTheFiveExercisesInOrder() {
    assertThat(catalog.all())
        .extracting(Exercise::name)
        .containsExactly("Swing", "Goblet squat", "Clean and press", "Turkish get-up", "Snatch");
  }

  @Test
  void everyExerciseHasAUniqueId() {
    assertThat(catalog.all()).extracting(Exercise::id).doesNotContainNull().doesNotHaveDuplicates();
  }

  @Test
  void idsAreStableAcrossInstances() {
    assertThat(new ExerciseCatalog().all()).isEqualTo(catalog.all());
    assertThat(catalog.all().get(0).id())
        .isEqualTo(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01"));
  }

  @Test
  void catalogCannotBeChangedByCallers() {
    assertThatThrownBy(() -> catalog.all().add(new Exercise(UUID.randomUUID(), "Press")))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(catalog.all()).hasSize(5);
  }
}
