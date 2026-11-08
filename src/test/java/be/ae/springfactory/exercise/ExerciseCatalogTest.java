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

    assertThat(swing.mismatch(BigDecimal.TEN, null)).isEmpty();
    assertThat(swing.mismatch(null, null)).contains("Swing needs a Bell weight");
    assertThat(pushUp.mismatch(null, null)).isEmpty();
    assertThat(pushUp.mismatch(BigDecimal.TEN, null))
        .contains("Push-up is a bodyweight Exercise and takes no Bell, was 10 kg");
  }

  @Test
  void onlyThePlankIsAHoldAndItNeedsAHoldTime() {
    Exercise plank = catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c07"));
    Exercise pushUp = catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06"));

    assertThat(catalog.all()).filteredOn(Exercise::hold).containsExactly(plank);
    assertThat(plank.bodyweight()).isTrue();
    assertThat(plank.mismatch(null, 60)).isEmpty();
    assertThat(plank.mismatch(null, null)).contains("Plank is a hold and needs a hold time");
    assertThat(pushUp.mismatch(null, 60))
        .contains("Push-up is not a hold and takes no hold time, was 60 s");
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
    Exercise press = new Exercise(UUID.randomUUID(), "Press", false, false);
    assertThatThrownBy(() -> catalog.all().add(press))
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
