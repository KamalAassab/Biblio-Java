/**
 * Browser-side API client.
 *
 * Attaches the double-submit CSRF token to every mutating request and normalises
 * failures into a thrown `ApiError` carrying an i18n key, so callers can surface a
 * localised message without parsing response shapes.
 */

export class ApiError extends Error {
  constructor(
    public readonly key: string,
    public readonly status: number,
    public readonly field?: string,
    public readonly retryAfter?: number,
  ) {
    super(key);
    this.name = "ApiError";
  }
}

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

const MUTATING = new Set(["POST", "PUT", "PATCH", "DELETE"]);

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);

  if (init.body && !headers.has("content-type")) {
    headers.set("content-type", "application/json");
  }
  if (MUTATING.has(method)) {
    const token = readCookie("biblio_csrf");
    if (token) headers.set("x-csrf-token", token);
  }

  let response: Response;
  try {
    response = await fetch(path, {
      ...init,
      method,
      headers,
      // Cookies carry the session; without this the API always sees a signed-out caller.
      credentials: "same-origin",
      cache: "no-store",
    });
  } catch {
    throw new ApiError("error.network", 0);
  }

  const payload = await response.json().catch(() => ({}) as Record<string, unknown>);

  if (!response.ok) {
    throw new ApiError(
      typeof payload.error === "string" ? payload.error : "error.server",
      response.status,
      typeof payload.field === "string" ? payload.field : undefined,
      typeof payload.retryAfter === "number" ? payload.retryAfter : undefined,
    );
  }
  return payload as T;
}

export const apiGet = <T>(path: string) => api<T>(path);

export const apiSend = <T>(path: string, method: "POST" | "PUT" | "DELETE", body?: unknown) =>
  api<T>(path, { method, body: body === undefined ? undefined : JSON.stringify(body) });
