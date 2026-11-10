/** Whole seconds left until `endsAt` (epoch ms) at `now`, never below 0. */
export function secondsLeft(endsAt: number, now: number): number {
  return Math.max(0, Math.ceil((endsAt - now) / 1000));
}

/** Seconds as a clock: `1:05`, `0:09`, `12:00`. */
export function formatClock(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  return `${minutes}:${String(seconds % 60).padStart(2, "0")}`;
}
