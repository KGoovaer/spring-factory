import type { PartClock } from "./partClock";

/** A short beep counts one of the last 5 seconds down; a long beep marks 0. */
export type Beep = "short" | "long";

/** How many seconds before 0 the countdown starts beeping. */
const COUNTDOWN_SECONDS = 5;

/**
 * The beep for a countdown that just reached `secondsLeft`: a short beep at
 * each of the last 5 seconds, a long one at 0, and none otherwise or for a
 * countdown that is not running (null).
 */
export function countdownBeep(secondsLeft: number | null): Beep | null {
  if (secondsLeft === null || secondsLeft > COUNTDOWN_SECONDS) {
    return null;
  }
  return secondsLeft === 0 ? "long" : "short";
}

/**
 * The seconds an EMOM or AMRAP clock counts down to its next turn: the next
 * minute of an EMOM, or the end of an AMRAP's time cap. A new EMOM minute is
 * its 0, so the minute that just started beeps long; the first minute does not,
 * nor does a clock that is not running (null).
 */
export function partClockCountdown(
  format: "EMOM" | "AMRAP",
  clock: PartClock | null,
): number | null {
  if (clock === null) {
    return null;
  }
  if (clock.over) {
    return 0;
  }
  return format === "EMOM" && clock.minute > 1 && clock.secondsLeft === 60 ? 0 : clock.secondsLeft;
}
