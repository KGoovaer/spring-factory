package be.ae.springfactory.exercise;

import org.flywaydb.core.Flyway;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Exercise catalogs for tests that need one without the app's own Spring context: the catalog, its
 * repository and its database on their own, migrated and seeded by the app's own Flyway
 * migrations.
 */
public final class ExerciseCatalogs {

  private static ConfigurableApplicationContext shared;

  private ExerciseCatalogs() {}

  /**
   * A catalog that holds the seeded Exercises and nothing else. Every call empties and reseeds the
   * one in-memory database all of them share, so a test takes one catalog and keeps it.
   */
  public static synchronized ExerciseCatalog seeded() {
    if (shared == null) {
      shared = start("jdbc:h2:mem:exercise-catalogs;DB_CLOSE_DELAY=-1");
    }
    Flyway flyway = shared.getBean(Flyway.class);
    flyway.clean();
    flyway.migrate();
    return shared.getBean(ExerciseCatalog.class);
  }

  /** Starts the catalog on the database at this JDBC URL, migrating it first. */
  static ConfigurableApplicationContext start(String url) {
    return new SpringApplicationBuilder(CatalogOnly.class)
        .web(WebApplicationType.NONE)
        .bannerMode(Banner.Mode.OFF)
        .logStartupInfo(false)
        .run("--spring.datasource.url=" + url, "--spring.flyway.clean-disabled=false");
  }

  /** The catalog with what Spring Boot configures for it: the datasource, Flyway and JPA. */
  @TestConfiguration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  @Import(ExerciseCatalog.class)
  static class CatalogOnly {}
}
