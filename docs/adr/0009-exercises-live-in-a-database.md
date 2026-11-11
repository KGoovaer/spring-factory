# Exercises live in a database

Issue #22 moves the Exercise catalog out of memory. This replaces the
persistence part of docs/adr/0007-lifters-add-exercises.md, which wanted
Exercises, Programs and Logged Sessions to move to a database together: the
Exercises move first, on their own.

- **Engine.** An embedded H2 database in a file, `./data/kettlebell` by default
  (`KETTLEBELL_DB_URL` points it elsewhere). The app still starts with
  `mvn spring-boot:run` and nothing else to install. Tests run against H2 too,
  in memory, through the same migrations.
- **Migrations.** Flyway, in `src/main/resources/db/migration`, run on startup.
  `V1` creates the `exercise` table; `V2` seeds the twelve Exercises with the
  fixed ids of docs/adr/0001-exercise-ids-are-uuids.md. Every later change to
  the schema or the seeds is a new migration; shipped ones are never edited.
- **Access.** Spring Data JPA: an `ExerciseRow` entity and its `ExerciseRows`
  repository, both package-private behind `ExerciseCatalog`, whose `all`, `get`
  and `add` keep their signatures. Hibernate only validates the entity against
  the schema; the migrations own it. A row is written only from an `Exercise`
  and read back only through one, so the Kettlebell, Bodyweight, Hold and Timed
  rules, and the 200 characters a name or gear may have, live in the record
  alone; the table does not repeat them as checks.
- **One app per database file.** The catalog takes one change at a time, and
  nothing else opens the file, so that is all the locking it needs.
- **Seeds are plain inserts.** `V1` and `V2` run together on an empty table,
  so no Lifter's Exercise can share a seed's name. A later seed migration that
  meets a name a Lifter took decides then what to do with it; none is planned.
- **A Lifter renames or removes an Exercise.** `PUT /api/exercises/{id}` takes
  only a name and keeps the id, classification and place in the catalog; a
  taken name is a 409, and a body with a classification is a 400. The
  classification never changes, because Prescriptions and Logged Sets look
  their Exercise up: a changed one would change the Equipment of Sessions
  already planned and turn their Bell weights, Hold times and Work times
  invalid. `DELETE /api/exercises/{id}` removes it from the list and frees its
  name, but keeps the row: `get` still finds it, so the Prescriptions and
  Logged Sets that refer to it keep their Equipment and Volume.

## Left out

- **Programs and Logged Sessions in the database.** They stay in memory and are
  still lost on restart. When they move, their references to Exercises become
  foreign keys.
- **Changing or removing Exercises in the setup page.** The API comes first.
- **Refusing a new Prescription for a removed Exercise.** The setup page only
  offers listed Exercises; the API still accepts a removed one's id.

## Considered options

- **Postgres with Testcontainers**: the engine a deployed app would want, but
  running the app would need a database server, and every test that builds a
  catalog a container. H2 runs Postgres-style SQL in-process; moving is a
  datasource and driver change plus a check of the migrations.
- **Liquibase**: changelogs in XML or YAML for what plain SQL says as well.
- **`JdbcClient` and hand-written SQL**: less machinery for one table. The
  first version of #22 used it; review asked for JPA instead, which Programs
  and Logged Sessions can share when they move.
- **Seeding at startup in Java**: keeps the seeds next to the code, but runs on
  every start and has to decide each time whether a Lifter renamed a seed.
- **Deleting a removed Exercise's row**: Prescriptions and Logged Sets that
  refer to it would fail to find it, and the Program pages with them.
