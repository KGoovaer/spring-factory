import { useEffect, useState } from "react";
import { formatClock, secondsLeft } from "./restCountdown";

/**
 * Counts the rest after a Set down to 0 on the wall clock, so it stays right
 * when the tab sleeps. Shows "Rest is over" at 0; Skip ends it early.
 */
export function RestTimer({
  endsAt,
  totalSeconds,
  onDismiss,
}: {
  endsAt: number;
  totalSeconds: number;
  onDismiss: () => void;
}) {
  const [now, setNow] = useState(() => Date.now());
  const left = secondsLeft(endsAt, now);

  useEffect(() => {
    if (left === 0) {
      return;
    }
    const tick = setInterval(() => setNow(Date.now()), 250);
    return () => clearInterval(tick);
  }, [left]);

  const done = left === 0;
  return (
    <section className={done ? "rest-timer over" : "rest-timer"} aria-label="Rest">
      <span className="rest-label">{done ? "Rest is over" : "Rest"}</span>
      <span role="timer" className="rest-clock">
        {formatClock(left)}
      </span>
      <progress max={totalSeconds} value={totalSeconds - left} aria-hidden="true" />
      <button type="button" onClick={onDismiss}>
        {done ? "Close" : "Skip rest"}
      </button>
    </section>
  );
}
