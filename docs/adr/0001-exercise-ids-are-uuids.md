# Exercise ids are UUIDs

An Exercise is identified by a UUID, not by a readable slug such as `goblet-squat` or a numeric id. Prescriptions and Logged Sessions will store Exercise ids, so the id must outlive renames (a slug would tie the id to the name) and must not depend on a database sequence that does not exist yet. The five seeded Exercises get fixed UUIDs written in `ExerciseCatalog`, so their ids stay the same across restarts and survive a later move of the catalog into a database.

Weeks and Sessions are identified the same way, for the same reasons, but with random UUIDs: a Week is addressed as `/api/weeks/{id}` rather than by its number inside a Program, and a Logged Session will refer to the Session it performed by its id.

## Considered options

- **Slugs** (`swing`, `clean-and-press`): readable in URLs and logs, but renaming an Exercise either breaks references or leaves a stale slug.
- **Numeric ids**: only meaningful once a database assigns them; seeds would need hand-picked numbers that can clash with generated ones.
