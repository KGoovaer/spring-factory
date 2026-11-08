# Bodyweight Exercises have no Bell

Push-ups, planks and sit-ups are seeded as Exercises with fixed UUIDs (`...1c06` to `...1c08`), like the five kettlebell Exercises, and carry a `bodyweight` flag. A Prescription or Logged Set of a bodyweight Exercise has `bellKg: null`; one of a kettlebell Exercise must have a Bell above 0 kg. `Programs` and `LoggedSessions` check that the Bell fits the Exercise, because only they look the Exercise up. A Set without a Bell adds 0 Volume, so Volume keeps its meaning (Reps × Bell weight) and body weight stays unmodelled.

## Considered options

- **`bellKg: 0` for no Bell**: a smaller change, but 0 kg reads as a very light Bell rather than none, and the "above 0" rule would get an exception.
- **A missing Bell allowed for any Exercise**: simpler, but a Swing without a Bell is a mistake the API should catch.
- **A Bell allowed on a bodyweight Exercise** (a weighted sit-up): left out for now. Allowing it later does not break clients; forbidding it later would.
- **A separate Rep count in the stats for bodyweight work**: not asked for yet. Bodyweight Sessions still count for Adherence.
