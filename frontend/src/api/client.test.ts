import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, getJson } from "./client";

function answer(status: number, body: unknown, contentType = "application/json") {
  const text = typeof body === "string" ? body : JSON.stringify(body);
  vi.stubGlobal(
    "fetch",
    vi.fn(async () => new Response(text, { status, headers: { "Content-Type": contentType } })),
  );
}

afterEach(() => vi.unstubAllGlobals());

describe("getJson", () => {
  it("returns the JSON body of a 2xx answer", async () => {
    answer(200, { id: "p1" });

    await expect(getJson("/api/programs/p1")).resolves.toEqual({ id: "p1" });
  });

  it("throws an ApiError with the ProblemDetail detail on a non-2xx answer", async () => {
    answer(404, { status: 404, detail: "No Program with id p1" }, "application/problem+json");

    const error = await getJson("/api/programs/p1").catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 404, message: "No Program with id p1" });
  });

  it("throws an ApiError with the status when the error body is not JSON", async () => {
    answer(502, "Bad gateway", "text/plain");

    await expect(getJson("/api/programs/p1")).rejects.toMatchObject({ status: 502 });
  });
});
