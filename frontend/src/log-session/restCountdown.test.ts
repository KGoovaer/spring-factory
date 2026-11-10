import { describe, expect, it } from "vitest";
import { formatClock, secondsLeft } from "./restCountdown";

describe("secondsLeft", () => {
  it("rounds a part of a second up, so 0 means the rest is over", () => {
    expect(secondsLeft(10_000, 0)).toBe(10);
    expect(secondsLeft(10_000, 9_001)).toBe(1);
    expect(secondsLeft(10_000, 10_000)).toBe(0);
  });

  it("never goes below 0", () => {
    expect(secondsLeft(10_000, 25_000)).toBe(0);
  });
});

describe("formatClock", () => {
  it("shows minutes and two-digit seconds", () => {
    expect(formatClock(0)).toBe("0:00");
    expect(formatClock(9)).toBe("0:09");
    expect(formatClock(65)).toBe("1:05");
    expect(formatClock(720)).toBe("12:00");
  });
});
