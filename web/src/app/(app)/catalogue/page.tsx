import type { Metadata } from "next";
import { getSession } from "@/lib/session";
import { listBooks, listGenres } from "@/lib/queries";
import { CatalogueView } from "./catalogue-view";

export const metadata: Metadata = { title: "Catalogue" };
export const dynamic = "force-dynamic";

export default async function CataloguePage() {
  const session = await getSession();
  if (!session) return null;

  const [books, genres] = await Promise.all([listBooks(), listGenres()]);
  return <CatalogueView books={books} genres={genres} isAdmin={session.role === "Admin"} />;
}
