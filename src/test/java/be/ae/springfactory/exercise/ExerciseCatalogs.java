package be.ae.springfactory.exercise;

import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Exercise catalogs for tests that need one without the Spring context: each is backed by its own
 * in-memory H2 database, migrated and seeded by the app's own Flyway migrations.
 */
public final class ExerciseCatalogs {

  private ExerciseCatalogs() {}

  /** A catalog that holds the seeded Exercises and nothing else. */
  public static ExerciseCatalog seeded() {
    DataSource database = freshDatabase();
    Flyway.configure().dataSource(database).load().migrate();
    return new ExerciseCatalog(JdbcClient.create(database));
  }

  /** An empty, unmigrated in-memory database of its own. */
  static DataSource freshDatabase() {
    return new DriverManagerDataSource(
        "jdbc:h2:mem:exercises-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
  }
}
