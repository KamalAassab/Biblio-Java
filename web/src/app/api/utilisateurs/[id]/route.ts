import { fail, handleError, ok } from "@/lib/api";
import { assertDemoMutationAllowed } from "@/lib/db";
import { deleteMember, getMemberName } from "@/lib/queries";
import { assertCsrf, requireAdmin } from "@/lib/session";
import { idSchema } from "@/lib/validation";
import { clientKey, rateLimit } from "@/lib/rate-limit";

export const runtime = "nodejs";

type Context = { params: Promise<{ id: string }> };

export async function DELETE(request: Request, { params }: Context) {
  try {
    const session = await requireAdmin();
    await assertCsrf(request);

    const limit = rateLimit(clientKey(request, "write"), 20, 60);
    if (!limit.ok) return fail(429, "error.rateLimited", { retryAfter: limit.retryAfterSeconds });

    const id = idSchema.parse((await params).id);

    // Deleting your own account would invalidate the session you are still using.
    if (id === session.id) return fail(400, "error.cannotDeleteSelf");

    // On the public demo, the seeded accounts must survive.
    const name = await getMemberName(id);
    assertDemoMutationAllowed(name);

    if (!(await deleteMember(id))) return fail(404, "error.notFound");
    return ok({ ok: true });
  } catch (error) {
    return handleError(error);
  }
}
