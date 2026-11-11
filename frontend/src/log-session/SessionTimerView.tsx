import { useEffect, useState } from "react";
import { formatSessionTime, sessionSeconds, type SessionTimer } from "./sessionTimer";

/**
 * The Session timer of the whole Session: Start session starts it, Stop session
 * stops it, and Resume session goes on from what it counted. Not shown until
 * started (or opened with the Session time of a Logged Session).
 */
export function SessionTimerView({
  timer,
  onStart,
  onStop,
}: {
  timer: SessionTimer | null;
  onStart: () => void;
  onStop: () => void;
}) {
  const [now, setNow] = useState(() => Date.now());
  const running = timer?.runningSince != null;

  // One interval while the timer runs.
  useEffect(() => {
    if (!running) {
      return;
    }
    const tick = setInterval(() => setNow(Date.now()), 250);
    return () => clearInterval(tick);
  }, [running]);

  return (
    <span className="session-timer">
      {timer && (
        <span role="timer" aria-label="Session time" className="session-clock">
          {formatSessionTime(sessionSeconds(timer, now))}
        </span>
      )}
      {running ? (
        <button type="button" onClick={onStop}>
          Stop session
        </button>
      ) : (
        <button type="button" className={timer ? undefined : "primary"} onClick={onStart}>
          {timer ? "Resume session" : "Start session"}
        </button>
      )}
    </span>
  );
}
