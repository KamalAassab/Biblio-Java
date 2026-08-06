import { fail, handleError, ok, readJson } from "@/lib/api";
import { assertDemoMutationAllowed } from "@/lib/db";
import { hashPassword } from "@/lib/password";
import { createMember, listMembers } from "@/lib/queries";
import { assertCsrf, requireAdmin, requireSession } from "@/lib/session";
import { userSchema } from "@/lib/validation";
import { clientKey, rateLimit } from "@/lib/rate-limit";

export const maxDuration = 30;
export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET() {
  try {
    // Any signed-in user may see the directory; only admins can change it.
    await requireSession();
    return ok({ members: await listMembers() });
  } catch (error) {
    return handleError(error);
  }
}

export async function POST(request: Request) {
  try {
    await requireAdmin();
    await assertCsrf(request);
    assertDemoMutationAllowed();

    const limit = rateLimit(clientKey(request, "write"), 20, 60);
    if (!limit.ok) return fail(429, "error.rateLimited", { retryAfter: limit.retryAfterSeconds });

    const input = userSchema.parse(await readJson(request));
    const hash = await hashPassword(input.motDePasse);
    const id = await createMember({
      nom: input.nom,
      email: input.email,
      numero: input.numero,
      hash,
      role: input.role,
    });
    return ok({ id }, { status: 201 });
  } catch (error) {
    // A duplicate username trips the unique index; report it as a field error
    // rather than a generic 500.
    if (error instanceof Error && /duplicate key|unique/i.test(error.message)) {
      return fail(409, "error.username.taken");
    }
    return handleError(error);
  }
}
