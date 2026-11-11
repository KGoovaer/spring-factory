/**
 * One step of a Part on the log form, in order: a Set row (with the EMOM minute
 * it is done in, if any) or the Score of an AMRAP.
 */
export type Step =
  | { kind: "set"; key: number; minute: number | null; done: boolean }
  | { kind: "amrap"; part: number; scored: boolean };

/** A Part's steps, and the minute its EMOM or AMRAP clock is in, or null when it is not running. */
export interface PartSteps {
  steps: Step[];
  clockMinute: number | null;
}

/** What the Lifter is to do now: these Set rows, or the AMRAP of this Part number. */
export interface CurrentStep {
  sets: number[];
  amrap: number | null;
}

/**
 * What the Lifter is to do now, to highlight on the form.
 *
 * - While an EMOM clock runs, the Sets of the minute it is in; while an AMRAP
 *   clock runs, that AMRAP.
 * - Otherwise the first step not done yet: a Set not marked done, or an AMRAP
 *   without a Score. In an EMOM that is every Set of that minute not done yet.
 * - Nothing once every step is done.
 */
export function currentStep(parts: PartSteps[]): CurrentStep {
  const running = parts.find((part) => part.clockMinute !== null);
  if (running) {
    const amrap = running.steps.find((step) => step.kind === "amrap");
    return amrap
      ? { sets: [], amrap: amrap.part }
      : {
          sets: setKeys(running.steps, (set) => set.minute === running.clockMinute),
          amrap: null,
        };
  }
  for (const part of parts) {
    for (const step of part.steps) {
      if (step.kind === "amrap" && !step.scored) {
        return { sets: [], amrap: step.part };
      }
      if (step.kind === "set" && !step.done) {
        return step.minute === null
          ? { sets: [step.key], amrap: null }
          : {
              sets: setKeys(part.steps, (set) => set.minute === step.minute && !set.done),
              amrap: null,
            };
      }
    }
  }
  return { sets: [], amrap: null };
}

/** Where a row of a Part sits among the EMOM minute headings of the form. */
export interface MinuteGroup {
  /** The minute heading the row sits under, or null when it sits under none. */
  minute: number | null;
  /** Whether the row is the first under that heading, so the heading goes above it. */
  startsGroup: boolean;
}

/**
 * How the form groups a Part's rows, given the EMOM minute of each (null for a
 * row outside an EMOM or for rest): a minute that holds more than one Set gets
 * a heading of its own, above the first of its rows, and its rows sit under it.
 */
export function minuteGroups(minutes: (number | null)[]): MinuteGroup[] {
  const counts = new Map<number, number>();
  for (const minute of minutes) {
    if (minute !== null) {
      counts.set(minute, (counts.get(minute) ?? 0) + 1);
    }
  }
  return minutes.map((minute, index) => {
    const grouped = minute !== null && (counts.get(minute) ?? 0) > 1;
    return {
      minute: grouped ? minute : null,
      startsGroup: grouped && minutes[index - 1] !== minute,
    };
  });
}

function setKeys(
  steps: Step[],
  matches: (set: Extract<Step, { kind: "set" }>) => boolean,
): number[] {
  return steps.flatMap((step) => (step.kind === "set" && matches(step) ? [step.key] : []));
}
