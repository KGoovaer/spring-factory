import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { LogSessionPage } from "./LogSessionPage";

const SWING = "e-swing";
const PLANK = "e-plank";

const PUSH_UP = "e-push-up";
const SKIPPING = "e-skipping";
const PRESS = "e-press";

const EXERCISES = [
  { id: SWING, name: "Swing", bodyweight: false, hold: false, timed: false },
  { id: PLANK, name: "Plank", bodyweight: true, hold: true, timed: false },
  { id: PUSH_UP, name: "Push-up", bodyweight: true, hold: false, timed: false },
  {
    id: SKIPPING,
    name: "Skipping",
    bodyweight: true,
    hold: false,
    timed: true,
  },
  {
    id: PRESS,
    name: "Clean to waiter press",
    bodyweight: false,
    hold: false,
    timed: false,
  },
];

/** A Prescription as the API answers it, with the fields a test leaves out set to null. */
function prescription(fields: Record<string, unknown>) {
  return {
    sets: 1,
    repsMax: null,
    side: null,
    holdSeconds: null,
    workSeconds: null,
    bellKg: null,
    restSeconds: null,
    minute: null,
    ...fields,
  };
}

/** A Session as the API answers it, with its Prescriptions in one Part of straight Sets. */
function session(id: string, prescriptions: Record<string, unknown>[]) {
  return {
    id,
    notes: null,
    equipment: [],
    parts: prescriptions.length
      ? [
          {
            number: 1,
            format: "SETS",
            rounds: null,
            minutes: null,
            rotation: null,
            restSeconds: null,
            prescriptions: prescriptions.map(prescription),
          },
        ]
      : [],
  };
}

function part(fields: Record<string, unknown>) {
  return {
    rounds: null,
    minutes: null,
    rotation: null,
    restSeconds: null,
    prescriptions: [],
    ...fields,
  };
}

/** The workout of issue #20: a 20 min EMOM, 60 s rest, then 3 Rounds. */
const EMOM_SESSION = {
  id: "s3",
  notes: "Keep intensity at 70-80%.",
  equipment: ["Body weight", "Jump rope", "Kettlebell"],
  parts: [
    part({
      number: 1,
      format: "EMOM",
      minutes: 20,
      rotation: 4,
      prescriptions: [
        prescription({
          exerciseId: SWING,
          reps: 5,
          side: "RIGHT",
          bellKg: 16,
          minute: 1,
        }),
        prescription({ exerciseId: PUSH_UP, reps: 5, minute: 1 }),
        prescription({
          exerciseId: SWING,
          reps: 5,
          side: "LEFT",
          bellKg: 16,
          minute: 2,
        }),
        prescription({
          exerciseId: SKIPPING,
          reps: 1,
          workSeconds: 45,
          minute: 3,
        }),
      ],
    }),
    part({ number: 2, format: "REST", restSeconds: 60 }),
    part({
      number: 3,
      format: "ROUNDS",
      rounds: 3,
      prescriptions: [prescription({ exerciseId: PRESS, reps: 8, side: "EACH", bellKg: 12 })],
    }),
  ],
};

/** A 12 min AMRAP of 3-4+3-4 presses and 10 swings, after a Part of 3-5 swings. */
const AMRAP_SESSION = {
  id: "s5",
  notes: null,
  equipment: ["Kettlebell"],
  parts: [
    part({
      number: 1,
      format: "SETS",
      prescriptions: [prescription({ exerciseId: SWING, reps: 3, repsMax: 5, bellKg: 24 })],
    }),
    part({
      number: 2,
      format: "AMRAP",
      minutes: 12,
      prescriptions: [
        prescription({
          exerciseId: PRESS,
          reps: 3,
          repsMax: 4,
          side: "EACH",
          bellKg: 12,
        }),
        prescription({ exerciseId: SWING, reps: 10, bellKg: 16 }),
      ],
    }),
  ],
};

const PROGRAM = {
  id: "p1",
  name: "Simple & Sinister",
  weeks: [
    {
      id: "w1",
      number: 1,
      sessions: [
        session("s0", []),
        session("s1", [
          { exerciseId: SWING, sets: 2, reps: 10, bellKg: 24, restSeconds: 60 },
          { exerciseId: PLANK, sets: 1, reps: 1, holdSeconds: 30 },
        ]),
        session("s2", [
          { exerciseId: SWING, sets: 1, reps: 10, bellKg: 24, restSeconds: 60 },
          { exerciseId: SWING, sets: 1, reps: 5, bellKg: 32, restSeconds: 120 },
        ]),
        EMOM_SESSION,
        session("s4", [
          { exerciseId: SWING, reps: 8, side: "LEFT", bellKg: 16, restSeconds: 30 },
          { exerciseId: SWING, reps: 8, side: "RIGHT", bellKg: 16, restSeconds: 90 },
        ]),
        AMRAP_SESSION,
      ],
    },
  ],
};

const LOGGED = {
  sessionId: "s1",
  date: "2026-11-08",
  sets: [
    {
      exerciseId: SWING,
      reps: 8,
      side: null,
      holdSeconds: null,
      workSeconds: null,
      bellKg: 20,
    },
  ],
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

  it("counts a hold of a Hold Exercise down from its Hold time", async () => {
    serve(notLogged());
    renderPage();
    await screen.findByRole("heading", { name: "Week 1 · Session 2" });
    vi.useFakeTimers({ toFake: ["setInterval", "clearInterval", "Date"] });
    const plank = screen.getByRole("row", { name: "Set 3" });

    fireEvent.change(within(plank).getByLabelText("Set 3 Hold time"), {
      target: { value: "" },
    });
    expect(within(plank).getByRole("button", { name: "Set 3 Start hold" })).toBeDisabled();
    fireEvent.change(within(plank).getByLabelText("Set 3 Hold time"), {
      target: { value: "20" },
    });
    fireEvent.click(within(plank).getByRole("button", { name: "Set 3 Start hold" }));

    const hold = within(plank).getByRole("timer", { name: "Set 3 hold" });
    expect(hold).toHaveTextContent("0:20");
    act(() => vi.advanceTimersByTime(5_000));
    expect(hold).toHaveTextContent("0:15");
    act(() => vi.advanceTimersByTime(15_000));
    expect(hold).toHaveTextContent("Hold is over");

    fireEvent.click(within(plank).getByRole("button", { name: "Set 3 Start hold" }));
    expect(hold).toHaveTextContent("0:20");
    fireEvent.click(within(plank).getByRole("button", { name: "Set 3 Stop hold" }));
    expect(within(plank).queryByRole("timer")).not.toBeInTheDocument();
  });

  it("logs only the Sets marked done, with what the Lifter changed", async () => {
    const fetch = serve(notLogged({ "POST /api/sessions/s1/log": [201, LOGGED] }));
    renderPage();
    await screen.findByRole("heading", { name: "Week 1 · Session 2" });

    fireEvent.change(screen.getByLabelText("Date"), {
      target: { value: "2026-11-09" },
    });
    fireEvent.change(screen.getByLabelText("Set 1 Reps"), {
      target: { value: "8" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Set 1 done" }));
    fireEvent.click(screen.getByRole("button", { name: "Set 3 done" }));
    expect(screen.getByText("2 of 3 Sets done")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Log Session" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Logged Session saved.");
    expect(sentBody(fetch, "POST")).toEqual({
      date: "2026-11-09",
      sets: [
        {
          exerciseId: SWING,
          reps: 8,
          side: null,
          holdSeconds: null,
          workSeconds: null,
          bellKg: 24,
        },
        {
          exerciseId: PLANK,
          reps: 1,
          side: null,
          holdSeconds: 30,
          workSeconds: null,
          bellKg: null,
        },
      ],
      scores: [],
    });
    expect(screen.getByRole("button", { name: "Correct Logged Session" })).toBeEnabled();

    fireEvent.change(screen.getByLabelText("Set 1 Reps"), {
      target: { value: "9" },
    });
    expect(screen.queryByText("Logged Session saved.")).not.toBeInTheDocument();
  });

  it("logs the done Sets even when a Set not done has an empty field", async () => {
    const fetch = serve(notLogged({ "POST /api/sessions/s1/log": [201, LOGGED] }));
    renderPage();
    await screen.findByRole("heading", { name: "Week 1 · Session 2" });

    fireEvent.change(screen.getByLabelText("Set 2 Reps"), {
      target: { value: "" },
    });
    fireEvent.change(screen.getByLabelText("Set 3 Hold time"), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Set 1 done" }));
    fireEvent.click(screen.getByRole("button", { name: "Log Session" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Logged Session saved.");
    expect(sentBody(fetch, "POST").sets).toEqual([
      {
        exerciseId: SWING,
        reps: 10,
        side: null,
        holdSeconds: null,
        workSeconds: null,
        bellKg: 24,
      },
    ]);
  });

  it("rejects a Bell of 0 kg on a done Set before sending anything", async () => {
    const fetch = serve(notLogged({ "POST /api/sessions/s1/log": [201, LOGGED] }));
    renderPage();
    await screen.findByRole("heading", { name: "Week 1 · Session 2" });

    fireEvent.change(screen.getByLabelText("Set 2 Bell"), {
      target: { value: "0" },
    });
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

  it("opens a Logged Session with its Sets done in their planned rows and corrects it", async () => {
    const fetch = serve(
      notLogged({
        "GET /api/sessions/s1/log": [200, LOGGED],
        "PUT /api/sessions/s1/log": [200, LOGGED],
      }),
    );
    renderPage();
    await screen.findByRole("heading", { name: "Week 1 · Session 2" });

    expect(screen.getByLabelText("Date")).toHaveValue("2026-11-08");
    expect(screen.getAllByRole("row", { name: /^Set \d+$/ })).toHaveLength(3);
    expect(screen.getByLabelText("Set 1 Reps")).toHaveValue(8);
    expect(screen.getByRole("button", { name: "Set 1 done" })).toHaveAttribute(
      "aria-pressed",
      "true",
    );
    expect(screen.getByRole("button", { name: "Set 2 done" })).toHaveAttribute(
      "aria-pressed",
      "false",
    );
    expect(screen.getByText("1 of 3 Sets done")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Set 1 Bell"), {
      target: { value: "16" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Correct Logged Session" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Logged Session saved.");
    expect(sentBody(fetch, "PUT")).toEqual({
      date: "2026-11-08",
      sets: [
        {
          exerciseId: SWING,
          reps: 8,
          side: null,
          holdSeconds: null,
          workSeconds: null,
          bellKg: 16,
        },
      ],
      scores: [],
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
            {
              exerciseId: SWING,
              reps: 10,
              side: null,
              holdSeconds: null,
              workSeconds: null,
              bellKg: 24,
            },
            {
              exerciseId: SWING,
              reps: 5,
              side: null,
              holdSeconds: null,
              workSeconds: null,
              bellKg: 32,
            },
          ],
        },
      ],
    });
    renderPage("/programs/p1/sessions/s2/log");
    await screen.findByRole("heading", { name: "Week 1 · Session 3" });

    expect(
      within(screen.getByRole("row", { name: "Set 1" })).getByText("60 s"),
    ).toBeInTheDocument();
    expect(
      within(screen.getByRole("row", { name: "Set 2" })).getByText("120 s"),
    ).toBeInTheDocument();
  });

  it("puts each logged Set in the planned row of its Side", async () => {
    const swing = (side: string) => ({
      exerciseId: SWING,
      reps: 8,
      side,
      holdSeconds: null,
      workSeconds: null,
      bellKg: 16,
    });
    serve({
      "GET /api/programs/p1": [200, PROGRAM],
      "GET /api/exercises": [200, EXERCISES],
      "GET /api/sessions/s4/log": [
        200,
        { sessionId: "s4", date: "2026-11-08", sets: [swing("RIGHT"), swing("LEFT")] },
      ],
    });
    renderPage("/programs/p1/sessions/s4/log");
    await screen.findByRole("heading", { name: "Week 1 · Session 5" });

    // Planned Left, then Right: each logged Set fills the row of its Side.
    const left = screen.getByRole("row", { name: "Set 1" });
    expect(within(left).getByText("Left")).toBeInTheDocument();
    expect(within(left).getByText("30 s")).toBeInTheDocument();
    const right = screen.getByRole("row", { name: "Set 2" });
    expect(within(right).getByText("Right")).toBeInTheDocument();
    expect(within(right).getByText("90 s")).toBeInTheDocument();
    expect(screen.getByText("2 of 2 Sets done")).toBeInTheDocument();
  });

  it("lays out an EMOM minute by minute, the rest between Parts and every Round", async () => {
    serve({
      "GET /api/programs/p1": [200, PROGRAM],
      "GET /api/exercises": [200, EXERCISES],
      "GET /api/sessions/s3/log": [404, { detail: "Session s3 has no Logged Session" }],
    });
    renderPage("/programs/p1/sessions/s3/log");
    await screen.findByRole("heading", { name: "Week 1 · Session 4" });

    expect(screen.getByText("Body weight, Jump rope, Kettlebell")).toBeInTheDocument();
    expect(screen.getByRole("region", { name: "Notes" })).toHaveTextContent(
      "Keep intensity at 70-80%.",
    );
    const emom = screen.getByRole("rowgroup", { name: "Part 1 · 20 min EMOM, rotating through 4 minutes" });
    // 5 passes of: minute 1 (2 Sets), minute 2 (1 Set), minute 3 (1 Set), minute 4 (rest).
    expect(within(emom).getAllByRole("row", { name: /^Set \d+$/ })).toHaveLength(20);
    expect(within(emom).getAllByText("Rest 60 s")).toHaveLength(5);
    const first = screen.getByRole("row", { name: "Set 1" });
    expect(within(first).getByText("Minute 1")).toBeInTheDocument();
    expect(within(first).getByText("Right")).toBeInTheDocument();
    expect(
      within(screen.getByRole("row", { name: "Set 3" })).getByText("Left"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Set 4 Work time")).toHaveValue(45);

    const rest = screen.getByRole("rowgroup", { name: "Part 2 · Rest 60 s" });
    fireEvent.click(within(rest).getByRole("button", { name: "Start rest" }));
    expect(
      within(screen.getByRole("region", { name: "Rest" })).getByRole("timer"),
    ).toHaveTextContent("1:00");

    const rounds = screen.getByRole("rowgroup", { name: "Part 3 · 3 Rounds" });
    expect(within(rounds).getAllByText("8+8")).toHaveLength(3);
    expect(within(rounds).getByText("Round 3")).toBeInTheDocument();
    expect(screen.getByText("0 of 23 Sets done")).toBeInTheDocument();
  });

  it("opens a logged EMOM in its Parts, minutes, rest and Rounds", async () => {
    const set = (exerciseId: string, fields: Record<string, unknown>) => ({
      exerciseId,
      side: null,
      holdSeconds: null,
      workSeconds: null,
      bellKg: null,
      ...fields,
    });
    serve({
      "GET /api/programs/p1": [200, PROGRAM],
      "GET /api/exercises": [200, EXERCISES],
      "GET /api/sessions/s3/log": [
        200,
        {
          sessionId: "s3",
          date: "2026-11-08",
          sets: [
            set(SWING, { reps: 5, side: "RIGHT", bellKg: 20 }),
            set(PUSH_UP, { reps: 4 }),
            set(PRESS, { reps: 8, side: "EACH", bellKg: 12 }),
            set(PLANK, { reps: 1, holdSeconds: 30 }),
          ],
        },
      ],
    });
    renderPage("/programs/p1/sessions/s3/log");
    await screen.findByRole("heading", { name: "Week 1 · Session 4" });

    const emom = screen.getByRole("rowgroup", {
      name: "Part 1 · 20 min EMOM, rotating through 4 minutes",
    });
    expect(within(emom).getAllByText("Rest 60 s")).toHaveLength(5);
    const first = screen.getByRole("row", { name: "Set 1" });
    expect(within(first).getByText("Minute 1")).toBeInTheDocument();
    expect(screen.getByLabelText("Set 1 Bell")).toHaveValue(20);
    expect(screen.getByLabelText("Set 2 Reps")).toHaveValue(4);
    expect(screen.getByRole("button", { name: "Set 2 done" })).toHaveAttribute(
      "aria-pressed",
      "true",
    );
    expect(screen.getByRole("button", { name: "Set 5 done" })).toHaveAttribute(
      "aria-pressed",
      "false",
    );
    expect(screen.getByRole("rowgroup", { name: "Part 2 · Rest 60 s" })).toBeInTheDocument();
    const rounds = screen.getByRole("rowgroup", { name: "Part 3 · 3 Rounds" });
    expect(within(within(rounds).getAllByRole("row", { name: /^Set \d+$/ })[0]).getByText(
      "Round 1",
    )).toBeInTheDocument();
    const extra = screen.getByRole("rowgroup", { name: "Sets not in the plan" });
    expect(within(extra).getByText("Plank")).toBeInTheDocument();
    expect(screen.getByText("4 of 24 Sets done")).toBeInTheDocument();
  });

  it("logs a Timed Exercise with its Work time and a Set on Each side", async () => {
    const fetch = serve({
      "GET /api/programs/p1": [200, PROGRAM],
      "GET /api/exercises": [200, EXERCISES],
      "GET /api/sessions/s3/log": [404, { detail: "Session s3 has no Logged Session" }],
      "POST /api/sessions/s3/log": [201, LOGGED],
    });
    renderPage("/programs/p1/sessions/s3/log");
    await screen.findByRole("heading", { name: "Week 1 · Session 4" });

    fireEvent.change(screen.getByLabelText("Set 4 Work time"), {
      target: { value: "40" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Set 4 done" }));
    fireEvent.click(screen.getByRole("button", { name: "Set 21 done" }));
    fireEvent.click(screen.getByRole("button", { name: "Log Session" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Logged Session saved.");
    expect(sentBody(fetch, "POST").sets).toEqual([
      {
        exerciseId: SKIPPING,
        reps: 1,
        side: null,
        holdSeconds: null,
        workSeconds: 40,
        bellKg: null,
      },
      {
        exerciseId: PRESS,
        reps: 8,
        side: "EACH",
        holdSeconds: null,
        workSeconds: null,
        bellKg: 12,
      },
    ]);
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

  it("keeps the Lifter's Sets and switches to correction mode when the Session was logged elsewhere", async () => {
    const routes = notLogged({
      "POST /api/sessions/s1/log": [
        409,
        { status: 409, detail: "This Session has a Logged Session already" },
      ],
      "PUT /api/sessions/s1/log": [200, LOGGED],
    });
    const fetch = serve(routes);
    renderPage();
    await screen.findByRole("heading", { name: "Week 1 · Session 2" });
    expect(screen.getByText("Not logged yet.")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Set 1 done" }));
    fireEvent.change(screen.getByLabelText("Set 1 Bell"), { target: { value: "28" } });
    // Another tab logs the Session before this form saves.
    routes["GET /api/sessions/s1/log"] = [200, LOGGED];
    fireEvent.click(screen.getByRole("button", { name: "Log Session" }));

    const notice = await screen.findByRole("alert");
    expect(notice).toHaveTextContent("This Session was logged elsewhere on 2026-11-08 (1 Set).");
    expect(notice).toHaveTextContent(
      "Your Sets below are kept. Saving now overwrites that Logged Session.",
    );
    expect(notice).not.toHaveTextContent("s1");
    expect(screen.getByLabelText("Set 1 Bell")).toHaveValue(28);
    expect(screen.getByRole("button", { name: "Set 1 done" })).toHaveAttribute(
      "aria-pressed",
      "true",
    );
    expect(fetch.mock.calls.filter(([, init]) => init?.method === "PUT")).toHaveLength(0);

    fireEvent.click(screen.getByRole("button", { name: "Overwrite Logged Session" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Logged Session saved.");
    expect(sentBody(fetch, "PUT")).toEqual(sentBody(fetch, "POST"));
    expect(sentBody(fetch, "PUT").sets[0].bellKg).toBe(28);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Correct Logged Session" })).toBeEnabled();
  });

  it("still switches to correction mode when the Logged Session made elsewhere cannot be loaded", async () => {
    const routes = notLogged({
      "POST /api/sessions/s1/log": [409, { status: 409, detail: "Logged already" }],
      "PUT /api/sessions/s1/log": [200, LOGGED],
    });
    const fetch = serve(routes);
    renderPage();
    await screen.findByRole("heading", { name: "Week 1 · Session 2" });

    fireEvent.click(screen.getByRole("button", { name: "Set 1 done" }));
    routes["GET /api/sessions/s1/log"] = [500, { detail: "Server error" }];
    fireEvent.click(screen.getByRole("button", { name: "Log Session" }));

    const notice = await screen.findByRole("alert");
    expect(notice).toHaveTextContent("This Session was logged elsewhere.");
    expect(notice).toHaveTextContent("Saving now overwrites that Logged Session.");
    fireEvent.click(screen.getByRole("button", { name: "Overwrite Logged Session" }));
    expect(await screen.findByRole("status")).toHaveTextContent("Logged Session saved.");
    expect(sentBody(fetch, "PUT").sets).toHaveLength(1);
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
    serve({
      "GET /api/programs/p1": [404, { detail: "No Program with id p1" }],
    });

    renderPage();

    expect(await screen.findByRole("alert")).toHaveTextContent("No Program with id p1");
  });

  it("runs the clock of an EMOM minute by minute", async () => {
    serve({
      "GET /api/programs/p1": [200, PROGRAM],
      "GET /api/exercises": [200, EXERCISES],
      "GET /api/sessions/s3/log": [404, { detail: "Session s3 has no Logged Session" }],
    });
    renderPage("/programs/p1/sessions/s3/log");
    await screen.findByRole("heading", { name: "Week 1 · Session 4" });
    vi.useFakeTimers({ toFake: ["setInterval", "clearInterval", "Date"] });
    const emom = screen.getByRole("rowgroup", {
      name: /^Part 1 · 20 min EMOM/,
    });

    fireEvent.click(within(emom).getByRole("button", { name: "Start EMOM" }));
    const clock = within(emom).getByRole("timer");
    expect(clock).toHaveTextContent("Minute 1 of 20 · 1:00");
    act(() => vi.advanceTimersByTime(75_000));
    expect(clock).toHaveTextContent("Minute 2 of 20 · 0:45");
    act(() => vi.advanceTimersByTime(20 * 60_000));
    expect(clock).toHaveTextContent("EMOM is over");

    fireEvent.click(within(emom).getByRole("button", { name: "Start EMOM" }));
    fireEvent.click(within(emom).getByRole("button", { name: "Stop EMOM" }));
    expect(within(emom).queryByRole("timer")).not.toBeInTheDocument();
  });

  it("counts an AMRAP down from its time cap and logs it as a Score", async () => {
    const fetch = serve({
      "GET /api/programs/p1": [200, PROGRAM],
      "GET /api/exercises": [200, EXERCISES],
      "GET /api/sessions/s5/log": [404, { detail: "Session s5 has no Logged Session" }],
      "POST /api/sessions/s5/log": [201, {}],
    });
    renderPage("/programs/p1/sessions/s5/log");
    await screen.findByRole("heading", { name: "Week 1 · Session 6" });
    const amrap = screen.getByRole("rowgroup", {
      name: "Part 2 · 12 min AMRAP",
    });

    expect(
      within(amrap).getByText(/3-4\+3-4 Clean to waiter press · 12 kg; 10 Swing · 16 kg/),
    ).toBeInTheDocument();
    expect(within(amrap).queryByRole("row", { name: /^Set/ })).not.toBeInTheDocument();
    expect(
      within(screen.getByRole("row", { name: "Set 1" })).getByText("of 3-5"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Set 1 Reps")).toHaveValue(3);

    vi.useFakeTimers({ toFake: ["setInterval", "clearInterval", "Date"] });
    fireEvent.click(within(amrap).getByRole("button", { name: "Start AMRAP" }));
    expect(within(amrap).getByRole("timer")).toHaveTextContent("12:00 left");
    act(() => vi.advanceTimersByTime(90_000));
    expect(within(amrap).getByRole("timer")).toHaveTextContent("10:30 left");
    act(() => vi.advanceTimersByTime(11 * 60_000));
    expect(within(amrap).getByRole("timer")).toHaveTextContent("AMRAP is over");
    vi.useRealTimers();

    expect(screen.getByRole("button", { name: "Log Session" })).toBeDisabled();
    fireEvent.change(screen.getByLabelText("Part 2 Rounds"), {
      target: { value: "5" },
    });
    fireEvent.change(screen.getByLabelText("Part 2 extra Reps"), {
      target: { value: "7" },
    });
    expect(screen.getByText("0 of 1 Sets done, 1 of 1 AMRAPs scored")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Log Session" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Logged Session saved.");
    expect(sentBody(fetch, "POST")).toMatchObject({
      sets: [],
      scores: [{ part: 2, rounds: 5, extraReps: 7 }],
    });
  });

  it("opens a logged AMRAP with its Score", async () => {
    serve({
      "GET /api/programs/p1": [200, PROGRAM],
      "GET /api/exercises": [200, EXERCISES],
      "GET /api/sessions/s5/log": [
        200,
        {
          sessionId: "s5",
          date: "2026-11-10",
          sets: [],
          scores: [{ part: 2, rounds: 6, extraReps: 0 }],
        },
      ],
    });
    renderPage("/programs/p1/sessions/s5/log");
    await screen.findByRole("heading", { name: "Week 1 · Session 6" });

    expect(screen.getByLabelText("Part 2 Rounds")).toHaveValue(6);
    expect(screen.getByLabelText("Part 2 extra Reps")).toHaveValue(0);
    expect(screen.getByRole("button", { name: "Correct Logged Session" })).toBeEnabled();
  });
});
