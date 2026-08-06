import { fail, handleError, ok, readJson } from "@/lib/api";
import { assertDemoMutationAllowed } from "@/lib/db";
import { createReservation, listReaders, listReservations } from "@/lib/queries";
import { assertCsrf, requireAdmin, requireSession } from "@/lib/session";
import { reservationSchema } from "@/lib/validation";
import { clientKey, rateLimit } from "@/lib/rate-limit";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET() {
  try {
    await requireSession();
    const [reservations, readers] = await Promise.all([listReservations(), listReaders()]);
    return ok({ reservations, readers });
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

    const input = reservationSchema.parse(await readJson(request));
    const id = await createReservation(input);
    return ok({ id }, { status: 201 });
  } catch (error) {
    return handleError(error);
  }
}
