import type { Metadata } from "next";
import { getSession } from "@/lib/session";
import { countForMember, listMembers } from "@/lib/queries";
import { ProfileView } from "./profile-view";

export const metadata: Metadata = { title: "Mon profil" };
export const dynamic = "force-dynamic";

export default async function ProfilePage() {
  const session = await getSession();
  if (!session) return null;

  const [activity, members] = await Promise.all([countForMember(session.id), listMembers()]);
  // The session carries only what was true at sign-in; read the current row for
  // the phone number, which the session does not store.
  const me = members.find((member) => member.id === session.id);

  return (
    <ProfileView
      user={{
        id: session.id,
        nom: session.nom,
        email: session.email,
        role: session.role,
        numero: me?.numero ?? 0,
      }}
      activity={activity}
    />
  );
}
