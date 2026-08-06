import { fail, handleError, ok, readJson } from "@/lib/api";
import { assertDemoMutationAllowed } from "@/lib/db";
import { createLoan, listLoans, listReaders } from "@/lib/queries";
import { assertCsrf, requireAdmin, requireSession } from "@/lib/session";
import { loanSchema } from "@/lib/validation";
import { clientKey, rateLimit } from "@/lib/rate-limit";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET() {
  try {
    await requireSession();
    const [loans, readers] = await Promise.all([listLoans(), listReaders()]);
    return ok({ loans, readers });
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

    const input = loanSchema.parse(await readJson(request));
    const result = await createLoan(input);
    if (!result.ok) return fail(409, "error.book.unavailable");
    return ok({ id: result.id }, { status: 201 });
  } catch (error) {
    return handleError(error);
  }
}
