import { handleError, ok } from "@/lib/api";
import { getStats, listBooks } from "@/lib/queries";
import { requireSession } from "@/lib/session";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

/** Dashboard payload: counters plus the shelf of recent additions, in one round trip. */
export async function GET() {
  try {
    await requireSession();
    const [stats, books] = await Promise.all([getStats(), listBooks()]);
    return ok({ stats, recent: books.slice(0, 12) });
  } catch (error) {
    return handleError(error);
  }
}
