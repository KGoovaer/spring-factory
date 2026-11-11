import { getJson, sendJson } from "../api/client";
import type {
  ExerciseResponse,
  PartFormat,
  PrescriptionResponse,
  ProgramResponse,
  Side,
} from "../api/programs";
import { repsLabel, sideLabel } from "../api/programs";

/** A Program as the setup page shows it, with the Exercises a Lifter can prescribe. */
export interface ProgramSetup {
  program: ProgramResponse;
  exercises: ExerciseResponse[];
}

/** A new Part, as the Lifter fills it in: only the fields its format takes. */
export interface NewPart {
  format: PartFormat;
  rounds?: number;
  minutes?: number;
  rotation?: number;
  restSeconds?: number;
}

/** A new Prescription, as the API takes it. */
export interface NewPrescription {
  exerciseId: string;
  sets: number;
  reps: number;
  repsMax: number | null;
  side: Side | null;
  holdSeconds: number | null;
  workSeconds: number | null;
  bellKg: number | null;
  restSeconds: number | null;
  minute: number | null;
}

/** A new Exercise for the catalog. */
export interface NewExercise {
  name: string;
  bodyweight: boolean;
  hold: boolean;
  timed: boolean;
  gear: string | null;
}

/** Loads the Program and the Exercise catalog. Throws ApiError when a call fails. */
export async function loadProgramSetup(programId: string): Promise<ProgramSetup> {
  const [program, exercises] = await Promise.all([
    getJson<ProgramResponse>(`/api/programs/${encodeURIComponent(programId)}`),
    getJson<ExerciseResponse[]>("/api/exercises"),
  ]);
  return { program, exercises };
}

/** Creates a Program with this name and returns its id. Throws ApiError, with the reason. */
export async function createProgram(name: string): Promise<string> {
  const program = await sendJson<{ id: string }>("POST", "/api/programs", { name });
  return program.id;
}

/** Appends the next Week to the Program. */
export async function addWeek(programId: string): Promise<void> {
  await sendJson("POST", `/api/programs/${encodeURIComponent(programId)}/weeks`, undefined);
}

/** Appends an empty Session to the Week. */
export async function addSession(weekId: string): Promise<void> {
  await sendJson("POST", `/api/weeks/${encodeURIComponent(weekId)}/sessions`, undefined);
}

/** Appends the Part to the Session. The server rejects fields its format does not take. */
export async function addPart(sessionId: string, part: NewPart): Promise<void> {
  await sendJson("POST", `${sessionPath(sessionId)}/parts`, part);
}

/** Appends the Prescription to Part `partNumber` of the Session. */
export async function addPrescription(
  sessionId: string,
  partNumber: number,
  prescription: NewPrescription,
): Promise<void> {
  await sendJson(
    "POST",
    `${sessionPath(sessionId)}/parts/${partNumber}/prescriptions`,
    prescription,
  );
}

/** Replaces the notes of the Session; blank notes clear them. */
export async function changeNotes(sessionId: string, notes: string): Promise<void> {
  await sendJson("PUT", `${sessionPath(sessionId)}/notes`, { notes });
}

/** Adds the Exercise to the catalog. Throws ApiError when the name is taken. */
export async function addExercise(exercise: NewExercise): Promise<void> {
  await sendJson("POST", "/api/exercises", exercise);
}

/**
 * A Prescription as one line, the way a Lifter writes it: "3 × 8+8 Clean to
 * waiter press · 16 kg", "5 × Swing · Right · 16 kg", "1 × 45 s Skipping",
 * "2 × 3-4 Swing · 24 kg".
 */
export function describePrescription(
  prescription: PrescriptionResponse,
  exercises: ExerciseResponse[],
): string {
  const name =
    exercises.find((exercise) => exercise.id === prescription.exerciseId)?.name ??
    "Unknown Exercise";
  const range = repsLabel(prescription.reps, prescription.repsMax);
  const reps = prescription.side === "EACH" ? sideLabel("EACH", range) : range;
  const parts = [`${prescription.sets} × ${reps} ${name}`];
  if (prescription.side === "LEFT" || prescription.side === "RIGHT") {
    parts.push(sideLabel(prescription.side, prescription.reps));
  }
  if (prescription.holdSeconds !== null) {
    parts.push(`hold ${prescription.holdSeconds} s`);
  }
  if (prescription.workSeconds !== null) {
    parts.push(`${prescription.workSeconds} s`);
  }
  if (prescription.bellKg !== null) {
    parts.push(`${prescription.bellKg} kg`);
  }
  if (prescription.restSeconds) {
    parts.push(`rest ${prescription.restSeconds} s`);
  }
  return parts.join(" · ");
}

function sessionPath(sessionId: string): string {
  return `/api/sessions/${encodeURIComponent(sessionId)}`;
}
