import { handleError, ok, readJson } from "@/lib/api";
import { assertDemoMutationAllowed } from "@/lib/db";
import { createBook, listBooks, listGenres } from "@/lib/queries";
import { assertCsrf, requireAdmin, requireSession } from "@/lib/session";
import { bookSchema } from "@/lib/validation";
import { clientKey, rateLimit } from "@/lib/rate-limit";
import { fail } from "@/lib/api";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET() {
  try {
    await requireSession();
    const [books, genres] = await Promise.all([listBooks(), listGenres()]);
    return ok({ books, genres });
  } catch (error) {
    return handleError(error);
  }
}

export async function POST(request: Request) {
  try {
    await requireAdmin();
    await assertCsrf(request);
    assertDemoMutationAllowed();

    const limit = rateLimit(clientKey(request, "write"), 40, 60);
    if (!limit.ok) return fail(429, "error.rateLimited", { retryAfter: limit.retryAfterSeconds });

    const input = bookSchema.parse(await readJson(request));
    const id = await createBook(input);
    return ok({ id }, { status: 201 });
  } catch (error) {
    return handleError(error);
  }
}
