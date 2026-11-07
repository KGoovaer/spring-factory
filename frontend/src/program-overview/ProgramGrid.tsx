import { formatAdherence, formatVolume } from "./format";
import type { ProgramOverview, SessionCell, WeekRow } from "./programOverview";

/** Weeks as rows, Sessions as cells, with each Week's Volume and Adherence. */
export function ProgramGrid({ program }: { program: ProgramOverview }) {
  const sessionColumns = Math.max(0, ...program.weeks.map((week) => week.sessions.length));
  return (
    <section>
      <h1>{program.name}</h1>
      {program.weeks.length === 0 ? (
        <p>This Program has no Weeks yet.</p>
      ) : (
        <table className="program-grid">
          <thead>
            <tr>
              <th scope="col">Week</th>
              {Array.from({ length: sessionColumns }, (_, i) => (
                <th scope="col" key={i}>
                  Session {i + 1}
                </th>
              ))}
              <th scope="col">Planned Volume</th>
              <th scope="col">Logged Volume</th>
              <th scope="col">Adherence</th>
            </tr>
          </thead>
          <tbody>
            {program.weeks.map((week) => (
              <WeekRowView key={week.id} week={week} sessionColumns={sessionColumns} />
            ))}
          </tbody>
        </table>
      )}
      <p className="legend">
        <span className="session logged">✓ Logged</span> Session has a Logged Session
      </p>
    </section>
  );
}

function WeekRowView({ week, sessionColumns }: { week: WeekRow; sessionColumns: number }) {
  return (
    <tr aria-label={`Week ${week.number}`}>
      <th scope="row">Week {week.number}</th>
      {Array.from({ length: sessionColumns }, (_, i) => (
        <td key={i}>{week.sessions[i] && <SessionCellView session={week.sessions[i]} />}</td>
      ))}
      <td className="number">{formatVolume(week.plannedVolume)}</td>
      <td className="number">{formatVolume(week.loggedVolume)}</td>
      <td className="number">{formatAdherence(week.adherence)}</td>
    </tr>
  );
}

function SessionCellView({ session }: { session: SessionCell }) {
  return (
    <div className={session.logged ? "session logged" : "session"}>
      <span className="volume">{formatVolume(session.plannedVolume)}</span>
      <span className="state">{session.logged ? "✓ Logged" : "Not logged"}</span>
    </div>
  );
}
