package be.ae.springfactory.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExerciseCatalogTest {

  private final ExerciseCatalog catalog = ExerciseCatalogs.seeded();

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
            "Clean to waiter press",
            "Split stance row",
            "Push-up",
            "Plank",
            "Sit-up",
            "Walking plank",
            "Skipping");
  }

  @Test
  void onlyPushUpsPlanksSitUpsWalkingPlanksAndSkippingAreBodyweight() {
    assertThat(catalog.all())
        .filteredOn(Exercise::bodyweight)
        .extracting(Exercise::name)
        .containsExactly("Push-up", "Plank", "Sit-up", "Walking plank", "Skipping");
    assertThat(catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06")).name())
        .isEqualTo("Push-up");
  }

  @Test
  void aKettlebellExerciseNeedsABellAndABodyweightExerciseTakesNone() {
    Exercise swing = catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01"));
    Exercise pushUp = catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06"));

    assertThat(swing.mismatch(BigDecimal.TEN, null, null)).isEmpty();
    assertThat(swing.mismatch(null, null, null)).contains("Swing needs a Bell weight");
    assertThat(pushUp.mismatch(null, null, null)).isEmpty();
    assertThat(pushUp.mismatch(BigDecimal.TEN, null, null))
        .contains("Push-up is a bodyweight Exercise and takes no Bell, was 10 kg");
  }

  @Test
  void onlyThePlankIsAHoldAndItNeedsAHoldTime() {
    Exercise plank = catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c07"));
    Exercise pushUp = catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06"));

    assertThat(catalog.all()).filteredOn(Exercise::hold).containsExactly(plank);
    assertThat(plank.bodyweight()).isTrue();
    assertThat(plank.mismatch(null, 60, null)).isEmpty();
    assertThat(plank.mismatch(null, null, null)).contains("Plank is a hold and needs a hold time");
    assertThat(pushUp.mismatch(null, 60, null))
        .contains("Push-up is not a hold and takes no hold time, was 60 s");
  }

  @Test
  void everyExerciseHasAUniqueId() {
    assertThat(catalog.all()).extracting(Exercise::id).doesNotContainNull().doesNotHaveDuplicates();
  }

  @Test
  void idsAreStableAcrossInstances() {
    assertThat(ExerciseCatalogs.seeded().all()).isEqualTo(catalog.all());
    assertThat(catalog.all().get(0).id())
        .isEqualTo(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01"));
  }

  @Test
  void catalogCannotBeChangedByCallers() {
    Exercise press = new Exercise(UUID.randomUUID(), "Press", false, false);
    assertThatThrownBy(() -> catalog.all().add(press))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(catalog.all()).hasSize(12);
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

  @Test
  void onlySkippingIsTimedItNeedsAWorkTimeAndIsDoneWithAJumpRope() {
    Exercise skipping = catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0c"));
    Exercise pushUp = catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06"));

    assertThat(catalog.all()).filteredOn(Exercise::timed).containsExactly(skipping);
    assertThat(skipping.mismatch(null, null, 60)).isEmpty();
    assertThat(skipping.mismatch(null, null, null))
        .contains("Skipping is timed and needs a work time");
    assertThat(pushUp.mismatch(null, null, 60))
        .contains("Push-up is not timed and takes no work time, was 60 s");
    assertThat(skipping.mismatch(null, 60, 60))
        .contains("Skipping is not a hold and takes no hold time, was 60 s");
  }

  @Test
  void anExerciseIsDoneWithAKettlebellItsGearOrBodyWeight() {
    assertThat(catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01")).equipment())
        .isEqualTo("Kettlebell");
    assertThat(catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0c")).equipment())
        .isEqualTo("Jump rope");
    assertThat(catalog.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06")).equipment())
        .isEqualTo("Body weight");
  }

  @Test
  void aLifterAddsAnExerciseAtTheEndOfTheCatalog() {
    Exercise added = catalog.add("  Box jump ", true, false, false, " Box ");

    assertThat(added.name()).isEqualTo("Box jump");
    assertThat(added.equipment()).isEqualTo("Box");
    assertThat(catalog.all()).hasSize(13).last().isEqualTo(added);
    assertThat(catalog.get(added.id())).isEqualTo(added);
  }

  @Test
  void anExerciseNameIsTakenIgnoringCase() {
    assertThatThrownBy(() -> catalog.add("swing", false, false, false, null))
        .isInstanceOf(ExerciseNameTakenException.class)
        .hasMessage("There is an Exercise named swing already");
    assertThat(catalog.all()).hasSize(12);
  }

  @Test
  void anAddedExerciseNeedsANameIsNotBothAHoldAndTimedAndHasGearOnlyWithoutABell() {
    assertThatThrownBy(() -> catalog.add(" ", false, false, false, null))
        .isInstanceOf(InvalidExerciseException.class)
        .hasMessage("An Exercise needs a name");
    assertThatThrownBy(() -> catalog.add("Wall sit", true, true, true, null))
        .isInstanceOf(InvalidExerciseException.class)
        .hasMessage("Wall sit cannot be both a hold and timed");
    assertThatThrownBy(() -> catalog.add("Sled push", false, false, false, "Sled"))
        .isInstanceOf(InvalidExerciseException.class)
        .hasMessage("Sled push is done with a Bell and names no other gear, was Sled");
    assertThat(catalog.all()).hasSize(12);
  }

  @Test
  void aNameOrGearLongerThanTheCatalogKeepsIsInvalid() {
    String tooLong = "x".repeat(201);
    UUID swing = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01");

    assertThatThrownBy(() -> catalog.add(tooLong, false, false, false, null))
        .isInstanceOf(InvalidExerciseException.class)
        .hasMessage("An Exercise name has at most 200 characters");
    assertThatThrownBy(() -> catalog.add("Drag", true, false, false, tooLong))
        .isInstanceOf(InvalidExerciseException.class)
        .hasMessage("Drag names gear of at most 200 characters");
    assertThatThrownBy(() -> catalog.rename(swing, tooLong))
        .isInstanceOf(InvalidExerciseException.class);
    assertThat(catalog.add("x".repeat(200), true, false, false, "y".repeat(200)).name())
        .hasSize(200);
  }

  @Test
  void aLifterRenamesAnExerciseKeepingItsIdClassificationAndPlace() {
    UUID plank = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c07");

    Exercise renamed = catalog.rename(plank, " Front plank ");

    assertThat(renamed).isEqualTo(new Exercise(plank, "Front plank", true, true));
    assertThat(catalog.get(plank)).isEqualTo(renamed);
    assertThat(catalog.all()).hasSize(12).element(8).isEqualTo(renamed);
  }

  @Test
  void aRenameKeepsNamesUniqueIgnoringCase() {
    UUID swing = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01");

    assertThatThrownBy(() -> catalog.rename(swing, "SNATCH"))
        .isInstanceOf(ExerciseNameTakenException.class);
    assertThatThrownBy(() -> catalog.rename(swing, " "))
        .isInstanceOf(InvalidExerciseException.class);
    assertThat(catalog.rename(swing, "swing").name()).isEqualTo("swing");
    assertThatThrownBy(() -> catalog.rename(UUID.randomUUID(), "Lunge"))
        .isInstanceOf(ExerciseNotFoundException.class);
  }

  @Test
  void aRemovedExerciseIsGoneAndFreesItsName() {
    UUID snatch = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c05");

    catalog.remove(snatch);

    assertThat(catalog.all()).hasSize(11).extracting(Exercise::name).doesNotContain("Snatch");
    assertThatThrownBy(() -> catalog.get(snatch)).isInstanceOf(ExerciseNotFoundException.class);
    Exercise again = catalog.add("Snatch", false, false, false, null);
    assertThat(again.id()).isNotEqualTo(snatch);
    assertThat(catalog.all()).last().isEqualTo(again);
  }

  @Test
  void aRemovedExerciseCannotBeRenamedOrRemovedAgain() {
    UUID snatch = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c05");
    catalog.remove(snatch);

    assertThatThrownBy(() -> catalog.remove(snatch)).isInstanceOf(ExerciseNotFoundException.class);
    assertThatThrownBy(() -> catalog.rename(snatch, "Snatch"))
        .isInstanceOf(ExerciseNotFoundException.class);
    assertThatThrownBy(() -> catalog.remove(UUID.randomUUID()))
        .isInstanceOf(ExerciseNotFoundException.class);
  }
}
