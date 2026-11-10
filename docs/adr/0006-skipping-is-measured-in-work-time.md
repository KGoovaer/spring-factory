# Skipping is measured in work time

Rope skipping is done for a number of seconds, so it is seeded as a Timed
Exercise: an Exercise with a `timed` flag whose Prescriptions and Logged Sets
carry `workSeconds`, the Work time of each Rep. It follows the Plank of
docs/adr/0004-a-plank-is-measured-in-hold-time.md: a Timed Exercise must have a
Work time and no other Exercise may have one, `Exercise.mismatch` checks it with
the Bell and the Hold time, Reps stay required (45 s of skipping is 1 Rep of 45
s), and Work time adds no Volume. No Exercise is both a hold and timed.

A walking plank moves, so it is a Bodyweight Exercise counted in Reps.

## Considered options

- **Hold time for skipping**: no new field, but skipping is not a hold, and the
  glossary would have to bend.
- **One "time" field for holds and timed work**: fewer fields, but the API would
  rename `holdSeconds`, and every client would break for no new behaviour.
