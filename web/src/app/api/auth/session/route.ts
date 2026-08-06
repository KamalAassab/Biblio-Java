import { handleError, ok } from "@/lib/api";
import { getSession } from "@/lib/session";

export const runtime = "nodejs";

/** Returns the current session, or `{ user: null }` when signed out. */
export async function GET() {
  try {
    const session = await getSession();
    if (!session) return ok({ user: null });
    const { id, nom, email, role } = session;
    return ok({ user: { id, nom, email, role } });
  } catch (error) {
    return handleError(error);
  }
}
