import { useEffect, useState } from "react";
import { formatClock, secondsLeft } from "./restCountdown";
import { useCountdownBeep } from "./sound";

/**
 * Counts one hold of a Hold Exercise down from its Hold time on the wall clock,
 * or, as `kind` "work", one Rep of a Timed Exercise from its Work time. Start
 * starts it, Stop ends it early, and at 0 it shows "Hold is over" (or "Work is
 * over") until the Lifter starts the next one. Beeps the last 5 seconds and 0.
 */
export function HoldTimer({
  name,
  seconds,
  kind = "hold",
}: {
  name: string;
  seconds: number | null;
  kind?: "hold" | "work";
}) {
  const Kind = kind === "hold" ? "Hold" : "Work";
  const [endsAt, setEndsAt] = useState<number | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const left = endsAt === null ? null : secondsLeft(endsAt, now);
  const running = left !== null && left > 0;
  useCountdownBeep(left);

  // One interval for the whole hold, stopped once it is over.
  useEffect(() => {
    if (!running) {
      return;
    }
    const tick = setInterval(() => setNow(Date.now()), 250);
    return () => clearInterval(tick);
  }, [endsAt, running]);

  const start = () => {
    const startedAt = Date.now();
    setNow(startedAt);
    setEndsAt(startedAt + (seconds ?? 0) * 1000);
  };

  return (
    <span className={left === 0 ? "hold-timer over" : "hold-timer"}>
      {left !== null && (
        <span role="timer" aria-label={`${name} ${kind}`}>
          {left === 0 ? `${Kind} is over` : formatClock(left)}
        </span>
      )}
      {running ? (
        <button type="button" aria-label={`${name} Stop ${kind}`} onClick={() => setEndsAt(null)}>
          Stop
        </button>
      ) : (
        <button
          type="button"
          aria-label={`${name} Start ${kind}`}
          disabled={!seconds}
          onClick={start}
        >
          Start {kind}
        </button>
      )}
    </span>
  );
}
