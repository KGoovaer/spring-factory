import { useCallback, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { errorMessage } from "../api/client";
import { useLoaded } from "../api/useLoaded";
import { AddExerciseForm } from "./AddExerciseForm";
import { addSession, addWeek, loadProgramSetup, type ProgramSetup } from "./programSetup";
import { SessionSetupView } from "./SessionSetupView";

/** The page at /programs/:id/setup: set up the Weeks, Sessions, Parts and Prescriptions. */
export function ProgramSetupPage() {
  const { id = "" } = useParams();
  const state = useLoaded(useCallback(() => loadProgramSetup(id), [id]));

  return (
    <main className="page program-setup">
      {state.status === "loading" && <p role="status">Loading Program…</p>}
      {state.status === "failed" && (
        <div role="alert" className="error">
          <h1>Could not load this Program</h1>
          <p>{state.message}</p>
        </div>
      )}
      {state.status === "loaded" && <ProgramSetupView initial={state.value} />}
    </main>
  );
}

/**
 * The Program being set up. Every change is sent to the API, and the Program is
 * loaded again afterwards, so the page always shows what the server holds.
 */
function ProgramSetupView({ initial }: { initial: ProgramSetup }) {
  const [setup, setSetup] = useState(initial);
  const [failure, setFailure] = useState<string | null>(null);
  const { program, exercises } = setup;

  /** Runs a change, then shows the Program as the server now holds it. Throws what the change throws. */
  const change = async (send: () => Promise<void>) => {
    await send();
    setSetup(await loadProgramSetup(program.id));
  };

  /** A change from a button, whose error the page shows. */
  const run = (send: () => Promise<void>) => {
    setFailure(null);
    change(send).catch((error: unknown) => setFailure(errorMessage(error)));
  };

  return (
    <>
      <p>
        <Link to={`/programs/${encodeURIComponent(program.id)}`}>← Overview of {program.name}</Link>
      </p>
      <h1>Set up {program.name}</h1>
      {failure && (
        <div role="alert" className="error">
          <p>{failure}</p>
        </div>
      )}
      {program.weeks.length === 0 && <p>This Program has no Weeks yet.</p>}
      {program.weeks.map((week) => (
        <section key={week.id} className="setup-week" aria-label={`Week ${week.number}`}>
          <h2>Week {week.number}</h2>
          {week.sessions.map((session, index) => (
            <SessionSetupView
              key={session.id}
              programId={program.id}
              label={`Week ${week.number} Session ${index + 1}`}
              session={session}
              exercises={exercises}
              change={change}
            />
          ))}
          <button type="button" onClick={() => run(() => addSession(week.id))}>
            Add Session to Week {week.number}
          </button>
        </section>
      ))}
      <p>
        <button type="button" className="primary" onClick={() => run(() => addWeek(program.id))}>
          Add Week
        </button>
      </p>
      <AddExerciseForm change={change} />
    </>
  );
}
