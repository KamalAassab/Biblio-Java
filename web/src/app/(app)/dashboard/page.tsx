import type { Metadata } from "next";
import { getSession } from "@/lib/session";
import { getStats, listBooks } from "@/lib/queries";
import { DashboardView } from "./dashboard-view";

export const metadata: Metadata = { title: "Tableau de bord" };
export const dynamic = "force-dynamic";

export default async function DashboardPage() {
  const session = await getSession();
  // The layout already guaranteed a session; this narrows the type.
  if (!session) return null;

  // Rendered on the server so the first paint carries real data rather than skeletons.
  const [stats, books] = await Promise.all([getStats(), listBooks()]);

  return (
    <DashboardView
      name={session.nom}
      isAdmin={session.role === "Admin"}
      stats={stats}
      recent={books.slice(0, 12)}
    />
  );
}
