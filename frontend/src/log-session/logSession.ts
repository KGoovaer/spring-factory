import { ApiError, getJson, sendJson } from "../api/client";
import type { PartResponse, PrescriptionResponse, ProgramResponse, Side } from "../api/programs";
import { partTitle } from "../api/programs";

/** An Exercise as the form needs it: whether its Sets take a Bell, a Hold time or a Work time. */
export interface ExerciseInfo {
  id: string;
  name: string;
  bodyweight: boolean;
  hold: boolean;
  timed: boolean;
}

/**
 * One Set row on the form: what was planned or logged, as the starting values
 * the Lifter edits. `restSeconds` is the rest the Prescription asks for after
 * this Set, or null when none was prescribed. `place` says where in its Part
 * the Set falls, such as "Round 2" or "Minute 5", or null.
 */
export interface SetToLog {
  exercise: ExerciseInfo;
  reps: number;
  side: Side | null;
  holdSeconds: number | null;
  workSeconds: number | null;
  bellKg: number | null;
  restSeconds: number | null;
  place: string | null;
  done: boolean;
}

/** A row of a Part on the form: a Set to tick off, or rest between Parts or in an EMOM. */
export type RowToLog =
  { kind: "set"; set: SetToLog } | { kind: "rest"; seconds: number; place: string | null };

/** A Part of the Session as the form lays it out: its title, then its rows in order. */
export interface PartToLog {
  title: string;
  rows: RowToLog[];
}

/** A Session opened for logging: where it sits in its Program, and its Parts. */
export interface SessionToLog {
  programId: string;
  programName: string;
  sessionId: string;
  weekNumber: number;
  sessionNumber: number;
  notes: string | null;
  equipment: string[];
  /** The date of the Logged Session, or null when the Session was not logged yet. */
  loggedDate: string | null;
  /**
   * The planned Parts with one row per Set and rest. Once there is a Logged
   * Session, its Sets fill the planned rows they match, and a last Part holds
   * any Sets that match none.
   */
  parts: PartToLog[];
}

/** One Set the Lifter really did, as the API takes it. */
export interface LoggedSetInput {
  exerciseId: string;
  reps: number;
  side: Side | null;
  holdSeconds: number | null;
  workSeconds: number | null;
  bellKg: number | null;
}

/** A Set as the Lifter fills it in on the form: the values as typed, and whether it was done. */
export interface SetEntry {
  exercise: ExerciseInfo;
  side: Side | null;
  reps: string;
  holdSeconds: string;
  workSeconds: string;
  bellKg: string;
  done: boolean;
}

/**
 * The Sets marked done, as the API takes them. Only a Hold Exercise has a Hold
 * time, only a Timed Exercise a Work time and only a Kettlebell Exercise a
 * Bell; the others send null.
 */
export function doneSets(entries: SetEntry[]): LoggedSetInput[] {
  return entries
    .filter((entry) => entry.done)
    .map((entry) => ({
      exerciseId: entry.exercise.id,
      reps: Number(entry.reps),
      side: entry.side,
      holdSeconds:
        entry.exercise.hold && entry.holdSeconds !== "" ? Number(entry.holdSeconds) : null,
      workSeconds:
        entry.exercise.timed && entry.workSeconds !== "" ? Number(entry.workSeconds) : null,
      bellKg: !entry.exercise.bodyweight && entry.bellKg !== "" ? Number(entry.bellKg) : null,
    }));
}

interface LoggedSessionResponse {
  date: string;
  sets: LoggedSetInput[];
}

/**
 * Loads a Session of a Program for logging: the Program, the Exercise catalog
 * and the Logged Session, if any. Every Part lays out its planned Sets: once in a Part of straight Sets, once per Round in a Rounds Part,
 * and minute by minute in an EMOM, where a minute without Prescriptions is a
 * rest minute. A logged Session fills those rows with its logged Sets, so the
 * Lifter corrects what was saved where it was planned. Throws ApiError when a call fails, and an Error when the
 * Program has no such Session.
 */
export async function loadSessionToLog(
  programId: string,
  sessionId: string,
): Promise<SessionToLog> {
  const [program, exercises, logged] = await Promise.all([
    getJson<ProgramResponse>(`/api/programs/${encodeURIComponent(programId)}`),
    getJson<ExerciseInfo[]>("/api/exercises"),
    findLoggedSession(sessionId),
  ]);
  const place = findSession(program, sessionId);
  const exercise = (id: string): ExerciseInfo =>
    exercises.find((candidate) => candidate.id === id) ?? {
      id,
      name: "Unknown Exercise",
      bodyweight: false,
      hold: false,
      timed: false,
    };
  const planned = place.session.parts.map((part) => plannedPart(part, exercise));
  const parts = logged ? withLoggedSets(planned, logged.sets, exercise) : planned;
  return {
    programId: program.id,
    programName: program.name,
    sessionId,
    weekNumber: place.weekNumber,
    sessionNumber: place.sessionNumber,
    notes: place.session.notes,
    equipment: place.session.equipment,
    loggedDate: logged?.date ?? null,
    parts,
  };
}

function plannedPart(part: PartResponse, exercise: (id: string) => ExerciseInfo): PartToLog {
  const sets = (prescription: PrescriptionResponse, place: string | null): RowToLog[] =>
    Array.from({ length: prescription.sets }, () => ({
      kind: "set",
      set: {
        exercise: exercise(prescription.exerciseId),
        reps: prescription.reps,
        side: prescription.side,
        holdSeconds: prescription.holdSeconds,
        workSeconds: prescription.workSeconds,
        bellKg: prescription.bellKg,
        restSeconds: prescription.restSeconds,
        place,
        done: false,
      },
    }));
  const title = partTitle(part);
  switch (part.format) {
    case "SETS":
      return { title, rows: part.prescriptions.flatMap((p) => sets(p, null)) };
    case "ROUNDS":
      return {
        title,
        rows: Array.from({ length: part.rounds ?? 1 }, (_, round) =>
          part.prescriptions.flatMap((p) => sets(p, `Round ${round + 1}`)),
        ).flat(),
      };
    case "EMOM": {
      const rotation = part.rotation ?? 1;
      return {
        title,
        rows: Array.from({ length: part.minutes ?? 1 }, (_, index): RowToLog[] => {
          const place = `Minute ${index + 1}`;
          const inMinute = part.prescriptions.filter((p) => p.minute === (index % rotation) + 1);
          return inMinute.length === 0
            ? [{ kind: "rest", seconds: 60, place }]
            : inMinute.flatMap((p) => sets(p, place));
        }).flat(),
      };
    }
    case "REST":
      return {
        title,
        rows: [{ kind: "rest", seconds: part.restSeconds ?? 0, place: null }],
      };
  }
}

/**
 * Why a Bell weight typed for a Kettlebell Exercise cannot be logged, or "" when
 * it can: a Kettlebell Exercise always has a Bell above 0 kg.
 */
export function bellWeightProblem(bellKg: string): string {
  return bellKg !== "" && !(Number(bellKg) > 0) ? "A Bell weighs more than 0 kg." : "";
}

/**
 * Logs the Session as performed on `date` with the Sets the Lifter did. The
 * server checks every rule and answers with ApiError (and its reason) when one
 * is broken, or when the Session has a Logged Session already.
 */
export async function logSession(
  sessionId: string,
  date: string,
  sets: LoggedSetInput[],
): Promise<void> {
  await sendJson("POST", logPath(sessionId), { date, sets });
}

/**
 * Replaces the Logged Session of the Session with what the Lifter really did.
 * Throws ApiError, as logSession does.
 */
export async function correctLoggedSession(
  sessionId: string,
  date: string,
  sets: LoggedSetInput[],
): Promise<void> {
  await sendJson("PUT", logPath(sessionId), { date, sets });
}

function logPath(sessionId: string): string {
  return `/api/sessions/${encodeURIComponent(sessionId)}/log`;
}

/**
 * The planned Parts with the logged Sets in them: each logged Set, in order,
 * takes the first planned Set row not yet taken of the same Exercise on the
 * same Side, which it marks done with what was logged. The planned rest and
 * place stay. Logged Sets that find no such row follow in a Part of their own.
 */
function withLoggedSets(
  planned: PartToLog[],
  logged: LoggedSetInput[],
  exercise: (id: string) => ExerciseInfo,
): PartToLog[] {
  const parts = planned.map((part) => ({ ...part, rows: [...part.rows] }));
  const rows = parts.flatMap((part) =>
    part.rows.map((row, index) => ({ part, index, row })),
  );
  const extra: RowToLog[] = [];
  for (const set of logged) {
    const free = rows.find(
      ({ row }) =>
        row.kind === "set" &&
        !row.set.done &&
        row.set.exercise.id === set.exerciseId &&
        row.set.side === set.side,
    );
    const done = (base: SetToLog | null): RowToLog => ({
      kind: "set",
      set: {
        exercise: exercise(set.exerciseId),
        reps: set.reps,
        side: set.side,
        holdSeconds: set.holdSeconds,
        workSeconds: set.workSeconds,
        bellKg: set.bellKg,
        restSeconds: base?.restSeconds ?? null,
        place: base?.place ?? null,
        done: true,
      },
    });
    if (free && free.row.kind === "set") {
      free.row = done(free.row.set);
      free.part.rows[free.index] = free.row;
    } else {
      extra.push(done(null));
    }
  }
  return extra.length > 0 ? [...parts, { title: "Sets not in the plan", rows: extra }] : parts;
}

async function findLoggedSession(sessionId: string): Promise<LoggedSessionResponse | null> {
  try {
    return await getJson<LoggedSessionResponse>(logPath(sessionId));
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

function findSession(program: ProgramResponse, sessionId: string) {
  for (const week of program.weeks) {
    const index = week.sessions.findIndex((session) => session.id === sessionId);
    if (index >= 0) {
      return {
        weekNumber: week.number,
        sessionNumber: index + 1,
        session: week.sessions[index],
      };
    }
  }
  throw new Error(`${program.name} has no Session ${sessionId}`);
}
