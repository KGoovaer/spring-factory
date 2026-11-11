# An AMRAP is logged as a Score

Issue #21 asked for timed Part formats, rep ranges and a way to log an AMRAP
score and a partly done EMOM. This amends the logging rule of
docs/adr/0005-a-session-is-made-of-parts.md and keeps the rest of it.

- **AMRAP.** A new Part format: as many Rounds as possible through its
  Prescriptions within a time cap of `minutes` (1 to 120). Its Prescriptions
  have no rest and no minute. Its planned Volume counts one Round: the plan
  cannot know how many the Lifter will do.
- **Score.** A Logged Session keeps its flat list of Sets and gains a Score per
  AMRAP Part done: `{part, rounds, extraReps}`, "5 Rounds + 7 Reps". The extra
  Reps run through the Prescriptions of a Round in order and must stay short of
  a whole Round. A Logged Session needs at least one Set or Score, so a Session
  that is only an AMRAP can be logged.
- **Volume of a Score** is derived from the plan, never stored: the Rounds times
  the Volume of a Round, plus each extra Rep at the Bell of the Prescription it
  falls in. A Rep on Each Side counts as the Lifter counts it, 3+3 is 6 Reps.
- **A partly done EMOM** needs nothing new: the Lifter ticks off the minutes done
  and only those Sets are sent, so no progress is lost.
- **Rep range.** A Prescription may have `repsMax` above `reps`: "3-4" is
  `reps: 3, repsMax: 4`. Planned Volume counts the low end, what the Lifter
  commits to; logged Volume counts the Reps logged.
- **Clocks.** The log page runs a clock for an EMOM (the minute it is in and
  what is left of it) and counts an AMRAP down from its time cap.

## Settled questions

- **8+8 stays one Set on Each Side.** It already means 8 left and 8 right, and
  its Volume counts 16 Reps; two Sets would double the rows the Lifter ticks.
- **Round, not Circuit.** A Rounds Part is the circuit; the glossary keeps
  Circuit on its avoid list.

## Left out

- **Intervals as a Part format** (work seconds, rest seconds). A Rounds Part of
  Timed Exercises with rest already gives 40 s on, 20 s off, with the work and
  rest timers the log page has. "Interval" stays on the avoid list.
- **EMOM intervals other than 60 s** (E2MOM, E90S). No Program asks for them
  yet.
- **Replacement Parts.** It is unclear whether choosing one changes planned
  Volume or Adherence; the Session's notes hold scale-down options until a
  Program needs more.

## Considered options

- **Logging an AMRAP as Sets**: no new shape, but the Lifter would have to add a
  row per Exercise per Round while out of breath, and the score itself would be
  lost.
- **Storing the Volume of a Score**: survives a later change to the plan, but
  breaks the rule that Volume is always derived.
