package be.ae.springfactory.exercise;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ConfigurableApplicationContext;

/** The catalog keeps what it is told across restarts, because it lives in the database. */
class ExercisePersistenceTest {

  private static final UUID SWING = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01");
  private static final UUID SNATCH = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c05");

  @TempDir Path directory;

  @Test
  void addedChangedAndRemovedExercisesOutliveARestart() {
    String url = "jdbc:h2:file:" + directory.resolve("kettlebell");
    Exercise boxJump;
    List<Exercise> before;
    try (ConfigurableApplicationContext app = ExerciseCatalogs.start(url)) {
      ExerciseCatalog catalog = app.getBean(ExerciseCatalog.class);
      boxJump = catalog.add("Box jump", true, false, false, "Box");
      catalog.change(SWING, "Two-hand swing", false, false, false, null);
      catalog.remove(SNATCH);
      before = catalog.all();
    }

    try (ConfigurableApplicationContext app = ExerciseCatalogs.start(url)) {
      ExerciseCatalog catalog = app.getBean(ExerciseCatalog.class);

      assertThat(catalog.all()).isEqualTo(before).hasSize(12).last().isEqualTo(boxJump);
      assertThat(catalog.get(SWING).name()).isEqualTo("Two-hand swing");
      assertThat(catalog.get(SNATCH).name()).isEqualTo("Snatch");
    }
  }
}
