/**
 * Creates the schema and seeds demonstration data.
 *
 * Run once against a fresh Neon branch:
 *   npm run db:seed
 *
 * Safe to re-run: every statement is idempotent and existing rows are left alone.
 * The schema matches what the Java desktop client creates, so both editions share
 * one database.
 */

import { pbkdf2, randomBytes } from "node:crypto";
import { promisify } from "node:util";

import { connect } from "./connection.mjs";

const pbkdf2Async = promisify(pbkdf2);

const sql = connect("db:seed");

// Must match src/lib/password.ts and Security.java.
const ITERATIONS = 210_000;

async function hash(password) {
  const salt = randomBytes(16);
  const derived = await pbkdf2Async(password, salt, ITERATIONS, 32, "sha256");
  return `pbkdf2$${ITERATIONS}$${salt.toString("base64url")}$${derived.toString("base64url")}`;
}

const TABLES = [
  `CREATE TABLE IF NOT EXISTS livre (
     id_livre SERIAL PRIMARY KEY, titre VARCHAR(255) NOT NULL, auteur VARCHAR(255) NOT NULL,
     genre VARCHAR(100), resume_livre TEXT, disponibilite BOOLEAN DEFAULT TRUE,
     image_url TEXT)`,
  `CREATE TABLE IF NOT EXISTS utilisateur (
     id_utilisateur SERIAL PRIMARY KEY, nom VARCHAR(255) NOT NULL, motDePasse VARCHAR(255),
     numero INTEGER, email VARCHAR(255), role_utilisateur VARCHAR(50))`,
  `CREATE TABLE IF NOT EXISTS admin (
     id_admin SERIAL PRIMARY KEY, id_utilisateur INTEGER REFERENCES utilisateur(id_utilisateur) ON DELETE CASCADE)`,
  `CREATE TABLE IF NOT EXISTS lecteur (
     id_lecteur SERIAL PRIMARY KEY, id_utilisateur INTEGER REFERENCES utilisateur(id_utilisateur) ON DELETE CASCADE)`,
  `CREATE TABLE IF NOT EXISTS emprunt (
     id_emprunt SERIAL PRIMARY KEY, id_utilisateur INTEGER, id_livre INTEGER,
     dateEmprunts DATE, dateRetour DATE, date_retour_livre DATE)`,
  `CREATE TABLE IF NOT EXISTS reservation (
     id_reservation SERIAL PRIMARY KEY, id_utilisateur INTEGER, dateReservation DATE)`,
];

// Each is optional: a failure means it is already applied, or existing data blocks it.
const MIGRATIONS = [
  `ALTER TABLE emprunt ADD COLUMN IF NOT EXISTS date_retour_livre DATE`,
  // Cover artwork, resolved from Open Library / Google Books by `npm run db:covers`.
  // Null means "no artwork found" — both clients then draw the generated gradient.
  `ALTER TABLE livre ADD COLUMN IF NOT EXISTS image_url TEXT`,
  `ALTER TABLE emprunt ADD CONSTRAINT emprunt_livre_fk
     FOREIGN KEY (id_livre) REFERENCES livre(id_livre) ON DELETE CASCADE`,
  `ALTER TABLE emprunt ADD CONSTRAINT emprunt_user_fk
     FOREIGN KEY (id_utilisateur) REFERENCES utilisateur(id_utilisateur) ON DELETE CASCADE`,
  `ALTER TABLE reservation ADD CONSTRAINT reservation_user_fk
     FOREIGN KEY (id_utilisateur) REFERENCES utilisateur(id_utilisateur) ON DELETE CASCADE`,
  `CREATE UNIQUE INDEX IF NOT EXISTS utilisateur_nom_unique ON utilisateur (LOWER(nom))`,
  `CREATE INDEX IF NOT EXISTS emprunt_livre_idx ON emprunt (id_livre)`,
  `CREATE INDEX IF NOT EXISTS emprunt_user_idx ON emprunt (id_utilisateur)`,
  `CREATE INDEX IF NOT EXISTS reservation_user_idx ON reservation (id_utilisateur)`,
  `CREATE INDEX IF NOT EXISTS livre_titre_idx ON livre (LOWER(titre))`,
  `CREATE INDEX IF NOT EXISTS livre_auteur_idx ON livre (LOWER(auteur))`,
];

const BOOKS = [
  ["La Boîte à Merveilles", "Ahmed Sefrioui", "Autobiographie",
   "Le récit de l'enfance de l'auteur dans la médina de Fès, entre superstitions, voisinage bruyant et émerveillement d'un enfant de six ans.", true],
  ["Antigone", "Jean Anouilh", "Tragédie",
   "Antigone brave l'interdit de Créon pour enterrer son frère, et choisit la mort plutôt que le compromis.", true],
  ["Le Dernier Jour d'un Condamné", "Victor Hugo", "Roman à thèse",
   "Le monologue d'un homme dans ses dernières heures, écrit comme un réquisitoire contre la peine capitale.", false],
  ["L'Étranger", "Albert Camus", "Roman",
   "Meursault enterre sa mère sans pleurer, tue un homme sous le soleil d'Alger, et se fait juger pour son indifférence autant que pour son crime.", true],
  ["Les Misérables", "Victor Hugo", "Roman historique",
   "De Jean Valjean au bagne aux barricades de 1832 : une fresque de la misère et de la rédemption dans la France du XIXe siècle.", true],
  ["Harry Potter à l'école des sorciers", "J.K. Rowling", "Fantastique",
   "Un orphelin découvre le jour de ses onze ans qu'il est sorcier, et pousse pour la première fois les portes de Poudlard.", false],
  ["Le Petit Prince", "Antoine de Saint-Exupéry", "Conte",
   "Un aviateur échoué dans le désert rencontre un enfant venu d'une autre planète, qui lui réapprend à voir l'essentiel.", true],
  ["Candide", "Voltaire", "Conte philosophique",
   "Chassé du paradis d'un château westphalien, Candide traverse un monde absurde et brutal en répétant que tout va pour le mieux.", true],
  ["Le Rouge et le Noir", "Stendhal", "Roman",
   "Julien Sorel, fils de charpentier, gravit la société de la Restauration par l'ambition et le calcul — jusqu'au vertige.", true],
  ["Madame Bovary", "Gustave Flaubert", "Roman",
   "Emma Bovary cherche dans les romans une vie que sa province ne lui donnera jamais, et s'y perd.", true],
];

async function main() {
  console.log("Creating schema…");
  // `sql.query` is the plain-string form; the tagged template is for parameterised
  // queries and rejects a bare string.
  for (const statement of TABLES) await sql.query(statement);

  // Migrations are individually optional — most fail simply because they are already
  // applied. Failures are reported rather than swallowed: a silently missing column
  // surfaces much later as a confusing runtime error on the dashboard.
  for (const statement of MIGRATIONS) {
    try {
      await sql.query(statement);
    } catch (error) {
      const alreadyApplied = /already exists|duplicate/i.test(error.message);
      if (!alreadyApplied) {
        console.warn(`  skipped: ${statement.split("\n")[0].trim()}\n    ↳ ${error.message}`);
      }
    }
  }

  const [{ count: bookCount }] = await sql`SELECT COUNT(*)::int AS count FROM livre`;
  if (bookCount === 0) {
    console.log(`Seeding ${BOOKS.length} books…`);
    for (const [titre, auteur, genre, resume, dispo] of BOOKS) {
      await sql`
        INSERT INTO livre (titre, auteur, genre, resume_livre, disponibilite)
        VALUES (${titre}, ${auteur}, ${genre}, ${resume}, ${dispo})
      `;
    }
  } else {
    console.log(`Catalogue already has ${bookCount} books — leaving it alone.`);
  }

  await ensureAccount("admin", "admin123", "admin@fsts.ac.ma", 612345678, "Admin");
  await ensureAccount("lecteur", "lecteur123", "lecteur@fsts.ac.ma", 987654321, "Lecteur");

  console.log("\nDone. Demo accounts:");
  console.log("  admin   / admin123    (administrator)");
  console.log("  lecteur / lecteur123  (reader)");
}

async function ensureAccount(nom, password, email, numero, role) {
  const existing = await sql`SELECT id_utilisateur FROM utilisateur WHERE LOWER(nom) = LOWER(${nom})`;
  if (existing.length > 0) {
    console.log(`Account "${nom}" already exists — leaving it alone.`);
    return;
  }

  const stored = await hash(password);
  const rows = await sql`
    INSERT INTO utilisateur (nom, motDePasse, numero, email, role_utilisateur)
    VALUES (${nom}, ${stored}, ${numero}, ${email}, ${role})
    RETURNING id_utilisateur
  `;
  const id = rows[0].id_utilisateur;

  if (role === "Admin") {
    await sql`INSERT INTO admin (id_utilisateur) VALUES (${id})`;
  } else {
    await sql`INSERT INTO lecteur (id_utilisateur) VALUES (${id})`;
  }
  console.log(`Created account "${nom}".`);
}

main().catch((error) => {
  console.error("Seed failed:", error.message);
  process.exit(1);
});
