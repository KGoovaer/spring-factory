/** An API call that answered with a status outside 2xx, or could not be read. */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

/**
 * GETs `path` and returns its JSON body. Throws {@link ApiError} for any
 * non-2xx answer, with the ProblemDetail `detail` as message when the server
 * sent one. Never returns an error-shaped value.
 */
export async function getJson<T>(path: string): Promise<T> {
  return requestJson<T>(path, { headers: { Accept: "application/json" } });
}

/**
 * Sends `body` as JSON to `path` with `method` (POST or PUT) and returns the
 * JSON body of the answer. Throws {@link ApiError} for any non-2xx answer, as
 * {@link getJson} does.
 */
export async function sendJson<T>(method: "POST" | "PUT", path: string, body: unknown): Promise<T> {
  return requestJson<T>(path, {
    method,
    headers: { Accept: "application/json", "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

async function requestJson<T>(path: string, init: RequestInit): Promise<T> {
  const response = await fetch(path, init);
  if (!response.ok) {
    throw new ApiError(response.status, await problemDetail(response));
  }
  return (await response.json()) as T;
}

async function problemDetail(response: Response): Promise<string> {
  const fallback = `${response.status} ${response.statusText}`.trim();
  try {
    const body: unknown = await response.json();
    if (body && typeof body === "object" && "detail" in body && typeof body.detail === "string") {
      return body.detail;
    }
  } catch {
    // Not JSON: keep the status line.
  }
  return fallback;
}

/** What to show the Lifter for a caught error: its message, or the value itself. */
export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
