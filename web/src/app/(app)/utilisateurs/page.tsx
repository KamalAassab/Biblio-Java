import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { listMembers } from "@/lib/queries";
import { MembersView } from "./members-view";

export const metadata: Metadata = { title: "Utilisateurs" };
export const dynamic = "force-dynamic";

export default async function MembersPage() {
  const session = await getSession();
  if (!session) return null;

  // The sidebar hides this entry for readers, but a direct URL must be blocked too.
  if (session.role !== "Admin") redirect("/dashboard");

  return <MembersView members={await listMembers()} currentUserId={session.id} />;
}
