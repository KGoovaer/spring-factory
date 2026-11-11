# Kettlebell Planner

Kettlebell Planner lets a lifter visualize their kettlebell training program: what is planned, what was done, and how load and volume develop over time. This glossary covers the language of programs, sessions and sets.

## Language

### Planning

**Program**:
An ordered plan of Weeks that a Lifter follows toward a goal.
_Avoid_: Plan, Routine, Cycle

**Week**:
A numbered block of a Program that holds its planned Sessions.
_Avoid_: Microcycle

**Session**:
One planned training day: an ordered list of Parts, with the Lifter's notes for it (intensity, warm-up, scale-down options) and the Equipment it needs.
_Avoid_: Workout, Day

**Part**:
One section of a Session, numbered from 1 and done in one format: straight Sets, Rounds, an EMOM, an AMRAP, or only rest. Rest between Parts is its own Part.
_Avoid_: Block (a Week is a block of a Program), Section, Circuit

**Round**:
One pass through all the Prescriptions of a Rounds Part or an AMRAP, in order. "3 Rounds" does each Prescription three times.
_Avoid_: Circuit, Lap

**EMOM**:
Every minute on the minute: a Part done for a number of minutes, cycling through a rotation of minutes. At the start of each minute the Lifter does the Prescriptions of that minute of the rotation, then rests for what is left of it. So an EMOM Prescription has no rest of its own. A minute of the rotation without Prescriptions is a rest minute.
_Avoid_: Interval, Clock

**AMRAP**:
As many Rounds as possible: a Part done in Rounds through its Prescriptions until its time cap of minutes runs out. Its Prescriptions have no rest of their own; the Lifter rests at will. Its result is a Score.
_Avoid_: Time cap Part, Metcon

**Rep range**:
Reps a Prescription asks for as a range, written 3-4: at least the low end, at most the high end.
_Avoid_: Target Reps

**Equipment**:
What a Session needs, derived from its Exercises and never typed in: Kettlebell for a Kettlebell Exercise, the gear of a Bodyweight Exercise that names some (a jump rope), and Body weight for one that does not.
_Avoid_: Gear (for the Session's list)

**Exercise**:
A named movement, such as a swing, goblet squat, clean and press or Turkish get-up. Most Exercises are Kettlebell Exercises, done with a Bell; a few, such as the push-up, plank and sit-up, are Bodyweight Exercises, done without one. Some are seeded; a Lifter adds others, and changes or removes any of them. A removed Exercise is no longer offered, but what was planned or logged with it keeps it.
_Avoid_: Movement, Lift

**Kettlebell Exercise**:
An Exercise done with a Bell. Its Prescriptions and Sets always have a Bell weight above 0.
_Avoid_: Weighted Exercise

**Bodyweight Exercise**:
An Exercise done without a Bell. Its Prescriptions and Sets have no Bell weight, and it is the only kind of Exercise that may have none.
_Avoid_: Calisthenics, Unweighted

**Hold Exercise**:
An Exercise measured in Hold time rather than movement, such as the plank. Its Prescriptions and Sets always have a Hold time; no other Exercise has one.
_Avoid_: Static Exercise, Isometric

**Hold time**:
How long each Rep of a Hold Exercise is held, in whole seconds, at least 1.
_Avoid_: Duration

**Timed Exercise**:
An Exercise done for a time rather than counted, such as rope skipping. Its Prescriptions and Sets always have a Work time; no other Exercise has one. No Exercise is both a Hold Exercise and a Timed Exercise.
_Avoid_: Cardio, Interval Exercise

**Work time**:
How long each Rep of a Timed Exercise is worked, in whole seconds, at least 1.
_Avoid_: Duration

**Side**:
Which side a Prescription or Set works: Left, Right, or Each, written 8+8: the Reps on the left, then the same Reps on the right. Without a Side, a Set uses both hands or needs no side.
_Avoid_: Arm, Hand, Per side

**Prescription**:
What the Program asks for one Exercise in a Part of a Session: Sets, Reps or a Rep range, optionally a Side, Hold time (only for a Hold Exercise), Work time (only for a Timed Exercise), Bell weight (none for a Bodyweight Exercise), optionally rest, and in an EMOM the minute it is done in.
_Avoid_: Target, Plan

### Doing

**Logged Session**:
A Session as actually performed on a date, with what the Lifter really did: the Sets done, a Score for each AMRAP, and the Session time if the Lifter timed it. Planned and logged data are never mixed.
_Avoid_: Completed workout, History entry

**Session time**:
How long the Lifter trained for a Logged Session, in whole seconds, as the Session timer counted it: from Start session, leaving out any time it was stopped. A Logged Session the Lifter did not time has none.
_Avoid_: Duration, Total time, Workout time

**Score**:
What a Lifter did in an AMRAP: whole Rounds plus extra Reps into the next, written 5 + 7. The extra Reps are fewer than a Round has.
_Avoid_: Result, AMRAP score (in code)

**Set**:
A group of consecutive Reps of one Exercise, followed by rest.
_Avoid_: Round (a Round is a pass through a Part)

**Rep**:
One repetition of an Exercise; for a Hold Exercise, one hold; for a Timed Exercise, one stretch of work.
_Avoid_: Count

**Bell**:
The kettlebell used for a Set, identified by its weight in kilograms.
_Avoid_: Weight, Kettlebell (in code)

### Measuring

**Volume**:
Reps times Bell weight, summed over a scope (a Set, Part, Session, Week or Program). A Set on Each Side counts its Reps twice; a Prescription in a Rounds Part counts once per Round, in an EMOM once per minute it is done, and in a planned AMRAP once. A Rep range counts its low end when planned. A Score counts its Rounds and then its extra Reps, each at the Bell of the Prescription it falls in. A Set without a Bell adds 0.
_Avoid_: Tonnage, Load

**Adherence**:
The share of planned Sessions that have a Logged Session.
_Avoid_: Compliance, Completion rate

### People

**Lifter**:
The person who follows a Program and logs Sessions.
_Avoid_: User, Athlete, Trainee

## Relationships

- A **Program** has many **Weeks**; a **Week** has many **Sessions**.
- A **Session** has many **Parts**; a **Part** has many **Prescriptions**, each for one **Exercise**.
- A **Logged Session** refers to the **Session** it performed and holds the **Sets** actually done, a **Score** per **AMRAP** done and, if timed, its **Session time**.
- **Volume**, **Adherence** and **Equipment** are always derived, never stored.

## Flagged ambiguities

- "Weight" means the **Bell** weight; body weight is not modelled, so a **Bodyweight Exercise** has no weight and adds no **Volume**.
- A plank is a hold, not a repetition: it is measured in **Hold time**, and one **Rep** of a plank is one hold. Hold time adds no **Volume**.
- A walking plank moves, so it is counted in **Reps**, not **Hold time**; skipping is counted in **Work time**, not Reps.
- "8+8" is one **Set** on **Each** side: 8 Reps on the left and 8 on the right, 16 **Reps** of **Volume**, not two Sets.
- "3-4x" is a **Rep range**, not 3 or 4 **Sets**.
- A partly done **EMOM** is logged as the **Sets** of the minutes done; it has no **Score**.
- "Right: 5x Swing" in an **EMOM** is a Prescription on the **Right** side; the next minute's "Left" is another Prescription.
