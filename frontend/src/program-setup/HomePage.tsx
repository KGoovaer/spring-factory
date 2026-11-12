import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { CsvExportLinks } from "../export/CsvExportLinks";
import { createProgram } from "./programSetup";
import { useSubmit } from "./useSubmit";

/** The page at /: create a Program, then set it up, or export everything as CSV. */
export function HomePage() {
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const { submit, saving, failure } = useSubmit(async () => {
    const id = await createProgram(name);
    navigate(`/programs/${encodeURIComponent(id)}/setup`);
  });

  return (
    <main className="page">
      <h1>Kettlebell Planner</h1>
      <p>Open a Program at /programs/&lt;id&gt;, or create one.</p>
      <form className="setup-form" aria-label="Create Program" onSubmit={submit}>
        <label>
          Name <input required value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        <button type="submit" className="primary" disabled={saving}>
          Create Program
        </button>
        {failure && (
          <p role="alert" className="error">
            Could not create the Program: {failure}
          </p>
        )}
      </form>
      <CsvExportLinks />
    </main>
  );
}
