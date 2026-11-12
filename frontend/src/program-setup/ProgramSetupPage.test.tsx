import { fireEvent, render, screen, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { HomePage } from "./HomePage";
import { ProgramSetupPage } from "./ProgramSetupPage";

const SWING = "e-swing";
const SKIPPING = "e-skipping";

const EXERCISES = [
  {
    id: SWING,
    name: "Swing",
    bodyweight: false,
    hold: false,
    timed: false,
    gear: null,
    equipment: "Kettlebell",
  },
  {
    id: SKIPPING,
    name: "Skipping",
    bodyweight: true,
    hold: false,
    timed: true,
    gear: "Jump rope",
    equipment: "Jump rope",
  },
];

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

const SESSION = {
  id: "s1",
  notes: "Warm up first.",
  equipment: ["Jump rope", "Kettlebell"],
  parts: [
    part({
      number: 1,
      format: "EMOM",
      minutes: 20,
      rotation: 4,
      prescriptions: [
        {
          exerciseId: SWING,
          sets: 1,
          reps: 5,
          repsMax: null,
          side: "RIGHT",
          holdSeconds: null,
          workSeconds: null,
          bellKg: 16,
          restSeconds: null,
          minute: 1,
        },
        {
          exerciseId: SKIPPING,
          sets: 1,
          reps: 1,
          repsMax: null,
          side: null,
          holdSeconds: null,
          workSeconds: 45,
          bellKg: null,
          restSeconds: null,
          minute: 3,
        },
      ],
    }),
    part({ number: 2, format: "REST", restSeconds: 60 }),
    part({ number: 3, format: "ROUNDS", rounds: 3 }),
  ],
};

const PROGRAM = {
  id: "p1",
  name: "Kettlebell EMOM",
  weeks: [{ id: "w1", number: 1, sessions: [SESSION] }],
};

type Answer = [number, unknown];

/** Stubs the API at the network boundary: "METHOD path" → [status, body]. */
function serve(routes: Record<string, Answer | (() => Answer)>) {
  const fetch = vi.fn(async (path: string, init?: RequestInit) => {
    const route = routes[`${init?.method ?? "GET"} ${path}`];
    const [status, body] = (typeof route === "function" ? route() : route) ?? [
      404,
      { detail: `No route ${path}` },
    ];
    return new Response(JSON.stringify(body), { status });
  });
  vi.stubGlobal("fetch", fetch);
  return fetch;
}

function sent(fetch: ReturnType<typeof serve>, method: string, path: string) {
  const call = fetch.mock.calls.find(([p, init]) => p === path && init?.method === method);
  return call && JSON.parse(String(call[1]?.body));
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/programs/:id/setup" element={<ProgramSetupPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

afterEach(() => vi.unstubAllGlobals());

describe("ProgramSetupPage", () => {
  it("shows each Session with its Equipment, notes and Parts", async () => {
    serve({ "GET /api/programs/p1": [200, PROGRAM], "GET /api/exercises": [200, EXERCISES] });

    renderAt("/programs/p1/setup");

    expect(screen.getByRole("status")).toHaveTextContent("Loading Program");
    const session = await screen.findByRole("article", { name: "Week 1 Session 1" });
    expect(within(session).getByText("Jump rope, Kettlebell")).toBeInTheDocument();
    expect(within(session).getByLabelText("Notes")).toHaveValue("Warm up first.");
    const emom = within(session).getByRole("region", {
      name: "Part 1 · 20 min EMOM, rotating through 4 minutes",
    });
    expect(within(emom).getByText("Minute 1: 1 × 5 Swing · Right · 16 kg")).toBeInTheDocument();
    expect(within(emom).getByText("Minute 2: Rest")).toBeInTheDocument();
    expect(within(emom).getByText("Minute 3: 1 × 1 Skipping · 45 s")).toBeInTheDocument();
    const rest = within(session).getByRole("region", { name: "Part 2 · Rest 60 s" });
    expect(within(rest).queryByRole("form", { name: "Add Prescription" })).not.toBeInTheDocument();
    expect(within(session).getByRole("region", { name: "Part 3 · 3 Rounds" })).toBeInTheDocument();
  });

  it("adds a Prescription on Each side to a Part and shows the Program again", async () => {
    let program: unknown = PROGRAM;
    const fetch = serve({
      "GET /api/programs/p1": () => [200, program],
      "GET /api/exercises": [200, EXERCISES],
      "POST /api/sessions/s1/parts/3/prescriptions": () => {
        program = {
          ...PROGRAM,
          weeks: [
            {
              ...PROGRAM.weeks[0],
              sessions: [
                { ...SESSION, parts: [SESSION.parts[0], SESSION.parts[1], ROUNDS_WITH_PRESS] },
              ],
            },
          ],
        };
        return [201, {}];
      },
    });
    renderAt("/programs/p1/setup");
    const rounds = await screen.findByRole("region", { name: "Part 3 · 3 Rounds" });
    const form = within(rounds).getByRole("form", { name: "Add Prescription" });

    expect(within(form).queryByLabelText("Minute")).not.toBeInTheDocument();
    fireEvent.change(within(form).getByLabelText("Exercise"), { target: { value: SWING } });
    fireEvent.change(within(form).getByLabelText("Reps"), { target: { value: "8" } });
    fireEvent.change(within(form).getByLabelText("Side"), { target: { value: "EACH" } });
    fireEvent.change(within(form).getByLabelText("Bell (kg)"), { target: { value: "12" } });
    fireEvent.click(within(form).getByRole("button", { name: "Add Prescription" }));

    expect(await screen.findByText("1 × 8+8 Swing · 12 kg")).toBeInTheDocument();
    expect(sent(fetch, "POST", "/api/sessions/s1/parts/3/prescriptions")).toEqual({
      exerciseId: SWING,
      sets: 1,
      reps: 8,
      repsMax: null,
      side: "EACH",
      holdSeconds: null,
      workSeconds: null,
      bellKg: 12,
      restSeconds: null,
      minute: null,
    });
  });

  it("asks a Timed Exercise in an EMOM for its Work time and minute, and one Set", async () => {
    const fetch = serve({
      "GET /api/programs/p1": [200, PROGRAM],
      "GET /api/exercises": [200, EXERCISES],
      "POST /api/sessions/s1/parts/1/prescriptions": [201, {}],
    });
    renderAt("/programs/p1/setup");
    const emom = await screen.findByRole("region", { name: /^Part 1 · 20 min EMOM/ });
    const form = within(emom).getByRole("form", { name: "Add Prescription" });

    fireEvent.change(within(form).getByLabelText("Exercise"), { target: { value: SKIPPING } });
    expect(within(form).queryByLabelText("Bell (kg)")).not.toBeInTheDocument();
    expect(within(form).queryByLabelText("Rest (s)")).not.toBeInTheDocument();
    expect(within(form).queryByLabelText("Sets")).not.toBeInTheDocument();
    fireEvent.change(within(form).getByLabelText("Minute"), { target: { value: "4" } });
    fireEvent.change(within(form).getByLabelText("Reps"), { target: { value: "1" } });
    fireEvent.change(within(form).getByLabelText("Work time (s)"), { target: { value: "40" } });
    fireEvent.click(within(form).getByRole("button", { name: "Add Prescription" }));

    await vi.waitFor(() =>
      expect(sent(fetch, "POST", "/api/sessions/s1/parts/1/prescriptions")).toMatchObject({
        exerciseId: SKIPPING,
        sets: 1,
        workSeconds: 40,
        bellKg: null,
        minute: 4,
      }),
    );
  });

  it("adds Parts, notes, Sessions, Weeks and Exercises, and shows why the server says no", async () => {
    const fetch = serve({
      "GET /api/programs/p1": [200, PROGRAM],
      "GET /api/exercises": [200, EXERCISES],
      "POST /api/sessions/s1/parts": [201, SESSION],
      "PUT /api/sessions/s1/notes": [200, SESSION],
      "POST /api/weeks/w1/sessions": [201, SESSION],
      "POST /api/programs/p1/weeks": [201, {}],
      "POST /api/exercises": [409, { detail: "There is an Exercise named Swing already" }],
    });
    renderAt("/programs/p1/setup");
    const session = await screen.findByRole("article", { name: "Week 1 Session 1" });

    const addPart = within(session).getByRole("form", { name: "Add Part" });
    fireEvent.change(within(addPart).getByLabelText("Format"), { target: { value: "EMOM" } });
    fireEvent.change(within(addPart).getByLabelText("Minutes"), { target: { value: "12" } });
    fireEvent.change(within(addPart).getByLabelText("Rotation (minutes)"), {
      target: { value: "3" },
    });
    fireEvent.click(within(addPart).getByRole("button", { name: "Add Part" }));
    await vi.waitFor(() =>
      expect(sent(fetch, "POST", "/api/sessions/s1/parts")).toEqual({
        format: "EMOM",
        minutes: 12,
        rotation: 3,
      }),
    );

    fireEvent.change(within(session).getByLabelText("Notes"), {
      target: { value: "Keep intensity at 70-80%." },
    });
    fireEvent.click(within(session).getByRole("button", { name: "Save notes" }));
    await vi.waitFor(() =>
      expect(sent(fetch, "PUT", "/api/sessions/s1/notes")).toEqual({
        notes: "Keep intensity at 70-80%.",
      }),
    );

    fireEvent.click(screen.getByRole("button", { name: "Add Session to Week 1" }));
    fireEvent.click(screen.getByRole("button", { name: "Add Week" }));
    await vi.waitFor(() =>
      expect(fetch.mock.calls.map(([path]) => path)).toEqual(
        expect.arrayContaining(["/api/weeks/w1/sessions", "/api/programs/p1/weeks"]),
      ),
    );

    const addExercise = screen.getByRole("form", { name: "Add Exercise" });
    fireEvent.change(within(addExercise).getByLabelText("Name"), { target: { value: "Swing" } });
    fireEvent.click(within(addExercise).getByRole("button", { name: "Add Exercise" }));
    expect(await within(addExercise).findByRole("alert")).toHaveTextContent(
      "Could not add the Exercise: There is an Exercise named Swing already",
    );
    expect(sent(fetch, "POST", "/api/exercises")).toEqual({
      name: "Swing",
      bodyweight: false,
      hold: false,
      timed: false,
      gear: null,
    });
  });

  it("shows the server's reason when the Program cannot be loaded", async () => {
    serve({ "GET /api/programs/p1": [404, { detail: "No Program with id p1" }] });

    renderAt("/programs/p1/setup");

    expect(await screen.findByRole("alert")).toHaveTextContent("No Program with id p1");
  });
});

describe("HomePage", () => {
  it("offers the CSV export", () => {
    renderAt("/");

    expect(screen.getByRole("heading", { name: "Export to CSV" })).toBeInTheDocument();
  });

  it("creates a Program and opens its setup", async () => {
    const fetch = serve({
      "POST /api/programs": [201, { id: "p1", name: "Kettlebell EMOM", weeks: [] }],
      "GET /api/programs/p1": [200, { id: "p1", name: "Kettlebell EMOM", weeks: [] }],
      "GET /api/exercises": [200, EXERCISES],
    });
    renderAt("/");

    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Kettlebell EMOM" } });
    fireEvent.click(screen.getByRole("button", { name: "Create Program" }));

    expect(
      await screen.findByRole("heading", { name: "Set up Kettlebell EMOM" }),
    ).toBeInTheDocument();
    expect(screen.getByText("This Program has no Weeks yet.")).toBeInTheDocument();
    expect(sent(fetch, "POST", "/api/programs")).toEqual({ name: "Kettlebell EMOM" });
  });

  it("adds an AMRAP with its time cap and a rep range without rest", async () => {
    const amrap = part({
      number: 4,
      format: "AMRAP",
      minutes: 12,
      prescriptions: [
        {
          exerciseId: SWING,
          sets: 1,
          reps: 3,
          repsMax: 4,
          side: "EACH",
          holdSeconds: null,
          workSeconds: null,
          bellKg: 16,
          restSeconds: null,
          minute: null,
        },
      ],
    });
    const program = {
      ...PROGRAM,
      weeks: [
        { id: "w1", number: 1, sessions: [{ ...SESSION, parts: [...SESSION.parts, amrap] }] },
      ],
    };
    const fetch = serve({
      "GET /api/programs/p1": [200, program],
      "GET /api/exercises": [200, EXERCISES],
      "POST /api/sessions/s1/parts": [201, SESSION],
      "POST /api/sessions/s1/parts/4/prescriptions": [201, {}],
    });
    renderAt("/programs/p1/setup");
    const region = await screen.findByRole("region", { name: "Part 4 · 12 min AMRAP" });

    expect(within(region).getByText("1 × 3-4+3-4 Swing · 16 kg")).toBeInTheDocument();
    const form = within(region).getByRole("form", { name: "Add Prescription" });
    expect(within(form).queryByLabelText("Rest (s)")).not.toBeInTheDocument();
    fireEvent.change(within(form).getByLabelText("Exercise"), { target: { value: SWING } });
    fireEvent.change(within(form).getByLabelText("Reps"), { target: { value: "8" } });
    fireEvent.change(within(form).getByLabelText("Up to Reps"), { target: { value: "10" } });
    fireEvent.change(within(form).getByLabelText("Bell (kg)"), { target: { value: "24" } });
    fireEvent.click(within(form).getByRole("button", { name: "Add Prescription" }));
    await vi.waitFor(() =>
      expect(sent(fetch, "POST", "/api/sessions/s1/parts/4/prescriptions")).toMatchObject({
        reps: 8,
        repsMax: 10,
        restSeconds: null,
      }),
    );

    const addPart = screen.getByRole("form", { name: "Add Part" });
    fireEvent.change(within(addPart).getByLabelText("Format"), { target: { value: "AMRAP" } });
    fireEvent.change(within(addPart).getByLabelText("Time cap (minutes)"), {
      target: { value: "15" },
    });
    fireEvent.click(within(addPart).getByRole("button", { name: "Add Part" }));
    await vi.waitFor(() =>
      expect(sent(fetch, "POST", "/api/sessions/s1/parts")).toEqual({
        format: "AMRAP",
        minutes: 15,
      }),
    );
  });
});

const ROUNDS_WITH_PRESS = part({
  number: 3,
  format: "ROUNDS",
  rounds: 3,
  prescriptions: [
    {
      exerciseId: SWING,
      sets: 1,
      reps: 8,
      repsMax: null,
      side: "EACH",
      holdSeconds: null,
      workSeconds: null,
      bellKg: 12,
      restSeconds: null,
      minute: null,
    },
  ],
});
