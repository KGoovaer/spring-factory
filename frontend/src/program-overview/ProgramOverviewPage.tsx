import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { loadProgramOverview, type ProgramOverview } from "./programOverview";
import { ProgramGrid } from "./ProgramGrid";

type Result =
  | { status: "failed"; message: string }
  | { status: "loaded"; program: ProgramOverview };

type State = { status: "loading" } | Result;

/** The page at /programs/:id: loads the Program and shows it as a grid. */
export function ProgramOverviewPage() {
  const { id = "" } = useParams();
  // The result remembers which Program it is for, so opening another id
  // shows "loading" again without resetting state inside the effect.
  const [result, setResult] = useState<{ id: string; result: Result } | null>(null);
  const state: State = result?.id === id ? result.result : { status: "loading" };

  useEffect(() => {
    let current = true;
    const settle = (outcome: Result) => current && setResult({ id, result: outcome });
    loadProgramOverview(id).then(
      (program) => settle({ status: "loaded", program }),
      (error: unknown) =>
        settle({
          status: "failed",
          message: error instanceof Error ? error.message : String(error),
        }),
    );
    return () => {
      current = false;
    };
  }, [id]);

  return (
    <main className="page">
      {state.status === "loading" && <p role="status">Loading Program…</p>}
      {state.status === "failed" && (
        <div role="alert" className="error">
          <h1>Could not load this Program</h1>
          <p>{state.message}</p>
        </div>
      )}
      {state.status === "loaded" && <ProgramGrid program={state.program} />}
    </main>
  );
}
