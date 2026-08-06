import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";

/** The root simply routes to the right place; there is no separate landing page. */
export default async function Home() {
  const session = await getSession();
  redirect(session ? "/dashboard" : "/login");
}
