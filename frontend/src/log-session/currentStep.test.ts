import { describe, expect, it } from "vitest";
import { currentStep, sharedMinutes, type PartSteps, type Step } from "./currentStep";

const set = (key: number, done = false, minute: number | null = null): Step => ({
  kind: "set",
  key,
  minute,
  done,
});

const stopped = (...steps: Step[]): PartSteps => ({ steps, clockMinute: null });

/** An EMOM of 2 minutes: Sets 2 and 3 in minute 1, Set 4 in minute 2. */
const emom = (clockMinute: number | null, done: number[] = []): PartSteps => ({
  steps: [set(2, done.includes(2), 1), set(3, done.includes(3), 1), set(4, done.includes(4), 2)],
  clockMinute,
});

describe("currentStep", () => {
  it("is the first Set not done yet", () => {
    expect(currentStep([stopped(set(0, true), set(1)), stopped(set(5))])).toEqual({
      sets: [1],
      amrap: null,
    });
  });

  it("is every Set not done yet of the first EMOM minute not done", () => {
    expect(currentStep([stopped(set(0, true)), emom(null)])).toEqual({
      sets: [2, 3],
      amrap: null,
    });
    expect(currentStep([emom(null, [2])])).toEqual({ sets: [3], amrap: null });
    expect(currentStep([emom(null, [2, 3])])).toEqual({
      sets: [4],
      amrap: null,
    });
  });

  it("follows a running EMOM clock to the Sets of its minute", () => {
    expect(currentStep([stopped(set(0)), emom(2)])).toEqual({
      sets: [4],
      amrap: null,
    });
    expect(currentStep([stopped(set(0)), emom(1, [2])])).toEqual({
      sets: [2, 3],
      amrap: null,
    });
  });

  it("is an AMRAP without a Score, or one whose clock runs", () => {
    const amrap = (scored: boolean, clockMinute: number | null = null): PartSteps => ({
      steps: [{ kind: "amrap", part: 2, scored }],
      clockMinute,
    });

    expect(currentStep([stopped(set(0, true)), amrap(false)])).toEqual({
      sets: [],
      amrap: 2,
    });
    expect(currentStep([amrap(true), stopped(set(1))])).toEqual({
      sets: [1],
      amrap: null,
    });
    expect(currentStep([stopped(set(0)), amrap(true, 3)])).toEqual({
      sets: [],
      amrap: 2,
    });
  });

  it("is nothing once every step is done", () => {
    expect(currentStep([stopped(set(0, true)), emom(null, [2, 3, 4])])).toEqual({
      sets: [],
      amrap: null,
    });
  });
});

describe("sharedMinutes", () => {
  it("names the EMOM minutes with more than one Set", () => {
    expect(sharedMinutes([null, null, 1, 1, 2, 3, 3, 3])).toEqual(new Set([1, 3]));
  });
});
