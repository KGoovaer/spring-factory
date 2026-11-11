import { useEffect, useState } from "react";
import { formatClock, secondsLeft } from "./restCountdown";
import { useCountdownBeep } from "./sound";

/**
 * Counts the rest after a Set down to 0 on the wall clock, so it stays right
 * when the tab sleeps. Shows "Rest is over" at 0; Skip ends it early. Beeps
 * the last 5 seconds and 0.
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
  const done = left === 0;
  useCountdownBeep(left);

  // One interval for the whole rest, stopped once it is over.
  useEffect(() => {
    if (done) {
      return;
    }
    const tick = setInterval(() => setNow(Date.now()), 250);
    return () => clearInterval(tick);
  }, [endsAt, done]);

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
