import { useEffect, useState } from "react";
import { formatClock, secondsLeft } from "./restCountdown";

/**
 * Counts one hold of a Hold Exercise down from its Hold time on the wall clock.
 * Start hold starts it, Stop hold ends it early, and at 0 it shows "Hold is
 * over" until the Lifter starts the next hold.
 */
export function HoldTimer({ name, seconds }: { name: string; seconds: number | null }) {
  const [endsAt, setEndsAt] = useState<number | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const left = endsAt === null ? null : secondsLeft(endsAt, now);
  const running = left !== null && left > 0;

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
        <span role="timer" aria-label={`${name} hold`}>
          {left === 0 ? "Hold is over" : formatClock(left)}
        </span>
      )}
      {running ? (
        <button type="button" aria-label={`${name} Stop hold`} onClick={() => setEndsAt(null)}>
          Stop
        </button>
      ) : (
        <button type="button" aria-label={`${name} Start hold`} disabled={!seconds} onClick={start}>
          Start hold
        </button>
      )}
    </span>
  );
}
