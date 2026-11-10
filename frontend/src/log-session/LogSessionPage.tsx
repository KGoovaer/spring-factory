import { useCallback } from "react";
import { useParams } from "react-router-dom";
import { useLoaded } from "../api/useLoaded";
import { LogSessionForm } from "./LogSessionForm";
import { loadSessionToLog } from "./logSession";

/** Today as `yyyy-mm-dd` in the Lifter's own time zone. */
function today(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

/** The page at /programs/:id/sessions/:sessionId/log: log a Session, or correct its log. */
export function LogSessionPage() {
  const { id = "", sessionId = "" } = useParams();
  const state = useLoaded(useCallback(() => loadSessionToLog(id, sessionId), [id, sessionId]));

  return (
    <main className="page">
      {state.status === "loading" && <p role="status">Loading Session…</p>}
      {state.status === "failed" && (
        <div role="alert" className="error">
          <h1>Could not load this Session</h1>
          <p>{state.message}</p>
        </div>
      )}
      {state.status === "loaded" && <LogSessionForm session={state.value} today={today()} />}
    </main>
  );
}
