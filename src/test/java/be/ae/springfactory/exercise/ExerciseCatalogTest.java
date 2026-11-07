package be.ae.springfactory.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExerciseCatalogTest {

  private final ExerciseCatalog catalog = new ExerciseCatalog();

  @Test
  void seedsTheKettlebellExercisesThenTheBodyweightOnesInOrder() {
    assertThat(catalog.all())
        .extracting(Exercise::name)
        .containsExactly(
            "Swing",
            "Goblet squat",
            "Clean and press",
            "Turkish get-up",
            "Snatch",
            "Push-up",
            "Plank",
            "Sit-up");
  }

  @Test
  void onlyPushUpsPlanksAndSitUpsAreBodyweight() {
    assertThat(catalog.all())
        .filteredOn(Exercise::bodyweight)
        .extracting(Exercise::name)
        .containsExactly("Push-up", "Plank", "Sit-up");
    assertThat(catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06")).name())
        .isEqualTo("Push-up");
  }

  @Test
  void aKettlebellExerciseNeedsABellAndABodyweightExerciseTakesNone() {
    Exercise swing = catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01"));
    Exercise pushUp = catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06"));

    assertThat(swing.fits(BigDecimal.TEN)).isTrue();
    assertThat(swing.fits(null)).isFalse();
    assertThat(pushUp.fits(null)).isTrue();
    assertThat(pushUp.fits(BigDecimal.TEN)).isFalse();
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
    assertThatThrownBy(() -> catalog.all().add(new Exercise(UUID.randomUUID(), "Press", false)))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(catalog.all()).hasSize(8);
  }

  @Test
  void findsAnExerciseById() {
    UUID swing = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01");

    assertThat(catalog.get(swing).name()).isEqualTo("Swing");
  }

  @Test
  void anUnknownExerciseIsNotFound() {
    assertThatThrownBy(() -> catalog.get(UUID.randomUUID()))
        .isInstanceOf(ExerciseNotFoundException.class);
  }
}
