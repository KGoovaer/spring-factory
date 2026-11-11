/**
 * The Session timer: how long the Lifter has trained so far. It counts while it
 * runs, keeps what it counted when stopped, and goes on from there when started
 * again, on the wall clock so it stays right when the tab sleeps.
 *
 * `countedMs` is what earlier stretches counted; `runningSince` (epoch ms) is
 * when the current stretch started, or null while stopped.
 */
export interface SessionTimer {
  countedMs: number;
  runningSince: number | null;
}

/** A stopped timer that already counted `seconds`, such as the Session time of a Logged Session. */
export function stoppedAt(seconds: number): SessionTimer {
  return { countedMs: seconds * 1000, runningSince: null };
}

/** The timer started at `now`, going on from what it counted; a timer never started starts at 0. */
export function startSessionTimer(timer: SessionTimer | null, now: number): SessionTimer {
  if (timer?.runningSince != null) {
    return timer;
  }
  return { countedMs: timer?.countedMs ?? 0, runningSince: now };
}

/** The timer stopped at `now`, keeping all it counted. */
export function stopSessionTimer(timer: SessionTimer, now: number): SessionTimer {
  return { countedMs: countedMs(timer, now), runningSince: null };
}

/** The whole seconds the timer counted by `now`: the Session time. */
export function sessionSeconds(timer: SessionTimer, now: number): number {
  return Math.floor(countedMs(timer, now) / 1000);
}

/** A Session time as a clock: `0:09`, `45:12`, `1:02:03`. */
export function formatSessionTime(seconds: number): string {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = String(seconds % 60).padStart(2, "0");
  return hours > 0 ? `${hours}:${String(minutes).padStart(2, "0")}:${secs}` : `${minutes}:${secs}`;
}

function countedMs(timer: SessionTimer, now: number): number {
  return (
    timer.countedMs + (timer.runningSince === null ? 0 : Math.max(0, now - timer.runningSince))
  );
}
