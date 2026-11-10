import { useState } from "react";
import { addExercise } from "./programSetup";
import { useSubmit } from "./useSubmit";

type Measure = "reps" | "hold" | "timed";

/** Adds an Exercise to the catalog: its name, whether it takes a Bell, how it is measured. */
export function AddExerciseForm({
  change,
}: {
  change: (send: () => Promise<void>) => Promise<void>;
}) {
  const [name, setName] = useState("");
  const [bodyweight, setBodyweight] = useState(false);
  const [measure, setMeasure] = useState<Measure>("reps");
  const [gear, setGear] = useState("");
  const { submit, saving, failure } = useSubmit(
    () =>
      change(() =>
        addExercise({
          name,
          bodyweight,
          hold: measure === "hold",
          timed: measure === "timed",
          gear: bodyweight && gear.trim() !== "" ? gear : null,
        }),
      ),
    () => {
      setName("");
      setGear("");
    },
  );

  return (
    <form className="setup-form" aria-label="Add Exercise" onSubmit={submit}>
      <h2>Add Exercise</h2>
      <label>
        Name <input required value={name} onChange={(e) => setName(e.target.value)} />
      </label>
      <label>
        <input
          type="checkbox"
          checked={bodyweight}
          onChange={(e) => setBodyweight(e.target.checked)}
        />{" "}
        Bodyweight (no Bell)
      </label>
      {bodyweight && (
        <label>
          Gear{" "}
          <input value={gear} placeholder="Jump rope" onChange={(e) => setGear(e.target.value)} />
        </label>
      )}
      <label>
        Measured in{" "}
        <select value={measure} onChange={(e) => setMeasure(e.target.value as Measure)}>
          <option value="reps">Reps</option>
          <option value="hold">Hold time</option>
          <option value="timed">Work time</option>
        </select>
      </label>
      <button type="submit" disabled={saving}>
        Add Exercise
      </button>
      {failure && (
        <p role="alert" className="error">
          Could not add the Exercise: {failure}
        </p>
      )}
    </form>
  );
}
