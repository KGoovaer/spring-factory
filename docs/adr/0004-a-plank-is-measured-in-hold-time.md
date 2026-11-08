# A plank is measured in hold time

A plank is held, not repeated, so the Plank is seeded as a Hold Exercise: an Exercise with a `hold` flag whose Prescriptions and Logged Sets carry `holdSeconds`, the Hold time of each Rep. A Hold Exercise must have a Hold time and no other Exercise may have one; `Programs` and `LoggedSessions` check this with the Bell, through `Exercise.mismatch`. Reps stay required, so three planks of 60 seconds are 3 Sets of 1 Rep held 60 seconds, and 3 × 3 holds of 20 seconds can be written too. Hold time adds no Volume, which stays Reps × Bell weight.

## Considered options

- **Reps count seconds for a plank**: no new field, but the same number would mean Reps for one Exercise and seconds for another, and Volume would multiply a Bell by seconds if a weighted hold is ever allowed.
- **Hold time instead of Reps** (Reps left out for a Hold Exercise): closer to how a plank is usually written, but Reps would become optional everywhere and every Volume computation would need a case for it.
