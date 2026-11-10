import { Route, Routes } from "react-router-dom";
import { LogSessionPage } from "./log-session/LogSessionPage";
import { ProgramOverviewPage } from "./program-overview/ProgramOverviewPage";
import { HomePage } from "./program-setup/HomePage";
import { ProgramSetupPage } from "./program-setup/ProgramSetupPage";

export function App() {
  return (
    <Routes>
      <Route path="/programs/:id" element={<ProgramOverviewPage />} />
      <Route path="/programs/:id/setup" element={<ProgramSetupPage />} />
      <Route path="/programs/:id/sessions/:sessionId/log" element={<LogSessionPage />} />
      <Route path="*" element={<HomePage />} />
    </Routes>
  );
}
