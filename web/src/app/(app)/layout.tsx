import { redirect } from "next/navigation";
import { AppShell } from "@/components/app-shell";
import { SessionProvider } from "@/components/providers";
import { getSession } from "@/lib/session";
import { getStats } from "@/lib/queries";

export const dynamic = "force-dynamic";

/**
 * Layout for every signed-in page.
 *
 * The session is verified here on the server — middleware only checks that a cookie
 * is present, since the Edge runtime cannot validate the HMAC.
 */
export default async function AppLayout({ children }: { children: React.ReactNode }) {
  const session = await getSession();
  if (!session) redirect("/login");

  const user = {
    id: session.id,
    nom: session.nom,
    email: session.email,
    role: session.role,
  };

  // The overdue count drives the bell badge. A failure here must not take the whole
  // application down, so it degrades to zero.
  let overdue = 0;
  try {
    overdue = (await getStats()).overdue;
  } catch (error) {
    console.error("[layout] stats unavailable", error);
  }

  return (
    <SessionProvider initialUser={user}>
      <AppShell user={user} overdue={overdue}>
        {children}
      </AppShell>
    </SessionProvider>
  );
}
