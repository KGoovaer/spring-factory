/**
 * The JSON shapes of a Program as `GET /api/programs/{id}` answers it: Weeks of
 * Sessions, each made of Parts of Prescriptions (see
 * docs/adr/0005-a-session-is-made-of-parts.md).
 */

/** Which side a Prescription or Set works: one side, or Each side in turn (8+8). */
export type Side = "LEFT" | "RIGHT" | "EACH";

/** How a Part is done: straight Sets, Rounds, an EMOM, or only rest. */
export type PartFormat = "SETS" | "ROUNDS" | "EMOM" | "REST";

export interface PrescriptionResponse {
  exerciseId: string;
  sets: number;
  reps: number;
  side: Side | null;
  holdSeconds: number | null;
  workSeconds: number | null;
  bellKg: number | null;
  restSeconds: number | null;
  minute: number | null;
}

export interface PartResponse {
  number: number;
  format: PartFormat;
  rounds: number | null;
  minutes: number | null;
  rotation: number | null;
  restSeconds: number | null;
  prescriptions: PrescriptionResponse[];
}

export interface SessionResponse {
  id: string;
  notes: string | null;
  equipment: string[];
  parts: PartResponse[];
}

export interface WeekResponse {
  id: string;
  number: number;
  sessions: SessionResponse[];
}

export interface ProgramResponse {
  id: string;
  name: string;
  weeks: WeekResponse[];
}

/** An Exercise of the catalog, as `GET /api/exercises` answers it. */
export interface ExerciseResponse {
  id: string;
  name: string;
  bodyweight: boolean;
  hold: boolean;
  timed: boolean;
  gear: string | null;
  equipment: string;
}

/** How a Side reads next to Reps: "Right", "Left", or "8+8" for Each side. */
export function sideLabel(side: Side | null, reps: number | string): string {
  switch (side) {
    case "LEFT":
      return "Left";
    case "RIGHT":
      return "Right";
    case "EACH":
      return `${reps}+${reps}`;
    default:
      return "";
  }
}

/**
 * How a Part is titled, on the setup and the log page alike: "Part 1 · 20 min
 * EMOM, rotating through 4 minutes", "Part 2 · Rest 60 s", "Part 3 · 3 Rounds".
 */
export function partTitle(part: PartResponse): string {
  const title = `Part ${part.number} · `;
  switch (part.format) {
    case "SETS":
      return `${title}Sets`;
    case "ROUNDS":
      return `${title}${part.rounds} Rounds`;
    case "EMOM":
      return `${title}${part.minutes} min EMOM, rotating through ${part.rotation} minutes`;
    case "REST":
      return `${title}Rest ${part.restSeconds} s`;
  }
}
