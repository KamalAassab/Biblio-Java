import type { Metadata } from "next";
import { getSession } from "@/lib/session";
import { listLoans } from "@/lib/queries";
import { LoansView } from "./loans-view";

export const metadata: Metadata = { title: "Emprunts" };
export const dynamic = "force-dynamic";

export default async function LoansPage() {
  const session = await getSession();
  if (!session) return null;

  return <LoansView loans={await listLoans()} isAdmin={session.role === "Admin"} />;
}
