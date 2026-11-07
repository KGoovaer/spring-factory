import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { formatVolume } from "./format";
import type { WeekRow } from "./programOverview";

const PLANNED = "#2a78d6";
const LOGGED = "#eb6834";

/**
 * Planned and logged Volume per Week as grouped bars. The grid below holds
 * the same numbers, so the chart adds the trend and the grid stays the table view.
 */
export function WeekVolumeChart({ weeks }: { weeks: WeekRow[] }) {
  const data = weeks.map((week) => ({
    week: `Week ${week.number}`,
    planned: week.plannedVolume,
    logged: week.loggedVolume,
  }));
  return (
    <figure className="week-volume-chart" aria-labelledby="week-volume-chart-title">
      <figcaption id="week-volume-chart-title">Volume per Week</figcaption>
      <ResponsiveContainer width="100%" height={260} initialDimension={{ width: 600, height: 260 }}>
        <BarChart data={data} barGap={2} margin={{ top: 8, right: 8, bottom: 0, left: 8 }}>
          <CartesianGrid vertical={false} stroke="#e5e7eb" />
          <XAxis dataKey="week" tickLine={false} axisLine={{ stroke: "#9aa4b1" }} />
          <YAxis tickFormatter={formatVolume} tickLine={false} axisLine={false} width={96} />
          <Tooltip
            formatter={(value) => formatVolume(Number(value))}
            cursor={{ fill: "#eef0f3" }}
          />
          <Legend
            itemSorter={null}
            formatter={(name) => <span className="legend-label">{name}</span>}
          />
          <Bar
            dataKey="planned"
            name="Planned Volume"
            fill={PLANNED}
            radius={[4, 4, 0, 0]}
            maxBarSize={32}
            isAnimationActive={false}
          />
          <Bar
            dataKey="logged"
            name="Logged Volume"
            fill={LOGGED}
            radius={[4, 4, 0, 0]}
            maxBarSize={32}
            isAnimationActive={false}
          />
        </BarChart>
      </ResponsiveContainer>
    </figure>
  );
}
