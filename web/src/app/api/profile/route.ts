import { cookies } from "next/headers";
import { fail, handleError, ok, readJson } from "@/lib/api";
import { assertDemoMutationAllowed } from "@/lib/db";
import { countForMember, updateProfile } from "@/lib/queries";
import {
  SESSION_COOKIE,
  assertCsrf,
  requireSession,
  serializeSession,
  sessionCookieOptions,
} from "@/lib/session";
import { profileSchema } from "@/lib/validation";
import { clientKey, rateLimit } from "@/lib/rate-limit";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET() {
  try {
    const session = await requireSession();
    const activity = await countForMember(session.id);
    return ok({ user: session, activity });
  } catch (error) {
    return handleError(error);
  }
}

export async function PUT(request: Request) {
  try {
    const session = await requireSession();
    await assertCsrf(request);
    assertDemoMutationAllowed(session.nom);

    const limit = rateLimit(clientKey(request, "write"), 20, 60);
    if (!limit.ok) return fail(429, "error.rateLimited", { retryAfter: limit.retryAfterSeconds });

    const input = profileSchema.parse(await readJson(request));
    if (!(await updateProfile(session.id, input))) return fail(404, "error.notFound");

    // The session carries the display name and email, so it has to be reissued —
    // otherwise the sidebar keeps showing the old values until the next sign-in.
    const updated = { id: session.id, nom: input.nom, email: input.email, role: session.role };
    const store = await cookies();
    store.set(SESSION_COOKIE, serializeSession(updated), sessionCookieOptions());

    return ok({ user: updated });
  } catch (error) {
    if (error instanceof Error && /duplicate key|unique/i.test(error.message)) {
      return fail(409, "error.username.taken");
    }
    return handleError(error);
  }
}
