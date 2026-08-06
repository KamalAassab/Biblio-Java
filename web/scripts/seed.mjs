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

/**
 * Append the ten demo loan scenarios even when the register already has rows.
 *   npm run db:seed -- --scenarios
 * Without it, an existing register is left untouched.
 */
const SEED_SCENARIOS = process.argv.includes("--scenarios");

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

/** Primary keys that need a sequence attached. Mirrors `installSequences` in
 *  DatabaseConnection.java — see `ensureSequences` below for why both exist. */
const PRIMARY_KEYS = [
  ["livre", "id_livre"],
  ["utilisateur", "id_utilisateur"],
  ["admin", "id_admin"],
  ["lecteur", "id_lecteur"],
  ["emprunt", "id_emprunt"],
  ["reservation", "id_reservation"],
];

/**
 * Gives every primary key a sequence default.
 *
 * The original coursework schema declared plain INTEGER keys and generated ids with
 * `MAX(id) + 1`, which loses writes under concurrency. The desktop client repairs this
 * on connect, but a database the desktop client has never reached still has bare
 * columns with no default — and then *every* insert from the web app fails with a
 * not-null violation on the id. Seeding must not depend on the Java app having been
 * run first, so the same repair happens here.
 *
 * `setval(…, max + 1, false)` makes the next `nextval` return exactly max + 1, so
 * existing rows keep their ids.
 */
async function ensureSequences() {
  for (const [table, column] of PRIMARY_KEYS) {
    const sequence = `${table}_${column}_seq`;
    try {
      await sql.query(`CREATE SEQUENCE IF NOT EXISTS ${sequence}`);
      await sql.query(
        `SELECT setval('${sequence}', COALESCE((SELECT MAX(${column}) FROM ${table}), 0) + 1, false)`,
      );
      await sql.query(
        `ALTER TABLE ${table} ALTER COLUMN ${column} SET DEFAULT nextval('${sequence}')`,
      );
      await sql.query(`ALTER SEQUENCE ${sequence} OWNED BY ${table}.${column}`);
    } catch (error) {
      console.warn(`  could not attach a sequence to ${table}.${column}: ${error.message}`);
    }
  }
}

/** Readers beyond the two demo accounts, so the register shows a range of names. */
const EXTRA_READERS = [
  ["Salma Bennani", "salma.bennani@fsts.ac.ma", 661234501],
  ["Youssef El Amrani", "youssef.elamrani@fsts.ac.ma", 661234502],
  ["Nadia Cherkaoui", "nadia.cherkaoui@fsts.ac.ma", 661234503],
  ["Omar Tazi", "omar.tazi@fsts.ac.ma", 661234504],
];

/**
 * The ten loan situations, as day offsets from the day the seed runs.
 *
 * `returned: null` means the book is still out. Each row is a state the interface
 * renders differently, so the demo exercises every badge and every filter.
 */
const LOAN_SCENARIOS = [
  { label: "Opened today — full loan period remaining", borrowed: 0, due: 14, returned: null },
  { label: "On loan — a week left", borrowed: -7, due: 7, returned: null },
  { label: "Due tomorrow", borrowed: -13, due: 1, returned: null },
  { label: "Due today", borrowed: -14, due: 0, returned: null },
  { label: "Overdue by 3 days", borrowed: -17, due: -3, returned: null },
  { label: "Overdue by 5 weeks — the worst case on the dashboard", borrowed: -49, due: -35, returned: null },
  { label: "Returned a week early", borrowed: -20, due: -6, returned: -13 },
  { label: "Returned exactly on the due date", borrowed: -28, due: -14, returned: -14 },
  { label: "Returned 8 days late", borrowed: -40, due: -26, returned: -18 },
  { label: "Closed historic loan — last term", borrowed: -120, due: -106, returned: -104 },
];

/** Reservation dates, as day offsets. A spread of recent requests. */
const RESERVATION_OFFSETS = [0, -1, -3, -6, -10, -15, -22];

/** `YYYY-MM-DD` for today shifted by `days`, in local time. */
function dayOffset(days) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

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

  console.log("Attaching primary-key sequences…");
  await ensureSequences();

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

  const adminId = await ensureAccount("admin", "admin123", "admin@fsts.ac.ma", 612345678, "Admin");
  const lecteurId = await ensureAccount("lecteur", "lecteur123", "lecteur@fsts.ac.ma", 987654321, "Lecteur");

  // Extra readers so the loan register is not a single name repeated ten times.
  const readerIds = [];
  for (const [nom, email, numero] of EXTRA_READERS) {
    readerIds.push(await ensureAccount(nom, "lecteur123", email, numero, "Lecteur"));
  }

  await seedScenarios([lecteurId, ...readerIds], adminId);

  console.log("\nDone. Demo accounts:");
  console.log("  admin   / admin123    (administrator)");
  console.log("  lecteur / lecteur123  (reader)");
  console.log(`  ${EXTRA_READERS.length} further readers, all with password lecteur123`);
}

/**
 * Seeds ten loan situations and a set of reservations.
 *
 * Every date is relative to the day the seed runs, so "overdue by three days" stays
 * overdue whenever the demo is opened rather than drifting into the distant past.
 * The ten cover each state the interface renders differently: on loan, due soon, due
 * today, mildly overdue, badly overdue, returned early, returned on time, returned
 * late, a loan opened today, and a closed historic loan.
 */
async function seedScenarios(readerIds, adminId) {
  const [{ count: loanCount }] = await sql`SELECT COUNT(*)::int AS count FROM emprunt`;

  // On a register that already has rows, adding ten more silently would be rude —
  // those rows may be someone's real testing. Appending is opt-in.
  if (loanCount > 0 && !SEED_SCENARIOS) {
    console.log(
      `Register already has ${loanCount} loan(s) — leaving it alone.\n` +
        "  Add the ten demo scenarios alongside them with: npm run db:seed -- --scenarios",
    );
    return;
  }

  // Only books with no open loan, so a scenario can never double-book a title.
  //
  // The register is the authority here, not `livre.disponibilite`: the flag is
  // denormalised and can be stale — a book left marked available while still out is
  // exactly the case that would otherwise be lent twice.
  const books = await sql`
    SELECT l.id_livre FROM livre l
    WHERE NOT EXISTS (
      SELECT 1 FROM emprunt e
      WHERE e.id_livre = l.id_livre AND e.date_retour_livre IS NULL
    )
    ORDER BY l.id_livre
    LIMIT ${LOAN_SCENARIOS.length}
  `;
  if (books.length < LOAN_SCENARIOS.length) {
    console.warn(`Only ${books.length} available book(s) — seeding that many scenarios.`);
  }

  const total = Math.min(books.length, LOAN_SCENARIOS.length);
  console.log(`\nSeeding ${total} loan scenarios…`);

  for (let i = 0; i < total; i++) {
    const { label, borrowed, due, returned } = LOAN_SCENARIOS[i];
    const bookId = books[i].id_livre;
    const readerId = readerIds[i % readerIds.length];

    await sql`
      INSERT INTO emprunt (id_utilisateur, id_livre, dateEmprunts, dateRetour, date_retour_livre)
      VALUES (
        ${readerId}, ${bookId},
        ${dayOffset(borrowed)}, ${dayOffset(due)},
        ${returned === null ? null : dayOffset(returned)}
      )
    `;

    // A book still out on loan must not show as available.
    await sql`
      UPDATE livre SET disponibilite = ${returned !== null} WHERE id_livre = ${bookId}
    `;
    console.log(`  • ${label}`);
  }

  console.log(`\nSeeding ${RESERVATION_OFFSETS.length + 1} reservations…`);
  for (let i = 0; i < RESERVATION_OFFSETS.length; i++) {
    const readerId = readerIds[i % readerIds.length];
    await sql`
      INSERT INTO reservation (id_utilisateur, dateReservation)
      VALUES (${readerId}, ${dayOffset(RESERVATION_OFFSETS[i])})
    `;
  }

  // One reservation from the administrator, so the list shows both roles.
  if (adminId) {
    await sql`
      INSERT INTO reservation (id_utilisateur, dateReservation)
      VALUES (${adminId}, ${dayOffset(-2)})
    `;
  }
}

/** Creates the account if missing. Returns its id either way. */
async function ensureAccount(nom, password, email, numero, role) {
  const existing = await sql`SELECT id_utilisateur FROM utilisateur WHERE LOWER(nom) = LOWER(${nom})`;
  if (existing.length > 0) {
    console.log(`Account "${nom}" already exists — leaving it alone.`);
    return existing[0].id_utilisateur;
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
  return id;
}

main().catch((error) => {
  console.error("Seed failed:", error.message);
  process.exit(1);
});
