import { useState } from "react";
import { Link } from "react-router-dom";
import type {
  ExerciseResponse,
  PartFormat,
  PartResponse,
  SessionResponse,
  Side,
} from "../api/programs";
import { partTitle } from "../api/programs";
import {
  addPart,
  addPrescription,
  changeNotes,
  describePrescription,
  type NewPart,
} from "./programSetup";
import { useSubmit } from "./useSubmit";

type Change = (send: () => Promise<void>) => Promise<void>;

/** One Session being set up: its notes, its Equipment, its Parts and their Prescriptions. */
export function SessionSetupView({
  programId,
  label,
  session,
  exercises,
  change,
}: {
  programId: string;
  label: string;
  session: SessionResponse;
  exercises: ExerciseResponse[];
  change: Change;
}) {
  return (
    <article className="setup-session" aria-label={label}>
      <h3>
        {label}{" "}
        <Link
          className="log-link"
          to={`/programs/${encodeURIComponent(programId)}/sessions/${encodeURIComponent(session.id)}/log`}
        >
          Log Session
        </Link>
      </h3>
      <p className="equipment">
        <strong>Equipment:</strong>{" "}
        {session.equipment.length > 0 ? session.equipment.join(", ") : "none yet"}
      </p>
      <NotesForm session={session} change={change} />
      {session.parts.length === 0 && <p>This Session has no Parts yet.</p>}
      {session.parts.map((part) => (
        <section key={part.number} className="setup-part" aria-label={partTitle(part)}>
          <h4>{partTitle(part)}</h4>
          {part.format !== "REST" && (
            <>
              <PrescriptionList part={part} exercises={exercises} />
              <AddPrescriptionForm
                sessionId={session.id}
                part={part}
                exercises={exercises}
                change={change}
              />
            </>
          )}
        </section>
      ))}
      <AddPartForm sessionId={session.id} change={change} />
    </article>
  );
}

function PrescriptionList({
  part,
  exercises,
}: {
  part: PartResponse;
  exercises: ExerciseResponse[];
}) {
  if (part.prescriptions.length === 0) {
    return <p>No Prescriptions yet.</p>;
  }
  if (part.format !== "EMOM") {
    return (
      <ol>
        {part.prescriptions.map((prescription, index) => (
          <li key={index}>{describePrescription(prescription, exercises)}</li>
        ))}
      </ol>
    );
  }
  return (
    <ol className="emom-minutes">
      {Array.from({ length: part.rotation ?? 0 }, (_, index) => {
        const inMinute = part.prescriptions.filter((p) => p.minute === index + 1);
        return (
          <li key={index}>
            Minute {index + 1}:{" "}
            {inMinute.length === 0
              ? "Rest"
              : inMinute.map((p) => describePrescription(p, exercises)).join("; ")}
          </li>
        );
      })}
    </ol>
  );
}

function NotesForm({ session, change }: { session: SessionResponse; change: Change }) {
  const [notes, setNotes] = useState(session.notes ?? "");
  const { submit, saving, failure } = useSubmit(() => change(() => changeNotes(session.id, notes)));
  return (
    <form className="setup-form notes-form" aria-label="Session notes" onSubmit={submit}>
      <label>
        Notes
        <textarea
          rows={3}
          value={notes}
          placeholder="Intensity, warm-up, scale-down options…"
          onChange={(e) => setNotes(e.target.value)}
        />
      </label>
      <button type="submit" disabled={saving}>
        Save notes
      </button>
      {failure && (
        <p role="alert" className="error">
          Could not save the notes: {failure}
        </p>
      )}
    </form>
  );
}

const FORMATS: { value: PartFormat; label: string }[] = [
  { value: "SETS", label: "Sets" },
  { value: "ROUNDS", label: "Rounds" },
  { value: "EMOM", label: "EMOM" },
  { value: "REST", label: "Rest" },
];

function AddPartForm({ sessionId, change }: { sessionId: string; change: Change }) {
  const [format, setFormat] = useState<PartFormat>("SETS");
  const [rounds, setRounds] = useState("3");
  const [minutes, setMinutes] = useState("20");
  const [rotation, setRotation] = useState("4");
  const [rest, setRest] = useState("60");
  const part = (): NewPart => {
    switch (format) {
      case "SETS":
        return { format };
      case "ROUNDS":
        return { format, rounds: Number(rounds) };
      case "EMOM":
        return { format, minutes: Number(minutes), rotation: Number(rotation) };
      case "REST":
        return { format, restSeconds: Number(rest) };
    }
  };
  const { submit, saving, failure } = useSubmit(() => change(() => addPart(sessionId, part())));

  return (
    <form className="setup-form" aria-label="Add Part" onSubmit={submit}>
      <label>
        Format{" "}
        <select value={format} onChange={(e) => setFormat(e.target.value as PartFormat)}>
          {FORMATS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>
      {format === "ROUNDS" && <NumberField label="Rounds" value={rounds} onChange={setRounds} />}
      {format === "EMOM" && (
        <>
          <NumberField label="Minutes" value={minutes} onChange={setMinutes} />
          <NumberField label="Rotation (minutes)" value={rotation} onChange={setRotation} />
        </>
      )}
      {format === "REST" && <NumberField label="Rest (s)" value={rest} onChange={setRest} />}
      <button type="submit" disabled={saving}>
        Add Part
      </button>
      {failure && (
        <p role="alert" className="error">
          Could not add the Part: {failure}
        </p>
      )}
    </form>
  );
}

function AddPrescriptionForm({
  sessionId,
  part,
  exercises,
  change,
}: {
  sessionId: string;
  part: PartResponse;
  exercises: ExerciseResponse[];
  change: Change;
}) {
  const [exerciseId, setExerciseId] = useState("");
  const [sets, setSets] = useState("1");
  const [reps, setReps] = useState("5");
  const [side, setSide] = useState<Side | "">("");
  const [time, setTime] = useState("");
  const [bell, setBell] = useState("");
  const [rest, setRest] = useState("");
  const [minute, setMinute] = useState("1");
  const exercise = exercises.find((candidate) => candidate.id === exerciseId);
  const emom = part.format === "EMOM";
  const optional = (typed: string) => (typed === "" ? null : Number(typed));

  const { submit, saving, failure } = useSubmit(() =>
    change(() =>
      addPrescription(sessionId, part.number, {
        exerciseId,
        sets: Number(sets),
        reps: Number(reps),
        side: side === "" ? null : side,
        holdSeconds: exercise?.hold ? optional(time) : null,
        workSeconds: exercise?.timed ? optional(time) : null,
        bellKg: exercise && !exercise.bodyweight ? optional(bell) : null,
        restSeconds: emom ? null : optional(rest),
        minute: emom ? Number(minute) : null,
      }),
    ),
  );

  return (
    <form className="setup-form" aria-label="Add Prescription" onSubmit={submit}>
      <label>
        Exercise{" "}
        <select required value={exerciseId} onChange={(e) => setExerciseId(e.target.value)}>
          <option value="">Choose…</option>
          {exercises.map((option) => (
            <option key={option.id} value={option.id}>
              {option.name}
            </option>
          ))}
        </select>
      </label>
      {emom && (
        <label>
          Minute{" "}
          <select value={minute} onChange={(e) => setMinute(e.target.value)}>
            {Array.from({ length: part.rotation ?? 1 }, (_, index) => (
              <option key={index} value={index + 1}>
                {index + 1}
              </option>
            ))}
          </select>
        </label>
      )}
      <NumberField label="Sets" value={sets} onChange={setSets} />
      <NumberField label="Reps" value={reps} onChange={setReps} />
      <label>
        Side{" "}
        <select value={side} onChange={(e) => setSide(e.target.value as Side | "")}>
          <option value="">None</option>
          <option value="LEFT">Left</option>
          <option value="RIGHT">Right</option>
          <option value="EACH">Each (8+8)</option>
        </select>
      </label>
      {exercise?.hold && <NumberField label="Hold time (s)" value={time} onChange={setTime} />}
      {exercise?.timed && <NumberField label="Work time (s)" value={time} onChange={setTime} />}
      {exercise && !exercise.bodyweight && (
        <NumberField label="Bell (kg)" value={bell} onChange={setBell} step="any" />
      )}
      {!emom && <NumberField label="Rest (s)" value={rest} onChange={setRest} required={false} />}
      <button type="submit" disabled={saving}>
        Add Prescription
      </button>
      {failure && (
        <p role="alert" className="error">
          Could not add the Prescription: {failure}
        </p>
      )}
    </form>
  );
}

function NumberField({
  label,
  value,
  onChange,
  step,
  required = true,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  step?: string;
  required?: boolean;
}) {
  return (
    <label>
      {label}{" "}
      <input
        type="number"
        step={step}
        required={required}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    </label>
  );
}
