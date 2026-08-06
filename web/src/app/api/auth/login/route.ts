import { cookies } from "next/headers";
import { fail, handleError, ok, readJson } from "@/lib/api";
import { clientKey, rateLimit } from "@/lib/rate-limit";
import { needsUpgrade, hashPassword, verifyPassword } from "@/lib/password";
import { findMemberForAuth, storePasswordHash } from "@/lib/queries";
import {
  CSRF_COOKIE,
  SESSION_COOKIE,
  csrfCookieOptions,
  newCsrfToken,
  serializeSession,
  sessionCookieOptions,
} from "@/lib/session";
import { loginSchema } from "@/lib/validation";

/** PBKDF2 verification is intentionally slow; the default 15s budget is not enough headroom. */
export const maxDuration = 30;
export const runtime = "nodejs";

export async function POST(request: Request) {
  try {
    // Two layers: a burst limit and a slower sustained limit. Together they stop
    // credential stuffing without locking out someone who mistypes twice.
    const burst = rateLimit(clientKey(request, "login-burst"), 5, 60);
    const sustained = rateLimit(clientKey(request, "login-hour"), 30, 3600);
    if (!burst.ok || !sustained.ok) {
      const retry = Math.max(burst.retryAfterSeconds, sustained.retryAfterSeconds);
      return fail(429, "login.error.locked", { retryAfter: retry });
    }

    const body = loginSchema.parse(await readJson(request));
    const member = await findMemberForAuth(body.nom);

    // Unknown username still runs a full verification pass, so response time does
    // not reveal which accounts exist.
    const valid = await verifyPassword(body.motDePasse, member?.hash ?? null);
    if (!member || !valid) {
      return fail(401, "login.error.invalid");
    }

    // Transparently upgrade a legacy plaintext row now that the password is known.
    if (needsUpgrade(member.hash)) {
      await storePasswordHash(member.id, await hashPassword(body.motDePasse));
    }

    const role = member.role === "Admin" ? "Admin" : "Lecteur";
    const session = { id: member.id, nom: member.nom, email: member.email ?? "", role } as const;

    const store = await cookies();
    store.set(SESSION_COOKIE, serializeSession(session), sessionCookieOptions());
    store.set(CSRF_COOKIE, newCsrfToken(), csrfCookieOptions());

    return ok({ user: session });
  } catch (error) {
    return handleError(error);
  }
}
