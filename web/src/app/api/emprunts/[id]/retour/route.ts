import { fail, handleError, ok } from "@/lib/api";
import { assertDemoMutationAllowed } from "@/lib/db";
import { returnLoan } from "@/lib/queries";
import { assertCsrf, requireAdmin } from "@/lib/session";
import { idSchema } from "@/lib/validation";
import { clientKey, rateLimit } from "@/lib/rate-limit";

export const runtime = "nodejs";

type Context = { params: Promise<{ id: string }> };

export async function POST(request: Request, { params }: Context) {
  try {
    await requireAdmin();
    await assertCsrf(request);
    assertDemoMutationAllowed();

    const limit = rateLimit(clientKey(request, "write"), 40, 60);
    if (!limit.ok) return fail(429, "error.rateLimited", { retryAfter: limit.retryAfterSeconds });

    const id = idSchema.parse((await params).id);
    const result = await returnLoan(id);
    if (!result.ok) return fail(404, "error.notFound");
    return ok({ ok: true, title: result.title });
  } catch (error) {
    return handleError(error);
  }
}
