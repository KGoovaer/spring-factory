package be.ae.springfactory.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The catalog keeps what it is told across instances, because it lives in the database. */
class ExercisePersistenceTest {

  private static final UUID SWING = UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01");

  private final DataSource database = ExerciseCatalogs.freshDatabase();

  @Test
  void addedChangedAndRemovedExercisesOutliveTheCatalogThatMadeThem() {
    migrate(null);
    ExerciseCatalog before = new ExerciseCatalog(JdbcClient.create(database));
    Exercise boxJump = before.add("Box jump", true, false, false, "Box");
    before.change(SWING, "Two-hand swing", false, false, false, null);
    before.remove(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c05"));

    ExerciseCatalog after = new ExerciseCatalog(JdbcClient.create(database));

    assertThat(after.all()).isEqualTo(before.all()).hasSize(12).last().isEqualTo(boxJump);
    assertThat(after.get(SWING).name()).isEqualTo("Two-hand swing");
    assertThat(after.get(UUID.fromString("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c05")).name())
        .isEqualTo("Snatch");
  }

  @Test
  void theDatabaseItselfRefusesAHoldThatIsTimedAndGearWithABell() {
    migrate(null);
    JdbcClient jdbc = JdbcClient.create(database);
    String insert =
        "INSERT INTO exercise (id, name, name_key, bodyweight, hold, timed, gear, position)"
            + " VALUES (RANDOM_UUID(), ?, ?, ?, ?, ?, ?, 99)";

    assertThatThrownBy(
            () -> jdbc.sql(insert).params("Hover", "hover", true, true, true, null).update())
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> jdbc.sql(insert).params("Sled", "sled", false, false, false, "Sled").update())
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private void migrate(String target) {
    var flyway = Flyway.configure().dataSource(database);
    if (target != null) {
      flyway.target(target);
    }
    flyway.load().migrate();
  }
}
