import { useState } from "react";
import { Link } from "react-router-dom";
import { errorMessage } from "../api/client";
import { RestTimer } from "./RestTimer";
import {
  bellWeightProblem,
  saveLoggedSession,
  type LoggedSetInput,
  type SessionToLog,
} from "./logSession";

interface SetRow {
  key: number;
  exerciseId: string;
  exerciseName: string;
  bodyweight: boolean;
  hold: boolean;
  reps: string;
  bellKg: string;
  holdSeconds: string;
  restSeconds: number | null;
  done: boolean;
}

type Saving =
  | { status: "idle" }
  | { status: "saving" }
  | { status: "saved" }
  | { status: "failed"; message: string };

/**
 * The Sets of one Session as rows the Lifter marks done while training, or
 * fills in afterwards. Marking a Set done starts its prescribed rest. Only the
 * Sets marked done are logged, so only their fields are required; a Logged
 * Session opens with all of its Sets done, so saving again corrects it.
 */
export function LogSessionForm({ session, today }: { session: SessionToLog; today: string }) {
  const [date, setDate] = useState(session.loggedDate ?? today);
  const [rows, setRows] = useState<SetRow[]>(() =>
    session.sets.map((set, key) => ({
      key,
      exerciseId: set.exercise.id,
      exerciseName: set.exercise.name,
      bodyweight: set.exercise.bodyweight,
      hold: set.exercise.hold,
      reps: String(set.reps),
      bellKg: set.bellKg === null ? "" : String(set.bellKg),
      holdSeconds: set.holdSeconds === null ? "" : String(set.holdSeconds),
      restSeconds: set.restSeconds,
      done: session.loggedDate !== null,
    })),
  );
  const [logged, setLogged] = useState(session.loggedDate !== null);
  const [rest, setRest] = useState<{ endsAt: number; seconds: number; key: number } | null>(null);
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
      setRest({ endsAt: Date.now() + row.restSeconds * 1000, seconds: row.restSeconds, key: row.key });
    } else if (row.done && rest?.key === row.key) {
      setRest(null);
    }
  };

  const doneSets: LoggedSetInput[] = rows
    .filter((row) => row.done)
    .map((row) => ({
      exerciseId: row.exerciseId,
      reps: Number(row.reps),
      holdSeconds: row.hold && row.holdSeconds !== "" ? Number(row.holdSeconds) : null,
      bellKg: !row.bodyweight && row.bellKg !== "" ? Number(row.bellKg) : null,
    }));

  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    setSaving({ status: "saving" });
    try {
      await saveLoggedSession(session.sessionId, date, doneSets, logged);
      setLogged(true);
      setSaving({ status: "saved" });
    } catch (error) {
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
      <p className="log-state">
        {logged ? "Logged: saving corrects this Logged Session." : "Not logged yet."}
      </p>
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

      {rows.length === 0 ? (
        <p>This Session has no Prescriptions yet.</p>
      ) : (
        <table className="log-sets">
          <thead>
            <tr>
              <th scope="col">Set</th>
              <th scope="col">Exercise</th>
              <th scope="col">Reps</th>
              <th scope="col">Hold time (s)</th>
              <th scope="col">Bell (kg)</th>
              <th scope="col">Rest</th>
              <th scope="col">Done</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, index) => {
              const name = `Set ${index + 1}`;
              return (
                <tr key={row.key} aria-label={name} className={row.done ? "done" : undefined}>
                  <th scope="row">{index + 1}</th>
                  <td>{row.exerciseName}</td>
                  <td>
                    <input
                      type="number"
                      min={1}
                      required={row.done}
                      aria-label={`${name} Reps`}
                      value={row.reps}
                      onChange={(e) => update(row.key, { reps: e.target.value })}
                    />
                  </td>
                  <td>
                    {row.hold && (
                      <input
                        type="number"
                        min={1}
                        required={row.done}
                        aria-label={`${name} Hold time`}
                        value={row.holdSeconds}
                        onChange={(e) => update(row.key, { holdSeconds: e.target.value })}
                      />
                    )}
                  </td>
                  <td>
                    {row.bodyweight ? (
                      <span className="no-bell">Bodyweight</span>
                    ) : (
                      <input
                        type="number"
                        step="any"
                        required={row.done}
                        ref={(input) =>
                          input?.setCustomValidity(row.done ? bellWeightProblem(row.bellKg) : "")
                        }
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
            })}
          </tbody>
        </table>
      )}

      <div className="log-actions">
        <button
          type="submit"
          className="primary"
          disabled={doneSets.length === 0 || saving.status === "saving"}
        >
          {logged ? "Correct Logged Session" : "Log Session"}
        </button>
        <span>
          {doneSets.length} of {rows.length} Sets done
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
