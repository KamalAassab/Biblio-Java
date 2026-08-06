import { fail, handleError, ok, readJson } from "@/lib/api";
import { assertDemoMutationAllowed } from "@/lib/db";
import { hashPassword, verifyPassword } from "@/lib/password";
import { getPasswordHash, storePasswordHash } from "@/lib/queries";
import { assertCsrf, requireSession } from "@/lib/session";
import { changePasswordSchema } from "@/lib/validation";
import { clientKey, rateLimit } from "@/lib/rate-limit";

export const maxDuration = 30;
export const runtime = "nodejs";

export async function PUT(request: Request) {
  try {
    const session = await requireSession();
    await assertCsrf(request);
    // Changing the demo accounts' passwords would lock every later visitor out.
    assertDemoMutationAllowed(session.nom);

    const limit = rateLimit(clientKey(request, "password"), 5, 300);
    if (!limit.ok) return fail(429, "error.rateLimited", { retryAfter: limit.retryAfterSeconds });

    const input = changePasswordSchema.parse(await readJson(request));

    const current = await getPasswordHash(session.id);
    if (!(await verifyPassword(input.currentPassword, current))) {
      return fail(403, "profile.password.wrong");
    }

    await storePasswordHash(session.id, await hashPassword(input.newPassword));
    return ok({ ok: true });
  } catch (error) {
    return handleError(error);
  }
}
