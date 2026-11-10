import { useCallback } from "react";
import { useParams } from "react-router-dom";
import { useLoaded } from "../api/useLoaded";
import { loadProgramOverview } from "./programOverview";
import { ProgramGrid } from "./ProgramGrid";

/** The page at /programs/:id: loads the Program and shows it as a grid. */
export function ProgramOverviewPage() {
  const { id = "" } = useParams();
  const state = useLoaded(useCallback(() => loadProgramOverview(id), [id]));

  return (
    <main className="page">
      {state.status === "loading" && <p role="status">Loading Program…</p>}
      {state.status === "failed" && (
        <div role="alert" className="error">
          <h1>Could not load this Program</h1>
          <p>{state.message}</p>
        </div>
      )}
      {state.status === "loaded" && <ProgramGrid program={state.value} />}
    </main>
  );
}
