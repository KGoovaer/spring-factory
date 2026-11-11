import { describe, expect, it } from "vitest";
import { amrapClock, emomClock } from "./partClock";

describe("emomClock", () => {
  it("starts minute 1 with the whole minute left", () => {
    expect(emomClock(10, 0)).toEqual({ minute: 1, secondsLeft: 60, over: false });
  });

  it("counts down what is left of the minute it is in", () => {
    expect(emomClock(10, 61.4)).toEqual({ minute: 2, secondsLeft: 59, over: false });
    expect(emomClock(10, 599)).toEqual({ minute: 10, secondsLeft: 1, over: false });
  });

  it("is over once every minute has passed", () => {
    expect(emomClock(10, 600)).toEqual({ minute: 10, secondsLeft: 0, over: true });
  });
});

describe("amrapClock", () => {
  it("counts the time cap down", () => {
    expect(amrapClock(12, 0)).toEqual({ minute: 1, secondsLeft: 720, over: false });
    expect(amrapClock(12, 90)).toEqual({ minute: 2, secondsLeft: 630, over: false });
  });

  it("is over at the time cap", () => {
    expect(amrapClock(12, 800)).toEqual({ minute: 12, secondsLeft: 0, over: true });
  });
});
