import { useEffect, useState } from "react";
import { amrapClock, emomClock } from "./partClock";
import { formatClock } from "./restCountdown";

/**
 * The running clock of an EMOM or AMRAP Part, on the wall clock so it stays
 * right when the tab sleeps. An EMOM shows the minute it is in and what is left
 * of it; an AMRAP counts its time cap down. Stop ends it early.
 */
export function PartClockView({
  format,
  minutes,
  name,
}: {
  format: "EMOM" | "AMRAP";
  minutes: number;
  name: string;
}) {
  const [startedAt, setStartedAt] = useState<number | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const clock =
    startedAt === null
      ? null
      : (format === "EMOM" ? emomClock : amrapClock)(minutes, (now - startedAt) / 1000);
  const running = clock !== null && !clock.over;

  // One interval for the whole Part, stopped once it is over.
  useEffect(() => {
    if (!running) {
      return;
    }
    const tick = setInterval(() => setNow(Date.now()), 250);
    return () => clearInterval(tick);
  }, [startedAt, running]);

  const start = () => {
    const at = Date.now();
    setNow(at);
    setStartedAt(at);
  };

  return (
    <div className={clock?.over ? "part-clock over" : "part-clock"}>
      {clock && (
        <span role="timer" aria-label={`${name} clock`}>
          {clock.over
            ? `${format} is over`
            : format === "EMOM"
              ? `Minute ${clock.minute} of ${minutes} · ${formatClock(clock.secondsLeft)}`
              : `${formatClock(clock.secondsLeft)} left`}
        </span>
      )}
      {running ? (
        <button type="button" onClick={() => setStartedAt(null)}>
          Stop {format}
        </button>
      ) : (
        <button type="button" onClick={start}>
          Start {format}
        </button>
      )}
    </div>
  );
}
