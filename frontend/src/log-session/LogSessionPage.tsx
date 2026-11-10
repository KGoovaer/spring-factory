import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { LogSessionForm } from "./LogSessionForm";
import { loadSessionToLog, type SessionToLog } from "./logSession";

type Result = { status: "failed"; message: string } | { status: "loaded"; session: SessionToLog };

type State = { status: "loading" } | Result;

/** Today as `yyyy-mm-dd` in the Lifter's own time zone. */
function today(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

/** The page at /programs/:id/sessions/:sessionId/log: log a Session, or correct its log. */
export function LogSessionPage() {
  const { id = "", sessionId = "" } = useParams();
  const key = `${id}/${sessionId}`;
  const [result, setResult] = useState<{ key: string; result: Result } | null>(null);
  const state: State = result?.key === key ? result.result : { status: "loading" };

  useEffect(() => {
    let current = true;
    const settle = (outcome: Result) => current && setResult({ key, result: outcome });
    loadSessionToLog(id, sessionId).then(
      (session) => settle({ status: "loaded", session }),
      (error: unknown) =>
        settle({
          status: "failed",
          message: error instanceof Error ? error.message : String(error),
        }),
    );
    return () => {
      current = false;
    };
  }, [id, sessionId, key]);

  return (
    <main className="page">
      {state.status === "loading" && <p role="status">Loading Session…</p>}
      {state.status === "failed" && (
        <div role="alert" className="error">
          <h1>Could not load this Session</h1>
          <p>{state.message}</p>
        </div>
      )}
      {state.status === "loaded" && <LogSessionForm session={state.session} today={today()} />}
    </main>
  );
}
