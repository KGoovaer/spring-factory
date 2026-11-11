# A Session is made of Parts

A Session is an ordered list of Parts, and each Part has one format: straight
Sets, Rounds, an EMOM or rest. A workout such as "20 min EMOM, 60 s rest, then 3
Rounds" is three Parts. Rest between Parts is a Part of its own, not a field on
the Part before or after it, because the Lifter writes it as its own line. A
Part is addressed by its number in the Session, from 1:
`POST /api/sessions/{id}/parts/{number}/prescriptions`.

- **Rounds.** A Rounds Part does every Prescription for its Sets, in order, once
  per Round. "Round" was on the _avoid_ list of Set; it now means a pass through
  a Part, so a Set stays a group of Reps of one Exercise.
- **EMOM.** An EMOM has its minutes (20) and a rotation (4): each Prescription
  names the minute of the rotation it is done in, and a minute without any is a
  rest minute. The Lifter rests for what is left of each minute, so an EMOM
  Prescription needs no rest. When the minutes do not divide by the rotation,
  the first minutes of the rotation are done once more.
- **Side.** "8+8" is one Set on Each Side, so it adds 16 Reps of Volume; "Right:
  5x Swing" is a Prescription on the Right Side. Side lives on the Prescription
  and the Logged Set, not on the Exercise: the same swing is done with both
  hands, one hand, or each hand in turn.
- **Notes and Equipment.** A Session has free-text notes for intensity, warm-up
  and scale-down options. Its Equipment is derived from its Exercises, never
  typed in.

A Logged Session stays a flat list of Sets: what the Lifter did, in order.
(Amended by docs/adr/0008-an-amrap-is-logged-as-a-score.md: an AMRAP Part, added
later, is logged as a Score next to the Sets.) The
log page lays out the planned Parts, Rounds and minutes as rows to tick off, but
it sends only the Sets.

`POST /api/sessions/{id}/prescriptions` still works: it adds to the last Part
when that is one of straight Sets, else to a new one, so a Session set up before
Parts existed is one Part of straight Sets.

## Considered options

- **A "Block" for a Part**: clashes with Week, "a numbered block of a Program".
- **Rounds as Sets** (3 Rounds = 3 Sets of each Prescription): loses the order
  in which the Lifter goes through them.
- **One EMOM minute per item with its own list of Prescriptions**: the same
  data, but setting it up needs one call per minute before any Prescription.
- **Logging the Part, Round and minute of every Set**: not asked for. Volume and
  Adherence do not need it, and the order of the Sets already shows it.
