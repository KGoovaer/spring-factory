/**
 * Where a running EMOM or AMRAP stands `elapsedSeconds` after the Lifter
 * started it, for a Part of `minutes` minutes.
 *
 * - An EMOM is in minute `minute` (from 1) with `secondsLeft` left of that
 *   minute: the rest before the next one starts.
 * - An AMRAP has `secondsLeft` left of its time cap; `minute` is the minute it
 *   is in.
 *
 * Both are `over` once all their minutes have passed.
 */
export interface PartClock {
  minute: number;
  secondsLeft: number;
  over: boolean;
}

export function emomClock(minutes: number, elapsedSeconds: number): PartClock {
  const elapsed = Math.max(0, Math.floor(elapsedSeconds));
  if (elapsed >= minutes * 60) {
    return { minute: minutes, secondsLeft: 0, over: true };
  }
  return { minute: Math.floor(elapsed / 60) + 1, secondsLeft: 60 - (elapsed % 60), over: false };
}

export function amrapClock(minutes: number, elapsedSeconds: number): PartClock {
  const elapsed = Math.max(0, Math.floor(elapsedSeconds));
  if (elapsed >= minutes * 60) {
    return { minute: minutes, secondsLeft: 0, over: true };
  }
  return { minute: Math.floor(elapsed / 60) + 1, secondsLeft: minutes * 60 - elapsed, over: false };
}
