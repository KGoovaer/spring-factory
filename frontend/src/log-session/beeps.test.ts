import { describe, expect, it } from "vitest";
import { countdownBeep, partClockCountdown } from "./beeps";
import { amrapClock, emomClock } from "./partClock";

describe("countdownBeep", () => {
  it("beeps short at each of the last 5 seconds and long at 0", () => {
    expect([6, 5, 4, 3, 2, 1, 0].map(countdownBeep)).toEqual([
      null,
      "short",
      "short",
      "short",
      "short",
      "short",
      "long",
    ]);
  });

  it("stays silent for a countdown that is not running", () => {
    expect(countdownBeep(null)).toBeNull();
  });
});

describe("partClockCountdown", () => {
  it("counts an EMOM down to each next minute, a new minute being its 0", () => {
    expect(partClockCountdown("EMOM", emomClock(3, 0))).toBe(60);
    expect(partClockCountdown("EMOM", emomClock(3, 56))).toBe(4);
    expect(partClockCountdown("EMOM", emomClock(3, 60))).toBe(0);
    expect(partClockCountdown("EMOM", emomClock(3, 61))).toBe(59);
    expect(partClockCountdown("EMOM", emomClock(3, 180))).toBe(0);
  });

  it("counts an AMRAP down to the end of its time cap", () => {
    expect(partClockCountdown("AMRAP", amrapClock(2, 60))).toBe(60);
    expect(partClockCountdown("AMRAP", amrapClock(2, 117))).toBe(3);
    expect(partClockCountdown("AMRAP", amrapClock(2, 120))).toBe(0);
  });

  it("has no countdown for a clock that is not running", () => {
    expect(partClockCountdown("EMOM", null)).toBeNull();
  });
});
