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
  const response = await fetch(path, { headers: { Accept: "application/json" } });
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
