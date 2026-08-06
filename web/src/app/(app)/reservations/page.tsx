import type { Metadata } from "next";
import { getSession } from "@/lib/session";
import { listReservations } from "@/lib/queries";
import { ReservationsView } from "./reservations-view";

export const metadata: Metadata = { title: "Réservations" };
export const dynamic = "force-dynamic";

export default async function ReservationsPage() {
  const session = await getSession();
  if (!session) return null;

  return (
    <ReservationsView reservations={await listReservations()} isAdmin={session.role === "Admin"} />
  );
}
