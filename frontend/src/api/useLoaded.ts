import { useEffect, useState } from "react";
import { errorMessage } from "./client";

/** Where a page's load stands: still loading, failed with a message, or loaded. */
export type Loaded<T> =
  | { status: "loading" }
  | { status: "failed"; message: string }
  | { status: "loaded"; value: T };

type Settled<T> = Exclude<Loaded<T>, { status: "loading" }>;

/**
 * Runs `load` and tracks its outcome for a page. Pass a `load` made with
 * `useCallback` over the route parameters: a new `load` shows "loading" again
 * and starts over, and an outcome that arrives for an earlier one is dropped.
 */
export function useLoaded<T>(load: () => Promise<T>): Loaded<T> {
  // The outcome remembers which load it is for, so a new load shows "loading"
  // again without resetting state inside the effect.
  const [outcome, setOutcome] = useState<{ load: () => Promise<T>; settled: Settled<T> } | null>(
    null,
  );

  useEffect(() => {
    let current = true;
    const settle = (settled: Settled<T>) => current && setOutcome({ load, settled });
    load().then(
      (value) => settle({ status: "loaded", value }),
      (error: unknown) => settle({ status: "failed", message: errorMessage(error) }),
    );
    return () => {
      current = false;
    };
  }, [load]);

  return outcome?.load === load ? outcome.settled : { status: "loading" };
}
