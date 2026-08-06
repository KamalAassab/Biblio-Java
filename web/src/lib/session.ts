import { createHmac, randomBytes, timingSafeEqual } from "node:crypto";
import { cookies } from "next/headers";
import { CSRF_COOKIE, CSRF_HEADER, SESSION_COOKIE } from "@/lib/cookie-names";

/**
 * Stateless sessions carried in a signed, httpOnly cookie.
 *
 * Deliberately dependency-free. The payload is small and non-secret (id, name, role),
 * so it is stored as base64url JSON with an HMAC-SHA256 tag over `payload.expiry`.
 * Tampering invalidates the tag; the cookie flags stop the browser handing it to
 * scripts or to cross-site requests.
 */

// Re-exported so server-side callers can keep importing everything from one place.
export { SESSION_COOKIE, CSRF_COOKIE, CSRF_HEADER };

/** Eight hours — a working day at the library desk, then re-authenticate. */
const MAX_AGE_SECONDS = 8 * 60 * 60;

export type Role = "Admin" | "Lecteur";

export interface Session {
  id: number;
  nom: string;
  email: string;
  role: Role;
  /** Unix seconds. */
  exp: number;
}

function secret(): Buffer {
  const value = process.env.SESSION_SECRET;
  if (!value || value.length < 32) {
    throw new Error(
      "SESSION_SECRET is missing or too short (needs >= 32 chars). Generate one with: " +
        'node -e "console.log(require(\'crypto\').randomBytes(32).toString(\'base64url\'))"',
    );
  }
  return Buffer.from(value, "utf8");
}

function sign(payload: string): string {
  return createHmac("sha256", secret()).update(payload).digest("base64url");
}

export function serializeSession(session: Omit<Session, "exp">): string {
  const exp = Math.floor(Date.now() / 1000) + MAX_AGE_SECONDS;
  const payload = Buffer.from(JSON.stringify({ ...session, exp }), "utf8").toString("base64url");
  return `${payload}.${sign(payload)}`;
}

export function parseSession(token: string | undefined): Session | null {
  if (!token) return null;

  const separator = token.lastIndexOf(".");
  if (separator <= 0) return null;

  const payload = token.slice(0, separator);
  const tag = token.slice(separator + 1);

  const expected = Buffer.from(sign(payload), "utf8");
  const provided = Buffer.from(tag, "utf8");
  if (expected.length !== provided.length || !timingSafeEqual(expected, provided)) return null;

  let session: Session;
  try {
    session = JSON.parse(Buffer.from(payload, "base64url").toString("utf8"));
  } catch {
    return null;
  }

  if (typeof session.exp !== "number" || session.exp * 1000 < Date.now()) return null;
  if (typeof session.id !== "number" || (session.role !== "Admin" && session.role !== "Lecteur")) {
    return null;
  }
  return session;
}

/** Reads the session for the current request, or null when signed out. */
export async function getSession(): Promise<Session | null> {
  const store = await cookies();
  return parseSession(store.get(SESSION_COOKIE)?.value);
}

/** Reads the session, throwing when absent — for routes that require a signed-in user. */
export async function requireSession(): Promise<Session> {
  const session = await getSession();
  if (!session) throw new UnauthorizedError();
  return session;
}

/** Reads the session, throwing unless the user is an administrator. */
export async function requireAdmin(): Promise<Session> {
  const session = await requireSession();
  if (session.role !== "Admin") throw new ForbiddenError();
  return session;
}

export class UnauthorizedError extends Error {
  constructor() {
    super("Not signed in");
    this.name = "UnauthorizedError";
  }
}

export class ForbiddenError extends Error {
  constructor() {
    super("Insufficient permissions");
    this.name = "ForbiddenError";
  }
}

export function sessionCookieOptions() {
  return {
    httpOnly: true,
    // Lax still allows a normal top-level navigation back into the app while
    // blocking the cross-site POSTs that CSRF depends on.
    sameSite: "lax" as const,
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: MAX_AGE_SECONDS,
  };
}

// ── CSRF ───────────────────────────────────────────────────────────────────────

/**
 * Double-submit CSRF token.
 *
 * The token is issued in a readable cookie and must be echoed in a request header.
 * An attacker's page can cause the cookie to ride along on a cross-site request but
 * cannot read it to populate the header, so the two never match.
 */
export function newCsrfToken(): string {
  return randomBytes(32).toString("base64url");
}

export function csrfCookieOptions() {
  return {
    httpOnly: false, // the client script has to read this to echo it back
    sameSite: "lax" as const,
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: MAX_AGE_SECONDS,
  };
}

export async function assertCsrf(request: Request): Promise<void> {
  const store = await cookies();
  const cookieToken = store.get(CSRF_COOKIE)?.value;
  const headerToken = request.headers.get(CSRF_HEADER);

  if (!cookieToken || !headerToken) throw new CsrfError();

  const a = Buffer.from(cookieToken, "utf8");
  const b = Buffer.from(headerToken, "utf8");
  if (a.length !== b.length || !timingSafeEqual(a, b)) throw new CsrfError();
}

export class CsrfError extends Error {
  constructor() {
    super("Invalid CSRF token");
    this.name = "CsrfError";
  }
}
