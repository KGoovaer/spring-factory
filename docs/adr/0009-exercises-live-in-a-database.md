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
- **Access.** Spring's `JdbcClient` behind `ExerciseCatalog`, whose `all`,
  `get` and `add` keep their signatures. Every row is rebuilt through the
  `Exercise` record, so the Kettlebell, Bodyweight, Hold and Timed rules hold
  for what is read back; the table repeats the two rules a row can break on its
  own (not both a hold and timed, gear only without a Bell) as checks.
- **A seed overrides a Lifter's Exercise of the same name.** A seed migration
  merges on the lower-cased name: a listed Exercise a Lifter added as "swing"
  becomes the seeded Swing, with its id and classification. That is safe while
  Programs and Logged Sessions live in memory, because nothing refers to the old
  id when migrations run at startup.
- **A Lifter changes or removes an Exercise.** `PUT /api/exercises/{id}` takes
  the same body as `POST` and keeps the id and place in the catalog; a taken
  name is a 409. `DELETE /api/exercises/{id}` removes it from the list and frees
  its name, but keeps the row: `get` still finds it, so the Prescriptions and
  Logged Sets that refer to it keep their Equipment and Volume. A change applies
  to what is planned or logged after it; what was planned or logged before keeps
  its Bell weight, Hold time and Work time.

## Left out

- **Programs and Logged Sessions in the database.** They stay in memory and are
  still lost on restart. When they move, their references to Exercises become
  foreign keys, and a seed can no longer take over a Lifter's id silently.
- **Changing or removing Exercises in the setup page.** The API comes first.
- **Refusing a new Prescription for a removed Exercise.** The setup page only
  offers listed Exercises; the API still accepts a removed one's id.

## Considered options

- **Postgres with Testcontainers**: the engine a deployed app would want, but
  running the app would need a database server, and every test that builds a
  catalog a container. H2 runs Postgres-style SQL in-process; moving is a
  datasource and driver change plus a check of the migrations.
- **Liquibase**: changelogs in XML or YAML for what plain SQL says as well.
- **JPA**: an entity, a repository and a second copy of the Exercise rules in
  annotations, for one table read through three queries.
- **Seeding at startup in Java**: keeps the seeds next to the code, but runs on
  every start and has to decide each time whether a Lifter renamed a seed.
- **Deleting a removed Exercise's row**: Prescriptions and Logged Sets that
  refer to it would fail to find it, and the Program pages with them.
