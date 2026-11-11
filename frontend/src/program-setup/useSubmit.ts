import { useState } from "react";
import { errorMessage } from "../api/client";

/**
 * Tracks one form's save: `submit` runs `send` and keeps its error message, so
 * the form shows why the server said no. `onSaved` runs after a save, to reset
 * the form.
 */
export function useSubmit(send: () => Promise<void>, onSaved: () => void = () => {}) {
  const [saving, setSaving] = useState(false);
  const [failure, setFailure] = useState<string | null>(null);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setFailure(null);
    try {
      await send();
      onSaved();
    } catch (error) {
      setFailure(errorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  return { submit, saving, failure };
}
