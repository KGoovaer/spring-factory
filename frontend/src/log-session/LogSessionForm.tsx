import { useState } from "react";
import { Link } from "react-router-dom";
import { errorMessage } from "../api/client";
import { sideLabel } from "../api/programs";
import { HoldTimer } from "./HoldTimer";
import { PartClockView } from "./PartClockView";
import { RestTimer } from "./RestTimer";
import {
  bellWeightProblem,
  correctLoggedSession,
  doneSets,
  filledScores,
  logSession,
  SessionLoggedElsewhere,
  type LoggedElsewhere,
  type ScoreEntry,
  type SessionToLog,
  type SetEntry,
} from "./logSession";

interface SetRow extends SetEntry {
  key: number;
  repsRange: string | null;
  restSeconds: number | null;
  place: string | null;
}

/** A row of a Part on the form: the Set with this key, or rest. */
type LayoutRow =
  | { kind: "set"; key: number }
  | { kind: "rest"; key: string; seconds: number; place: string | null };

type Saving =
  | { status: "idle" }
  | { status: "saving" }
  | { status: "saved" }
  | { status: "failed"; message: string };

const COLUMNS = 8;

/**
 * The Parts of one Session, each a list of Set rows the Lifter marks done while
 * training, or fills in afterwards, with rest between them. Marking a Set done
 * starts its prescribed rest; a rest row starts its rest on demand. Only the
 * Sets marked done are logged, so only their fields are required; a Logged
 * Session opens with its Sets done in the rows they were planned in, so saving
 * again corrects it. An EMOM or AMRAP has a clock to start; an AMRAP is logged
 * as its Score, the Rounds and extra Reps done, rather than as Sets.
 *
 * When the Session was logged elsewhere while the form was open, the form keeps
 * what the Lifter filled in and switches to correction mode, saying that saving
 * now overwrites that Logged Session.
 */
export function LogSessionForm({ session, today }: { session: SessionToLog; today: string }) {
  const [date, setDate] = useState(session.loggedDate ?? today);
  const [layout] = useState(() => {
    let key = 0;
    return session.parts.map((part, partIndex) => ({
      title: part.title,
      clock: part.clock,
      amrap: part.amrap,
      rows: part.rows.map((row, rowIndex): LayoutRow =>
        row.kind === "set"
          ? { kind: "set", key: key++ }
          : {
              kind: "rest",
              key: `${partIndex}-${rowIndex}`,
              seconds: row.seconds,
              place: row.place,
            },
      ),
    }));
  });
  const [rows, setRows] = useState<SetRow[]>(() =>
    session.parts
      .flatMap((part) => part.rows.flatMap((row) => (row.kind === "set" ? [row.set] : [])))
      .map((set, key) => ({
        key,
        exercise: set.exercise,
        side: set.side,
        reps: String(set.reps),
        repsRange: set.repsRange,
        bellKg: set.bellKg === null ? "" : String(set.bellKg),
        holdSeconds: set.holdSeconds === null ? "" : String(set.holdSeconds),
        workSeconds: set.workSeconds === null ? "" : String(set.workSeconds),
        restSeconds: set.restSeconds,
        place: set.place,
        done: set.done,
      })),
  );
  const [scores, setScores] = useState<ScoreEntry[]>(() =>
    session.parts.flatMap((part) =>
      part.amrap
        ? [
            {
              part: part.amrap.part,
              rounds: part.amrap.rounds === null ? "" : String(part.amrap.rounds),
              extraReps: part.amrap.extraReps === null ? "" : String(part.amrap.extraReps),
            },
          ]
        : [],
    ),
  );
  const [logged, setLogged] = useState(session.loggedDate !== null);
  // Set when a log was refused because the Session was logged elsewhere; the next save overwrites it.
  const [elsewhere, setElsewhere] = useState<{ logged: LoggedElsewhere | null } | null>(null);
  const [rest, setRest] = useState<{
    endsAt: number;
    seconds: number;
    key: number | string;
  } | null>(null);
  const [saving, setSaving] = useState<Saving>({ status: "idle" });

  // An edit makes a "saved" or "failed" message stale: it no longer describes the form.
  const edited = () =>
    setSaving((current) => (current.status === "saving" ? current : { status: "idle" }));

  const update = (key: number, change: Partial<SetRow>) => {
    edited();
    setRows((current) => current.map((row) => (row.key === key ? { ...row, ...change } : row)));
  };

  const toggleDone = (row: SetRow) => {
    update(row.key, { done: !row.done });
    if (!row.done && row.restSeconds) {
      setRest({
        endsAt: Date.now() + row.restSeconds * 1000,
        seconds: row.restSeconds,
        key: row.key,
      });
    } else if (row.done && rest?.key === row.key) {
      setRest(null);
    }
  };

  const updateScore = (part: number, change: Partial<ScoreEntry>) => {
    edited();
    setScores((current) =>
      current.map((score) => (score.part === part ? { ...score, ...change } : score)),
    );
  };

  const done = doneSets(rows);
  const scored = filledScores(scores);

  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    setSaving({ status: "saving" });
    try {
      const send = logged ? correctLoggedSession : logSession;
      await send(session.sessionId, date, done, scored);
      setLogged(true);
      setElsewhere(null);
      setSaving({ status: "saved" });
    } catch (error) {
      if (error instanceof SessionLoggedElsewhere) {
        setLogged(true);
        setElsewhere({ logged: error.logged });
        setSaving({ status: "idle" });
        return;
      }
      setSaving({ status: "failed", message: errorMessage(error) });
    }
  };

  return (
    <form className="log-session" onSubmit={save}>
      <p>
        <Link to={`/programs/${encodeURIComponent(session.programId)}`}>
          ← {session.programName}
        </Link>
      </p>
      <h1>
        Week {session.weekNumber} · Session {session.sessionNumber}
      </h1>
      {session.equipment.length > 0 && (
        <p className="equipment">
          <strong>Equipment:</strong> {session.equipment.join(", ")}
        </p>
      )}
      {session.notes && (
        <section className="notes" aria-label="Notes">
          {session.notes}
        </section>
      )}
      {elsewhere ? (
        <div role="alert" className="log-state logged-elsewhere">
          <p>
            <strong>This Session was logged elsewhere</strong>
            {elsewhere.logged && ` on ${elsewhere.logged.date} (${contents(elsewhere.logged)})`}.
          </p>
          <p>Your Sets below are kept. Saving now overwrites that Logged Session.</p>
        </div>
      ) : (
        <p className="log-state">
          {logged ? "Logged: saving corrects this Logged Session." : "Not logged yet."}
        </p>
      )}
      <label className="log-date">
        Date{" "}
        <input
          type="date"
          required
          value={date}
          onChange={(e) => {
            edited();
            setDate(e.target.value);
          }}
        />
      </label>

      {rest && (
        <RestTimer
          key={rest.endsAt}
          endsAt={rest.endsAt}
          totalSeconds={rest.seconds}
          onDismiss={() => setRest(null)}
        />
      )}

      {layout.every((part) => part.rows.length === 0 && !part.amrap) ? (
        <p>This Session has no Prescriptions yet.</p>
      ) : (
        <table className="log-sets">
          <thead>
            <tr>
              <th scope="col">Set</th>
              <th scope="col">Exercise</th>
              <th scope="col">Side</th>
              <th scope="col">Reps</th>
              <th scope="col">Time (s)</th>
              <th scope="col">Bell (kg)</th>
              <th scope="col">Rest</th>
              <th scope="col">Done</th>
            </tr>
          </thead>
          {layout.map((part) => (
            <tbody key={part.title} aria-label={part.title}>
              <tr className="part-heading">
                <th scope="rowgroup" colSpan={COLUMNS}>
                  {part.title}
                </th>
              </tr>
              {part.clock && (
                <tr className="clock-row">
                  <td colSpan={COLUMNS}>
                    <PartClockView
                      format={part.clock.format}
                      minutes={part.clock.minutes}
                      name={part.title}
                    />
                  </td>
                </tr>
              )}
              {part.amrap && (
                <AmrapRow
                  amrap={part.amrap}
                  score={scores.find((score) => score.part === part.amrap?.part)}
                  update={updateScore}
                />
              )}
              {part.rows.map((item) =>
                item.kind === "rest" ? (
                  <tr key={item.key} className="rest-row">
                    <th scope="row" className="place">
                      {item.place}
                    </th>
                    <td colSpan={COLUMNS - 2}>Rest {item.seconds} s</td>
                    <td>
                      <button
                        type="button"
                        onClick={() =>
                          setRest({
                            endsAt: Date.now() + item.seconds * 1000,
                            seconds: item.seconds,
                            key: item.key,
                          })
                        }
                      >
                        Start rest
                      </button>
                    </td>
                  </tr>
                ) : (
                  <SetRowView
                    key={item.key}
                    row={rows[item.key]}
                    number={item.key + 1}
                    update={update}
                    toggleDone={toggleDone}
                  />
                ),
              )}
            </tbody>
          ))}
        </table>
      )}

      <div className="log-actions">
        <button
          type="submit"
          className="primary"
          disabled={(done.length === 0 && scored.length === 0) || saving.status === "saving"}
        >
          {elsewhere ? "Overwrite Logged Session" : logged ? "Correct Logged Session" : "Log Session"}
        </button>
        <span>
          {done.length} of {rows.length} Sets done
          {scores.length > 0 && `, ${scored.length} of ${scores.length} AMRAPs scored`}
        </span>
      </div>
      {saving.status === "saved" && <p role="status">Logged Session saved.</p>}
      {saving.status === "failed" && (
        <div role="alert" className="error">
          <p>Could not save the Logged Session: {saving.message}</p>
        </div>
      )}
    </form>
  );
}

/** What a Logged Session holds, as "3 Sets, 1 Score". */
function contents(logged: LoggedElsewhere): string {
  const count = (n: number, noun: string) => `${n} ${noun}${n === 1 ? "" : "s"}`;
  return [
    count(logged.sets, "Set"),
    ...(logged.scores > 0 ? [count(logged.scores, "Score")] : []),
  ].join(", ");
}

/** One Round of an AMRAP, and its Score: whole Rounds and extra Reps into the next. */
function AmrapRow({
  amrap,
  score,
  update,
}: {
  amrap: NonNullable<SessionToLog["parts"][number]["amrap"]>;
  score: ScoreEntry | undefined;
  update: (part: number, change: Partial<ScoreEntry>) => void;
}) {
  const name = `Part ${amrap.part}`;
  return (
    <tr className="amrap-row">
      <td colSpan={COLUMNS}>
        <p className="amrap-round">
          <strong>One Round:</strong> {amrap.round.length > 0 ? amrap.round.join("; ") : "–"}
        </p>
        <label>
          Rounds{" "}
          <input
            type="number"
            min={0}
            aria-label={`${name} Rounds`}
            value={score?.rounds ?? ""}
            onChange={(e) => update(amrap.part, { rounds: e.target.value })}
          />
        </label>{" "}
        <label>
          + extra Reps{" "}
          <input
            type="number"
            min={0}
            aria-label={`${name} extra Reps`}
            value={score?.extraReps ?? ""}
            onChange={(e) => update(amrap.part, { extraReps: e.target.value })}
          />
        </label>
      </td>
    </tr>
  );
}

function SetRowView({
  row,
  number,
  update,
  toggleDone,
}: {
  row: SetRow;
  number: number;
  update: (key: number, change: Partial<SetRow>) => void;
  toggleDone: (row: SetRow) => void;
}) {
  const name = `Set ${number}`;
  return (
    <tr aria-label={name} className={row.done ? "done" : undefined}>
      <th scope="row">
        {number}
        {row.place && <span className="place">{row.place}</span>}
      </th>
      <td>{row.exercise.name}</td>
      <td>{sideLabel(row.side, row.reps)}</td>
      <td>
        <input
          type="number"
          min={1}
          required={row.done}
          aria-label={`${name} Reps`}
          value={row.reps}
          onChange={(e) => update(row.key, { reps: e.target.value })}
        />
        {row.repsRange && <span className="rep-range"> of {row.repsRange}</span>}
      </td>
      <td>
        {row.exercise.hold && (
          <TimeField
            name={name}
            kind="hold"
            required={row.done}
            value={row.holdSeconds}
            onChange={(holdSeconds) => update(row.key, { holdSeconds })}
          />
        )}
        {row.exercise.timed && (
          <TimeField
            name={name}
            kind="work"
            required={row.done}
            value={row.workSeconds}
            onChange={(workSeconds) => update(row.key, { workSeconds })}
          />
        )}
      </td>
      <td>
        {row.exercise.bodyweight ? (
          <span className="no-bell">Bodyweight</span>
        ) : (
          <input
            type="number"
            step="any"
            required={row.done}
            ref={(input) => input?.setCustomValidity(row.done ? bellWeightProblem(row.bellKg) : "")}
            aria-label={`${name} Bell`}
            value={row.bellKg}
            onChange={(e) => update(row.key, { bellKg: e.target.value })}
          />
        )}
      </td>
      <td className="number">{row.restSeconds ? `${row.restSeconds} s` : "–"}</td>
      <td>
        <button
          type="button"
          aria-pressed={row.done}
          aria-label={`${name} done`}
          onClick={() => toggleDone(row)}
        >
          {row.done ? "✓ Done" : "Done"}
        </button>
      </td>
    </tr>
  );
}

/** A typed time as whole seconds, or null when it is not a time of at least 1 s. */
function wholeSeconds(typed: string): number | null {
  const seconds = Number(typed);
  return typed !== "" && Number.isInteger(seconds) && seconds >= 1 ? seconds : null;
}

/** The Hold time or Work time of a Set, with a timer that counts it down. */
function TimeField({
  name,
  kind,
  required,
  value,
  onChange,
}: {
  name: string;
  kind: "hold" | "work";
  required: boolean;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="hold">
      <input
        type="number"
        min={1}
        required={required}
        aria-label={`${name} ${kind === "hold" ? "Hold" : "Work"} time`}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
      <HoldTimer name={name} seconds={wholeSeconds(value)} kind={kind} />
    </div>
  );
}
