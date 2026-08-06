import { sql } from "@/lib/db";

/**
 * Typed data access.
 *
 * Every value is interpolated through the `sql` tagged template, which sends it as a
 * bound parameter — string concatenation into SQL never happens here.
 *
 * The table and column names are the ones the Java desktop client created, so both
 * applications read and write the same rows.
 */

export interface Book {
  id: number;
  titre: string;
  auteur: string;
  genre: string | null;
  resume: string | null;
  disponibilite: boolean;
  /**
   * Cover artwork, resolved by `npm run db:covers` and served by Open Library or
   * Google Books. Null when no match was found — the clients then draw the
   * generated gradient cover instead.
   */
  imageUrl: string | null;
}

export interface Member {
  id: number;
  nom: string;
  email: string | null;
  numero: number | null;
  role: "Admin" | "Lecteur";
}

export interface Loan {
  id: number;
  idUtilisateur: number;
  lecteur: string;
  idLivre: number;
  livre: string;
  dateEmprunt: string;
  dateRetour: string | null;
  dateRetourLivre: string | null;
}

export interface Reservation {
  id: number;
  idUtilisateur: number;
  lecteur: string;
  dateReservation: string;
}

export interface Stats {
  books: number;
  available: number;
  activeLoans: number;
  overdue: number;
  reservations: number;
  members: number;
}

// ── Books ──────────────────────────────────────────────────────────────────────

export async function listBooks(): Promise<Book[]> {
  const rows = await sql`
    SELECT id_livre, titre, auteur, genre, resume_livre, image_url,
           COALESCE(disponibilite, TRUE) AS dispo
    FROM livre
    ORDER BY id_livre DESC
  `;
  return rows.map(toBook);
}

export async function getBook(id: number): Promise<Book | null> {
  const rows = await sql`
    SELECT id_livre, titre, auteur, genre, resume_livre, image_url,
           COALESCE(disponibilite, TRUE) AS dispo
    FROM livre WHERE id_livre = ${id}
  `;
  return rows.length ? toBook(rows[0]) : null;
}

export async function createBook(input: {
  titre: string;
  auteur: string;
  genre: string;
  resume: string;
  disponibilite: boolean;
}): Promise<number> {
  const rows = await sql`
    INSERT INTO livre (titre, auteur, genre, resume_livre, disponibilite)
    VALUES (${input.titre}, ${input.auteur}, ${input.genre}, ${input.resume}, ${input.disponibilite})
    RETURNING id_livre
  `;
  return rows[0].id_livre as number;
}

export async function updateBook(
  id: number,
  input: { titre: string; auteur: string; genre: string; resume: string; disponibilite: boolean },
): Promise<boolean> {
  const rows = await sql`
    UPDATE livre
    SET titre = ${input.titre}, auteur = ${input.auteur}, genre = ${input.genre},
        resume_livre = ${input.resume}, disponibilite = ${input.disponibilite}
    WHERE id_livre = ${id}
    RETURNING id_livre
  `;
  return rows.length > 0;
}

export async function deleteBook(id: number): Promise<boolean> {
  const rows = await sql`DELETE FROM livre WHERE id_livre = ${id} RETURNING id_livre`;
  return rows.length > 0;
}

export async function listGenres(): Promise<string[]> {
  const rows = await sql`
    SELECT genre FROM livre
    WHERE genre IS NOT NULL AND genre <> ''
    GROUP BY genre
    ORDER BY COUNT(*) DESC, genre
  `;
  return rows.map((r) => r.genre as string);
}

// ── Members ────────────────────────────────────────────────────────────────────

export async function listMembers(): Promise<Member[]> {
  // Password hashes are deliberately not selected — no view needs them.
  const rows = await sql`
    SELECT id_utilisateur, nom, email, numero, role_utilisateur
    FROM utilisateur ORDER BY id_utilisateur
  `;
  return rows.map(toMember);
}

export async function listReaders(): Promise<Member[]> {
  const rows = await sql`
    SELECT id_utilisateur, nom, email, numero, role_utilisateur
    FROM utilisateur
    WHERE role_utilisateur IS DISTINCT FROM 'Admin'
    ORDER BY nom
  `;
  return rows.map(toMember);
}

export async function findMemberForAuth(
  nom: string,
): Promise<{ id: number; nom: string; email: string | null; role: string; hash: string | null } | null> {
  const rows = await sql`
    SELECT id_utilisateur, nom, email, role_utilisateur, motDePasse
    FROM utilisateur WHERE LOWER(nom) = LOWER(${nom}) LIMIT 1
  `;
  if (!rows.length) return null;
  const row = rows[0];
  return {
    id: row.id_utilisateur as number,
    nom: row.nom as string,
    email: (row.email as string) ?? null,
    role: (row.role_utilisateur as string) ?? "Lecteur",
    hash: (row.motdepasse as string) ?? null,
  };
}

export async function storePasswordHash(id: number, hash: string): Promise<void> {
  await sql`UPDATE utilisateur SET motDePasse = ${hash} WHERE id_utilisateur = ${id}`;
}

export async function getPasswordHash(id: number): Promise<string | null> {
  const rows = await sql`SELECT motDePasse FROM utilisateur WHERE id_utilisateur = ${id}`;
  return rows.length ? ((rows[0].motdepasse as string) ?? null) : null;
}

export async function updateProfile(
  id: number,
  input: { nom: string; email: string; numero: number },
): Promise<boolean> {
  const rows = await sql`
    UPDATE utilisateur
    SET nom = ${input.nom}, email = ${input.email}, numero = ${input.numero}
    WHERE id_utilisateur = ${id}
    RETURNING id_utilisateur
  `;
  return rows.length > 0;
}

export async function createMember(input: {
  nom: string;
  email: string;
  numero: number;
  hash: string;
  role: "Admin" | "Lecteur";
}): Promise<number> {
  const rows = await sql`
    INSERT INTO utilisateur (nom, motDePasse, numero, email, role_utilisateur)
    VALUES (${input.nom}, ${input.hash}, ${input.numero}, ${input.email}, ${input.role})
    RETURNING id_utilisateur
  `;
  const id = rows[0].id_utilisateur as number;

  // Mirror the row into the role table the desktop client's schema defines.
  if (input.role === "Admin") {
    await sql`INSERT INTO admin (id_utilisateur) VALUES (${id})`;
  } else {
    await sql`INSERT INTO lecteur (id_utilisateur) VALUES (${id})`;
  }
  return id;
}

export async function deleteMember(id: number): Promise<boolean> {
  const rows = await sql`DELETE FROM utilisateur WHERE id_utilisateur = ${id} RETURNING id_utilisateur`;
  return rows.length > 0;
}

export async function getMemberName(id: number): Promise<string | null> {
  const rows = await sql`SELECT nom FROM utilisateur WHERE id_utilisateur = ${id}`;
  return rows.length ? (rows[0].nom as string) : null;
}

// ── Loans ──────────────────────────────────────────────────────────────────────

export async function listLoans(): Promise<Loan[]> {
  const rows = await sql`
    SELECT e.id_emprunt, u.id_utilisateur, u.nom, l.id_livre, l.titre,
           e.dateEmprunts, e.dateRetour, e.date_retour_livre
    FROM emprunt e
    JOIN utilisateur u ON e.id_utilisateur = u.id_utilisateur
    JOIN livre l ON e.id_livre = l.id_livre
    ORDER BY e.id_emprunt DESC
  `;
  return rows.map((r) => ({
    id: r.id_emprunt as number,
    idUtilisateur: r.id_utilisateur as number,
    lecteur: r.nom as string,
    idLivre: r.id_livre as number,
    livre: r.titre as string,
    dateEmprunt: isoDate(r.dateemprunts),
    dateRetour: r.dateretour ? isoDate(r.dateretour) : null,
    dateRetourLivre: r.date_retour_livre ? isoDate(r.date_retour_livre) : null,
  }));
}

/**
 * Records a loan and marks the book unavailable.
 *
 * Both writes must land together — a loan against a book still flagged available would
 * let the same copy be lent twice. `SELECT … FOR UPDATE` locks the row for the duration.
 */
export async function createLoan(input: {
  idUtilisateur: number;
  idLivre: number;
  dateRetour: string;
}): Promise<{ ok: true; id: number } | { ok: false; reason: "unavailable" }> {
  const result = await sql.transaction((tx) => [
    tx`SELECT COALESCE(disponibilite, TRUE) AS dispo FROM livre WHERE id_livre = ${input.idLivre} FOR UPDATE`,
    tx`INSERT INTO emprunt (id_utilisateur, id_livre, dateEmprunts, dateRetour)
       SELECT ${input.idUtilisateur}, ${input.idLivre}, CURRENT_DATE, ${input.dateRetour}::date
       WHERE EXISTS (
         SELECT 1 FROM livre WHERE id_livre = ${input.idLivre} AND COALESCE(disponibilite, TRUE)
       )
       RETURNING id_emprunt`,
    tx`UPDATE livre SET disponibilite = FALSE WHERE id_livre = ${input.idLivre}`,
  ]);

  const inserted = result[1] as Record<string, unknown>[];
  if (!inserted.length) return { ok: false, reason: "unavailable" };
  return { ok: true, id: inserted[0].id_emprunt as number };
}

export async function returnLoan(
  id: number,
): Promise<{ ok: true; title: string } | { ok: false }> {
  const open = await sql`
    SELECT e.id_livre, l.titre FROM emprunt e
    JOIN livre l ON l.id_livre = e.id_livre
    WHERE e.id_emprunt = ${id} AND e.date_retour_livre IS NULL
  `;
  if (!open.length) return { ok: false };

  const bookId = open[0].id_livre as number;
  await sql.transaction((tx) => [
    tx`UPDATE emprunt SET date_retour_livre = CURRENT_DATE WHERE id_emprunt = ${id}`,
    tx`UPDATE livre SET disponibilite = TRUE WHERE id_livre = ${bookId}`,
  ]);
  return { ok: true, title: open[0].titre as string };
}

// ── Reservations ───────────────────────────────────────────────────────────────

export async function listReservations(): Promise<Reservation[]> {
  const rows = await sql`
    SELECT r.id_reservation, u.id_utilisateur, u.nom, r.dateReservation
    FROM reservation r
    JOIN utilisateur u ON r.id_utilisateur = u.id_utilisateur
    ORDER BY r.id_reservation DESC
  `;
  return rows.map((r) => ({
    id: r.id_reservation as number,
    idUtilisateur: r.id_utilisateur as number,
    lecteur: r.nom as string,
    dateReservation: isoDate(r.datereservation),
  }));
}

export async function createReservation(input: {
  idUtilisateur: number;
  dateReservation: string;
}): Promise<number> {
  const rows = await sql`
    INSERT INTO reservation (id_utilisateur, dateReservation)
    VALUES (${input.idUtilisateur}, ${input.dateReservation}::date)
    RETURNING id_reservation
  `;
  return rows[0].id_reservation as number;
}

export async function deleteReservation(id: number): Promise<boolean> {
  const rows = await sql`DELETE FROM reservation WHERE id_reservation = ${id} RETURNING id_reservation`;
  return rows.length > 0;
}

// ── Aggregates ─────────────────────────────────────────────────────────────────

export async function getStats(): Promise<Stats> {
  const rows = await sql`
    SELECT
      (SELECT COUNT(*) FROM livre) AS books,
      (SELECT COUNT(*) FROM livre WHERE COALESCE(disponibilite, TRUE)) AS available,
      (SELECT COUNT(*) FROM emprunt WHERE date_retour_livre IS NULL) AS active_loans,
      (SELECT COUNT(*) FROM emprunt WHERE date_retour_livre IS NULL AND dateRetour < CURRENT_DATE) AS overdue,
      (SELECT COUNT(*) FROM reservation) AS reservations,
      (SELECT COUNT(*) FROM utilisateur) AS members
  `;
  const r = rows[0];
  return {
    books: Number(r.books),
    available: Number(r.available),
    activeLoans: Number(r.active_loans),
    overdue: Number(r.overdue),
    reservations: Number(r.reservations),
    members: Number(r.members),
  };
}

export async function countForMember(id: number): Promise<{ loans: number; reservations: number }> {
  const rows = await sql`
    SELECT
      (SELECT COUNT(*) FROM emprunt WHERE id_utilisateur = ${id}) AS loans,
      (SELECT COUNT(*) FROM reservation WHERE id_utilisateur = ${id}) AS reservations
  `;
  return { loans: Number(rows[0].loans), reservations: Number(rows[0].reservations) };
}

// ── Row mapping ────────────────────────────────────────────────────────────────

/**
 * PostgreSQL folds unquoted identifiers to lower case, so the driver returns
 * `resume_livre` and `motdepasse` regardless of how the column was written in the DDL.
 */
function toBook(row: Record<string, unknown>): Book {
  return {
    id: row.id_livre as number,
    titre: row.titre as string,
    auteur: row.auteur as string,
    genre: (row.genre as string) ?? null,
    resume: (row.resume_livre as string) ?? null,
    disponibilite: Boolean(row.dispo ?? row.disponibilite ?? true),
    imageUrl: (row.image_url as string) || null,
  };
}

function toMember(row: Record<string, unknown>): Member {
  const role = (row.role_utilisateur as string) === "Admin" ? "Admin" : "Lecteur";
  return {
    id: row.id_utilisateur as number,
    nom: row.nom as string,
    email: (row.email as string) ?? null,
    numero: (row.numero as number) ?? null,
    role,
  };
}

function isoDate(value: unknown): string {
  if (value instanceof Date) {
    // A DATE column has no time zone, and the driver hands it back as *local*
    // midnight. `toISOString()` converts to UTC first, so anywhere east of Greenwich
    // local midnight is the previous day in UTC and every date renders a day early —
    // a loan due today reads as one day overdue. Read the local calendar fields
    // instead, which is what the column actually meant.
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, "0");
    const day = String(value.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }
  return String(value).slice(0, 10);
}
