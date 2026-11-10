import { ApiError, getJson, sendJson } from "../api/client";

/** An Exercise as the form needs it: whether its Sets take a Bell and a Hold time. */
export interface ExerciseInfo {
  id: string;
  name: string;
  bodyweight: boolean;
  hold: boolean;
}

/**
 * One Set row on the form: what was planned or logged, as the starting values
 * the Lifter edits. `restSeconds` is the rest the Prescription asks for after
 * this Set, or null when none was prescribed.
 */
export interface SetToLog {
  exercise: ExerciseInfo;
  reps: number;
  holdSeconds: number | null;
  bellKg: number | null;
  restSeconds: number | null;
}

/** A Session opened for logging: where it sits in its Program, and its Set rows. */
export interface SessionToLog {
  programId: string;
  programName: string;
  sessionId: string;
  weekNumber: number;
  sessionNumber: number;
  /** The date of the Logged Session, or null when the Session was not logged yet. */
  loggedDate: string | null;
  /** The logged Sets when there is a Logged Session, else one row per planned Set. */
  sets: SetToLog[];
}

/** One Set the Lifter really did, as the API takes it. */
export interface LoggedSetInput {
  exerciseId: string;
  reps: number;
  holdSeconds: number | null;
  bellKg: number | null;
}

interface PrescriptionResponse {
  exerciseId: string;
  sets: number;
  reps: number;
  holdSeconds: number | null;
  bellKg: number | null;
  restSeconds: number | null;
}

interface ProgramResponse {
  id: string;
  name: string;
  weeks: {
    number: number;
    sessions: { id: string; prescriptions: PrescriptionResponse[] }[];
  }[];
}

interface LoggedSessionResponse {
  date: string;
  sets: { exerciseId: string; reps: number; holdSeconds: number | null; bellKg: number | null }[];
}

/**
 * Loads a Session of a Program for logging: the Program, the Exercise catalog
 * and the Logged Session, if any. A logged Session starts from its logged
 * Sets, so the Lifter corrects what was saved; otherwise every planned Set
 * becomes a row. Throws ApiError when a call fails or the Program has no such
 * Session.
 */
export async function loadSessionToLog(programId: string, sessionId: string): Promise<SessionToLog> {
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
    };
  const restAfter = new Map(
    place.prescriptions.map((prescription) => [prescription.exerciseId, prescription.restSeconds]),
  );
  const sets: SetToLog[] = logged
    ? logged.sets.map((set) => ({
        exercise: exercise(set.exerciseId),
        reps: set.reps,
        holdSeconds: set.holdSeconds,
        bellKg: set.bellKg,
        restSeconds: restAfter.get(set.exerciseId) ?? null,
      }))
    : place.prescriptions.flatMap((prescription) =>
        Array.from({ length: prescription.sets }, () => ({
          exercise: exercise(prescription.exerciseId),
          reps: prescription.reps,
          holdSeconds: prescription.holdSeconds,
          bellKg: prescription.bellKg,
          restSeconds: prescription.restSeconds,
        })),
      );
  return {
    programId: program.id,
    programName: program.name,
    sessionId,
    weekNumber: place.weekNumber,
    sessionNumber: place.sessionNumber,
    loggedDate: logged?.date ?? null,
    sets,
  };
}

/**
 * Saves the Sets the Lifter did on `date`: logs the Session, or corrects its
 * Logged Session when it has one already. The server checks every rule and
 * answers with ApiError (and its reason) when one is broken.
 */
export async function saveLoggedSession(
  sessionId: string,
  date: string,
  sets: LoggedSetInput[],
  alreadyLogged: boolean,
): Promise<void> {
  await sendJson(alreadyLogged ? "PUT" : "POST", `/api/sessions/${encodeURIComponent(sessionId)}/log`, {
    date,
    sets,
  });
}

async function findLoggedSession(sessionId: string): Promise<LoggedSessionResponse | null> {
  try {
    return await getJson<LoggedSessionResponse>(
      `/api/sessions/${encodeURIComponent(sessionId)}/log`,
    );
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
        prescriptions: week.sessions[index].prescriptions,
      };
    }
  }
  throw new ApiError(404, `${program.name} has no Session ${sessionId}`);
}
