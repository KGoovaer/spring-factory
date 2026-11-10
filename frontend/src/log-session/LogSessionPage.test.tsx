import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { LogSessionPage } from "./LogSessionPage";

const SWING = "e-swing";
const PLANK = "e-plank";

const EXERCISES = [
  { id: SWING, name: "Swing", bodyweight: false, hold: false },
  { id: PLANK, name: "Plank", bodyweight: true, hold: true },
];

const PROGRAM = {
  id: "p1",
  name: "Simple & Sinister",
  weeks: [
    {
      id: "w1",
      number: 1,
      sessions: [
        { id: "s0", prescriptions: [] },
        {
          id: "s1",
          prescriptions: [
            { exerciseId: SWING, sets: 2, reps: 10, holdSeconds: null, bellKg: 24, restSeconds: 60 },
            { exerciseId: PLANK, sets: 1, reps: 1, holdSeconds: 30, bellKg: null, restSeconds: null },
          ],
        },
        {
          id: "s2",
          prescriptions: [
            { exerciseId: SWING, sets: 1, reps: 10, holdSeconds: null, bellKg: 24, restSeconds: 60 },
            { exerciseId: SWING, sets: 1, reps: 5, holdSeconds: null, bellKg: 32, restSeconds: 120 },
          ],
        },
      ],
    },
  ],
};

const LOGGED = {
  sessionId: "s1",
  date: "2026-11-08",
  sets: [{ exerciseId: SWING, reps: 8, holdSeconds: null, bellKg: 20 }],
};

type Routes = Record<string, [number, unknown]>;

/** Stubs the API at the network boundary: "METHOD path" → [status, body]. */
function serve(routes: Routes) {
  const fetch = vi.fn(async (path: string, init?: RequestInit) => {
    const [status, body] = routes[`${init?.method ?? "GET"} ${path}`] ?? [
      404,
      { detail: `No route ${path}` },
    ];
    return new Response(JSON.stringify(body), { status });
  });
  vi.stubGlobal("fetch", fetch);
  return fetch;
}

function notLogged(extra: Routes = {}): Routes {
  return {
    "GET /api/programs/p1": [200, PROGRAM],
    "GET /api/exercises": [200, EXERCISES],
    "GET /api/sessions/s1/log": [404, { detail: "Session s1 has no Logged Session" }],
    ...extra,
  };
}

function renderPage(path = "/programs/p1/sessions/s1/log") {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/programs/:id/sessions/:sessionId/log" element={<LogSessionPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

function sentBody(fetch: ReturnType<typeof serve>, method: string) {
  const call = fetch.mock.calls.find(([, init]) => init?.method === method);
  return JSON.parse(String(call?.[1]?.body));
}

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe("LogSessionPage", () => {
  it("shows a loading state, then one row per planned Set", async () => {
    serve(notLogged());

    renderPage();

    expect(screen.getByRole("status")).toHaveTextContent("Loading Session");
    expect(await screen.findByRole("heading", { name: "Week 1 · Session 2" })).toBeInTheDocument();
    expect(screen.getByText("Not logged yet.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "← Simple & Sinister" })).toHaveAttribute(
      "href",
      "/programs/p1",
    );
    expect(screen.getByLabelText("Set 1 Reps")).toHaveValue(10);
    expect(screen.getByLabelText("Set 2 Bell")).toHaveValue(24);
    const plank = screen.getByRole("row", { name: "Set 3" });
    expect(within(plank).getByText("Plank")).toBeInTheDocument();
    expect(within(plank).getByText("Bodyweight")).toBeInTheDocument();
    expect(within(plank).getByLabelText("Set 3 Hold time")).toHaveValue(30);
    expect(within(plank).queryByLabelText("Set 3 Bell")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Log Session" })).toBeDisabled();
  });

  it("counts the prescribed rest down once a Set is marked done", async () => {
    serve(notLogged());
    renderPage();
    await screen.findByRole("heading", { name: "Week 1 · Session 2" });
    vi.useFakeTimers({ toFake: ["setInterval", "clearInterval", "Date"] });

    fireEvent.click(screen.getByRole("button", { name: "Set 1 done" }));

    const rest = screen.getByRole("region", { name: "Rest" });
    expect(within(rest).getByRole("timer")).toHaveTextContent("1:00");
    act(() => vi.advanceTimersByTime(15_000));
    expect(within(rest).getByRole("timer")).toHaveTextContent("0:45");
    act(() => vi.advanceTimersByTime(45_000));
    expect(within(rest).getByRole("timer")).toHaveTextContent("0:00");
    expect(within(rest).getByText("Rest is over")).toBeInTheDocument();

    fireEvent.click(within(rest).getByRole("button", { name: "Close" }));
    expect(screen.queryByRole("region", { name: "Rest" })).not.toBeInTheDocument();
  });

  it("lets the Lifter skip the rest, and starts no rest when none is prescribed", async () => {
    serve(notLogged());
    renderPage();
    await screen.findByRole("heading", { name: "Week 1 · Session 2" });

    fireEvent.click(screen.getByRole("button", { name: "Set 2 done" }));
    fireEvent.click(screen.getByRole("button", { name: "Skip rest" }));
    expect(screen.queryByRole("timer")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Set 3 done" }));
    expect(screen.queryByRole("timer")).not.toBeInTheDocument();
  });

  it("logs only the Sets marked done, with what the Lifter changed", async () => {
    const fetch = serve(notLogged({ "POST /api/sessions/s1/log": [201, LOGGED] }));
    renderPage();
    await screen.findByRole("heading", { name: "Week 1 · Session 2" });

    fireEvent.change(screen.getByLabelText("Date"), { target: { value: "2026-11-09" } });
    fireEvent.change(screen.getByLabelText("Set 1 Reps"), { target: { value: "8" } });
    fireEvent.click(screen.getByRole("button", { name: "Set 1 done" }));
    fireEvent.click(screen.getByRole("button", { name: "Set 3 done" }));
    expect(screen.getByText("2 of 3 Sets done")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Log Session" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Logged Session saved.");
    expect(sentBody(fetch, "POST")).toEqual({
      date: "2026-11-09",
      sets: [
        { exerciseId: SWING, reps: 8, holdSeconds: null, bellKg: 24 },
        { exerciseId: PLANK, reps: 1, holdSeconds: 30, bellKg: null },
      ],
    });
    expect(screen.getByRole("button", { name: "Correct Logged Session" })).toBeEnabled();

    fireEvent.change(screen.getByLabelText("Set 1 Reps"), { target: { value: "9" } });
    expect(screen.queryByText("Logged Session saved.")).not.toBeInTheDocument();
  });

  it("logs the done Sets even when a Set not done has an empty field", async () => {
    const fetch = serve(notLogged({ "POST /api/sessions/s1/log": [201, LOGGED] }));
    renderPage();
    await screen.findByRole("heading", { name: "Week 1 · Session 2" });

    fireEvent.change(screen.getByLabelText("Set 2 Reps"), { target: { value: "" } });
    fireEvent.change(screen.getByLabelText("Set 3 Hold time"), { target: { value: "" } });
    fireEvent.click(screen.getByRole("button", { name: "Set 1 done" }));
    fireEvent.click(screen.getByRole("button", { name: "Log Session" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Logged Session saved.");
    expect(sentBody(fetch, "POST").sets).toEqual([
      { exerciseId: SWING, reps: 10, holdSeconds: null, bellKg: 24 },
    ]);
  });

  it("rejects a Bell of 0 kg on a done Set before sending anything", async () => {
    const fetch = serve(notLogged({ "POST /api/sessions/s1/log": [201, LOGGED] }));
    renderPage();
    await screen.findByRole("heading", { name: "Week 1 · Session 2" });

    fireEvent.change(screen.getByLabelText("Set 2 Bell"), { target: { value: "0" } });
    fireEvent.click(screen.getByRole("button", { name: "Set 1 done" }));
    fireEvent.click(screen.getByRole("button", { name: "Log Session" }));
    expect(screen.getByLabelText("Set 2 Bell")).toBeValid();
    expect(await screen.findByRole("status")).toHaveTextContent("Logged Session saved.");

    fireEvent.click(screen.getByRole("button", { name: "Set 2 done" }));
    fireEvent.click(screen.getByRole("button", { name: "Skip rest" }));
    const bell = screen.getByLabelText("Set 2 Bell");
    expect(bell).toBeInvalid();
    expect((bell as HTMLInputElement).validationMessage).toBe("A Bell weighs more than 0 kg.");
    fireEvent.click(screen.getByRole("button", { name: "Correct Logged Session" }));
    expect(fetch.mock.calls.filter(([, init]) => init?.method === "PUT")).toHaveLength(0);
  });

  it("opens a Logged Session with its own Sets and corrects it", async () => {
    const fetch = serve(
      notLogged({
        "GET /api/sessions/s1/log": [200, LOGGED],
        "PUT /api/sessions/s1/log": [200, LOGGED],
      }),
    );
    renderPage();
    await screen.findByRole("heading", { name: "Week 1 · Session 2" });

    expect(screen.getByLabelText("Date")).toHaveValue("2026-11-08");
    expect(screen.getAllByRole("row", { name: /^Set \d+$/ })).toHaveLength(1);
    expect(screen.getByRole("button", { name: "Set 1 done" })).toHaveAttribute(
      "aria-pressed",
      "true",
    );
    fireEvent.change(screen.getByLabelText("Set 1 Bell"), { target: { value: "16" } });
    fireEvent.click(screen.getByRole("button", { name: "Correct Logged Session" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Logged Session saved.");
    expect(sentBody(fetch, "PUT")).toEqual({
      date: "2026-11-08",
      sets: [{ exerciseId: SWING, reps: 8, holdSeconds: null, bellKg: 16 }],
    });
  });

  it("keeps the rest of each planned Set when an Exercise is prescribed twice", async () => {
    serve({
      "GET /api/programs/p1": [200, PROGRAM],
      "GET /api/exercises": [200, EXERCISES],
      "GET /api/sessions/s2/log": [
        200,
        {
          sessionId: "s2",
          date: "2026-11-08",
          sets: [
            { exerciseId: SWING, reps: 10, holdSeconds: null, bellKg: 24 },
            { exerciseId: SWING, reps: 5, holdSeconds: null, bellKg: 32 },
          ],
        },
      ],
    });
    renderPage("/programs/p1/sessions/s2/log");
    await screen.findByRole("heading", { name: "Week 1 · Session 3" });

    expect(within(screen.getByRole("row", { name: "Set 1" })).getByText("60 s")).toBeInTheDocument();
    expect(within(screen.getByRole("row", { name: "Set 2" })).getByText("120 s")).toBeInTheDocument();
  });

  it("shows the server's reason when the log is rejected", async () => {
    serve(
      notLogged({
        "POST /api/sessions/s1/log": [400, { status: 400, detail: "Swing needs a Bell weight" }],
      }),
    );
    renderPage();
    await screen.findByRole("heading", { name: "Week 1 · Session 2" });

    fireEvent.click(screen.getByRole("button", { name: "Set 1 done" }));
    fireEvent.click(screen.getByRole("button", { name: "Log Session" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Could not save the Logged Session: Swing needs a Bell weight",
    );
    expect(screen.getByRole("button", { name: "Log Session" })).toBeEnabled();
  });

  it("shows an error when the Program has no such Session", async () => {
    serve({
      "GET /api/programs/p1": [200, PROGRAM],
      "GET /api/exercises": [200, EXERCISES],
      "GET /api/sessions/nope/log": [404, { detail: "No Session with id nope" }],
    });

    renderPage("/programs/p1/sessions/nope/log");

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Could not load this Session");
    expect(alert).toHaveTextContent("Simple & Sinister has no Session nope");
  });

  it("shows the server's reason when the Program cannot be loaded", async () => {
    serve({ "GET /api/programs/p1": [404, { detail: "No Program with id p1" }] });

    renderPage();

    expect(await screen.findByRole("alert")).toHaveTextContent("No Program with id p1");
  });
});
