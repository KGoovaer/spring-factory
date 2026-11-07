import { Route, Routes } from "react-router-dom";
import { ProgramOverviewPage } from "./program-overview/ProgramOverviewPage";

export function App() {
  return (
    <Routes>
      <Route path="/programs/:id" element={<ProgramOverviewPage />} />
      <Route
        path="*"
        element={
          <main className="page">
            <h1>Kettlebell Planner</h1>
            <p>Open a Program at /programs/&lt;id&gt;.</p>
          </main>
        }
      />
    </Routes>
  );
}
