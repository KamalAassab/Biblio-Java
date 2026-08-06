import { cookies } from "next/headers";
import { handleError, ok } from "@/lib/api";
import { CSRF_COOKIE, SESSION_COOKIE } from "@/lib/session";

export const runtime = "nodejs";

export async function POST() {
  try {
    // No CSRF check here on purpose: a forced sign-out is not a harmful action, and
    // refusing it would leave a user stuck with a stale session they cannot clear.
    const store = await cookies();
    store.delete(SESSION_COOKIE);
    store.delete(CSRF_COOKIE);
    return ok({ ok: true });
  } catch (error) {
    return handleError(error);
  }
}
