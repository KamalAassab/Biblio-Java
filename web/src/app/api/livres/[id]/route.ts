import { fail, handleError, ok, readJson } from "@/lib/api";
import { assertDemoMutationAllowed } from "@/lib/db";
import { deleteBook, updateBook } from "@/lib/queries";
import { assertCsrf, requireAdmin } from "@/lib/session";
import { bookSchema, idSchema } from "@/lib/validation";
import { clientKey, rateLimit } from "@/lib/rate-limit";

export const runtime = "nodejs";

type Context = { params: Promise<{ id: string }> };

export async function PUT(request: Request, { params }: Context) {
  try {
    await requireAdmin();
    await assertCsrf(request);
    assertDemoMutationAllowed();

    const limit = rateLimit(clientKey(request, "write"), 40, 60);
    if (!limit.ok) return fail(429, "error.rateLimited", { retryAfter: limit.retryAfterSeconds });

    const id = idSchema.parse((await params).id);
    const input = bookSchema.parse(await readJson(request));

    if (!(await updateBook(id, input))) return fail(404, "error.notFound");
    return ok({ ok: true });
  } catch (error) {
    return handleError(error);
  }
}

export async function DELETE(request: Request, { params }: Context) {
  try {
    await requireAdmin();
    await assertCsrf(request);
    assertDemoMutationAllowed();

    const limit = rateLimit(clientKey(request, "write"), 40, 60);
    if (!limit.ok) return fail(429, "error.rateLimited", { retryAfter: limit.retryAfterSeconds });

    const id = idSchema.parse((await params).id);
    if (!(await deleteBook(id))) return fail(404, "error.notFound");
    return ok({ ok: true });
  } catch (error) {
    return handleError(error);
  }
}
