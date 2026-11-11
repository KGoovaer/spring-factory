# Lifters add Exercises

The persistence of Exercises, and editing and removing them, are replaced by
docs/adr/0009-exercises-live-in-a-database.md.

A Lifter adds an Exercise with `POST /api/exercises` and in the setup page:
a name, whether it is bodyweight, a hold or timed, and for a Bodyweight
Exercise the gear it needs, such as a jump rope. Added Exercises get random
UUIDs and live in memory, like Programs, so they are lost on restart. The seeded
Exercises keep their fixed UUIDs (docs/adr/0001-exercise-ids-are-uuids.md).
Names are unique, ignoring case, so the setup page never shows two Swings; a
taken name is a 409.

A Session's Equipment is derived from the Exercises: Kettlebell for a Kettlebell
Exercise, the gear of a Bodyweight Exercise that has some, and Body weight
otherwise.

The API now rejects a request body with a field it does not know (400) instead
of dropping it, so a typo such as `"sides"` cannot pass silently.

## Considered options

- **Persistence for added Exercises**: Programs and Logged Sessions are still in
  memory, so persisting only Exercises would leave Prescriptions that refer to
  nothing after a restart. All three move to a database together.
- **Editing or removing an Exercise**: a Prescription or Logged Set may refer to
  it. Left out until someone asks.
- **Equipment typed per Session**: duplicates what the Exercises already say,
  and goes stale when a Prescription is added.
