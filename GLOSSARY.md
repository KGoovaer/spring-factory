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
One planned training day: an ordered list of Exercises with prescribed Sets.
_Avoid_: Workout, Day

**Exercise**:
A named movement, such as a swing, goblet squat, clean and press or Turkish get-up. Most Exercises are Kettlebell Exercises, done with a Bell; a few, such as the push-up, plank and sit-up, are Bodyweight Exercises, done without one.
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

**Prescription**:
What the Program asks for one Exercise in a Session: Sets, Reps, Hold time (only for a Hold Exercise), Bell weight (none for a Bodyweight Exercise) and optionally rest.
_Avoid_: Target, Plan

### Doing

**Logged Session**:
A Session as actually performed on a date, with what the Lifter really did. Planned and logged data are never mixed.
_Avoid_: Completed workout, History entry

**Set**:
A group of consecutive Reps of one Exercise, followed by rest.
_Avoid_: Round

**Rep**:
One repetition of an Exercise; for a Hold Exercise, one hold.
_Avoid_: Count

**Bell**:
The kettlebell used for a Set, identified by its weight in kilograms.
_Avoid_: Weight, Kettlebell (in code)

### Measuring

**Volume**:
Reps times Bell weight, summed over a scope (a Set, Session, Week or Program). A Set without a Bell adds 0.
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
- A **Session** has many **Prescriptions**, each for one **Exercise**.
- A **Logged Session** refers to the **Session** it performed and holds the **Sets** actually done.
- **Volume** and **Adherence** are always derived, never stored.

## Flagged ambiguities

- "Weight" means the **Bell** weight; body weight is not modelled, so a **Bodyweight Exercise** has no weight and adds no **Volume**.
- A plank is a hold, not a repetition: it is measured in **Hold time**, and one **Rep** of a plank is one hold. Hold time adds no **Volume**.
