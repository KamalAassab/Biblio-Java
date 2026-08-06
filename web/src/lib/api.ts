import { NextResponse } from "next/server";
import { ZodError } from "zod";
import { CsrfError, ForbiddenError, UnauthorizedError } from "@/lib/session";
import { DemoRestrictionError } from "@/lib/db";

/**
 * Response helpers for route handlers.
 *
 * The important rule here: internal error detail never reaches the client. A database
 * failure returns a generic message and the real cause goes to the server log, so a
 * probing request cannot map out the schema from error strings.
 */

export function ok<T>(data: T, init?: ResponseInit) {
  return NextResponse.json(data, {
    ...init,
    headers: { "cache-control": "no-store", ...(init?.headers ?? {}) },
  });
}

export function fail(status: number, code: string, extra?: Record<string, unknown>) {
  return NextResponse.json(
    { error: code, ...extra },
    { status, headers: { "cache-control": "no-store" } },
  );
}

/**
 * Maps a thrown error to a safe HTTP response.
 *
 * Error codes are i18n keys the client resolves, so the API never returns
 * user-facing prose in a single hard-coded language.
 */
export function handleError(error: unknown) {
  if (error instanceof UnauthorizedError) return fail(401, "error.unauthorized");
  if (error instanceof ForbiddenError) return fail(403, "error.notAllowed");
  if (error instanceof CsrfError) return fail(403, "error.csrf");
  if (error instanceof DemoRestrictionError) return fail(403, "error.demoRestricted");

  if (error instanceof ZodError) {
    const first = error.issues[0];
    return fail(400, first?.message ?? "error.invalidInput", {
      field: first?.path?.join(".") ?? undefined,
    });
  }

  console.error("[api]", error);
  return fail(500, "error.server");
}

/** Parses a JSON body, rejecting anything oversized before it reaches a schema. */
export async function readJson(request: Request, maxBytes = 64 * 1024): Promise<unknown> {
  const raw = await request.text();
  if (raw.length > maxBytes) throw new ZodError([]);
  if (!raw) return {};
  try {
    return JSON.parse(raw);
  } catch {
    throw new ZodError([]);
  }
}
