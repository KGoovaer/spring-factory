import { render, screen, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ProgramOverviewPage } from "./ProgramOverviewPage";

const PROGRAM = { id: "p1", name: "Simple & Sinister", weeks: [] };

const STATS = {
  programId: "p1",
  weeks: [
    {
      weekId: "w1",
      number: 1,
      plannedVolume: 3600,
      loggedVolume: 2400,
      adherence: 50,
      sessions: [
        { sessionId: "s1", plannedVolume: 2400, logged: true },
        { sessionId: "s2", plannedVolume: 1200, logged: false },
      ],
    },
    {
      weekId: "w2",
      number: 2,
      plannedVolume: 0,
      loggedVolume: 0,
      adherence: null,
      sessions: [],
    },
  ],
};

/** Stubs the API at the network boundary: path → [status, body]. */
function serve(routes: Record<string, [number, unknown]>) {
  const fetch = vi.fn(async (path: string) => {
    const [status, body] = routes[path] ?? [404, { detail: `No route ${path}` }];
    return new Response(JSON.stringify(body), { status });
  });
  vi.stubGlobal("fetch", fetch);
  return fetch;
}

function renderPage(path = "/programs/p1") {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/programs/:id" element={<ProgramOverviewPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

afterEach(() => vi.unstubAllGlobals());

describe("ProgramOverviewPage", () => {
  it("shows a loading state until the Program arrives", async () => {
    serve({ "/api/programs/p1": [200, PROGRAM], "/api/programs/p1/stats": [200, STATS] });

    renderPage();

    expect(screen.getByRole("status")).toHaveTextContent("Loading Program");
    expect(await screen.findByRole("heading", { name: "Simple & Sinister" })).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("shows Weeks as rows with their Volume and Adherence", async () => {
    serve({ "/api/programs/p1": [200, PROGRAM], "/api/programs/p1/stats": [200, STATS] });

    renderPage();

    const week1 = await screen.findByRole("row", { name: "Week 1" });
    expect(within(week1).getAllByRole("cell").map((cell) => cell.textContent)).toEqual([
      "2,400 kg✓ Logged",
      "1,200 kgNot logged",
      "3,600 kg",
      "2,400 kg",
      "50%",
    ]);
    const week2 = screen.getByRole("row", { name: "Week 2" });
    expect(within(week2).getAllByRole("cell").map((cell) => cell.textContent)).toEqual([
      "",
      "",
      "0 kg",
      "0 kg",
      "–",
    ]);
    expect(screen.getAllByRole("columnheader").map((header) => header.textContent)).toEqual([
      "Week",
      "Session 1",
      "Session 2",
      "Planned Volume",
      "Logged Volume",
      "Adherence",
    ]);
  });

  it("charts planned and logged Volume per Week", async () => {
    serve({ "/api/programs/p1": [200, PROGRAM], "/api/programs/p1/stats": [200, STATS] });

    renderPage();

    const chart = await screen.findByRole("figure", { name: "Volume per Week" });
    expect(within(chart).getByText("Planned Volume")).toBeInTheDocument();
    expect(within(chart).getByText("Logged Volume")).toBeInTheDocument();
  });

  it("marks logged Sessions apart from the others", async () => {
    serve({ "/api/programs/p1": [200, PROGRAM], "/api/programs/p1/stats": [200, STATS] });

    renderPage();

    const week1 = await screen.findByRole("row", { name: "Week 1" });
    const [logged, notLogged] = within(week1).getAllByRole("cell");
    expect(within(logged).getByText("✓ Logged")).toBeInTheDocument();
    expect(within(notLogged).getByText("Not logged")).toBeInTheDocument();
    expect(within(notLogged).queryByText("✓ Logged")).not.toBeInTheDocument();
  });

  it("shows a fractional Volume as the server sent it", async () => {
    const week = { ...STATS.weeks[0], plannedVolume: 2573.5, adherence: 66.67 };
    week.sessions = [{ sessionId: "s1", plannedVolume: 173.5, logged: false }];
    serve({
      "/api/programs/p1": [200, PROGRAM],
      "/api/programs/p1/stats": [200, { programId: "p1", weeks: [week] }],
    });

    renderPage();

    const row = await screen.findByRole("row", { name: "Week 1" });
    const cells = within(row).getAllByRole("cell").map((cell) => cell.textContent);
    expect(cells).toEqual(["173.5 kgNot logged", "2,573.5 kg", "2,400 kg", "66.67%"]);
  });

  it("says so when the Program has no Weeks", async () => {
    serve({
      "/api/programs/p1": [200, PROGRAM],
      "/api/programs/p1/stats": [200, { programId: "p1", weeks: [] }],
    });

    renderPage();

    expect(await screen.findByText("This Program has no Weeks yet.")).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
    expect(screen.queryByRole("figure")).not.toBeInTheDocument();
  });

  it("shows the server's reason when the Program cannot be loaded", async () => {
    serve({
      "/api/programs/missing": [404, { status: 404, detail: "No Program with id missing" }],
      "/api/programs/missing/stats": [404, { status: 404, detail: "No Program with id missing" }],
    });

    renderPage("/programs/missing");

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Could not load this Program");
    expect(alert).toHaveTextContent("No Program with id missing");
  });

  it("asks the server for the Program and the stats of its Weeks", async () => {
    const fetch = serve({
      "/api/programs/p1": [200, PROGRAM],
      "/api/programs/p1/stats": [200, STATS],
    });

    renderPage();
    await screen.findByRole("table");

    expect(fetch.mock.calls.map(([path]) => path).sort()).toEqual([
      "/api/programs/p1",
      "/api/programs/p1/stats",
    ]);
  });
});
