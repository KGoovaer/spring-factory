import { describe, expect, it } from "vitest";
import {
  formatSessionTime,
  sessionSeconds,
  startSessionTimer,
  stoppedAt,
  stopSessionTimer,
} from "./sessionTimer";

describe("the Session timer", () => {
  it("counts whole seconds from 0 once started", () => {
    const timer = startSessionTimer(null, 10_000);

    expect(sessionSeconds(timer, 10_000)).toBe(0);
    expect(sessionSeconds(timer, 10_999)).toBe(0);
    expect(sessionSeconds(timer, 75_000)).toBe(65);
  });

  it("keeps what it counted when stopped, and goes on from there when started again", () => {
    const stopped = stopSessionTimer(startSessionTimer(null, 0), 90_500);

    expect(sessionSeconds(stopped, 500_000)).toBe(90);
    const again = startSessionTimer(stopped, 600_000);
    expect(sessionSeconds(again, 610_000)).toBe(100);
  });

  it("does not restart a running timer when started again", () => {
    const running = startSessionTimer(null, 0);

    expect(startSessionTimer(running, 30_000)).toBe(running);
  });

  it("goes on from the Session time of a Logged Session", () => {
    expect(sessionSeconds(stoppedAt(2_710), 0)).toBe(2_710);
    expect(sessionSeconds(startSessionTimer(stoppedAt(60), 0), 5_000)).toBe(65);
  });
});

describe("formatSessionTime", () => {
  it("shows minutes and seconds, and hours once there are any", () => {
    expect(formatSessionTime(9)).toBe("0:09");
    expect(formatSessionTime(2_712)).toBe("45:12");
    expect(formatSessionTime(3_723)).toBe("1:02:03");
  });
});
