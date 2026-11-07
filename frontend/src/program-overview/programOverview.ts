import { getJson } from "../api/client";

/** One planned Session in the grid: its planned Volume and whether it has a Logged Session. */
export interface SessionCell {
  id: string;
  plannedVolume: number;
  logged: boolean;
}

/** One Week row: its Volume, its Adherence (a percentage, null without Sessions) and its Sessions. */
export interface WeekRow {
  id: string;
  number: number;
  plannedVolume: number;
  loggedVolume: number;
  adherence: number | null;
  sessions: SessionCell[];
}

/** A Program as the overview page shows it: Weeks as rows, Sessions as cells. */
export interface ProgramOverview {
  id: string;
  name: string;
  weeks: WeekRow[];
}

interface ProgramResponse {
  id: string;
  name: string;
}

interface ProgramStatsResponse {
  programId: string;
  weeks: {
    weekId: string;
    number: number;
    plannedVolume: number;
    loggedVolume: number;
    adherence: number | null;
    sessions: { sessionId: string; plannedVolume: number; logged: boolean }[];
  }[];
}

/**
 * Loads the Program and the stats of its Weeks. Volume and Adherence come
 * from the server; the client derives nothing. Throws ApiError when either
 * call fails.
 */
export async function loadProgramOverview(programId: string): Promise<ProgramOverview> {
  const id = encodeURIComponent(programId);
  const [program, stats] = await Promise.all([
    getJson<ProgramResponse>(`/api/programs/${id}`),
    getJson<ProgramStatsResponse>(`/api/programs/${id}/stats`),
  ]);
  return {
    id: program.id,
    name: program.name,
    weeks: stats.weeks.map((week) => ({
      id: week.weekId,
      number: week.number,
      plannedVolume: week.plannedVolume,
      loggedVolume: week.loggedVolume,
      adherence: week.adherence,
      sessions: week.sessions.map((session) => ({
        id: session.sessionId,
        plannedVolume: session.plannedVolume,
        logged: session.logged,
      })),
    })),
  };
}
