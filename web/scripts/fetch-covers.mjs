/**
 * Resolves real cover artwork for every book in the catalogue.
 *
 *   npm run db:covers          fill in books that have no cover yet
 *   npm run db:covers -- --all re-resolve every book, overwriting existing covers
 *
 * What it stores is a *URL*, not an image file. Cover art is copyrighted; Open Library
 * and Google Books both serve it for exactly this purpose (catalogue display), so the
 * repository stays free of redistributed artwork and the clients fetch on demand.
 * A book with no match keeps `image_url` NULL and both clients fall back to the
 * generated gradient cover, so a miss is never a broken image.
 *
 * Two sources, tried in order:
 *   1. Open Library search  — no key, no quota, good on classics
 *   2. Google Books         — better on French-language and contemporary titles
 *
 * Every candidate is checked against the local title and author before it is accepted;
 * a search for "Antigone" happily returns unrelated editions otherwise.
 */

import { connect } from "./connection.mjs";

const sql = connect("db:covers");

const REFRESH_ALL = process.argv.includes("--all");

// Courtesy delay between upstream calls. Both APIs are free and unauthenticated;
// hammering them for 160 books is the fastest way to get rate-limited.
const THROTTLE_MS = 350;

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Strips accents, punctuation, articles and subtitles so "L'Étranger" and
 * "Letranger: roman" compare equal. Used only for match scoring, never for storage.
 */
function normalise(text) {
  return (text ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "") // combining diacritics left by NFD
    .toLowerCase()
    .replace(/['‘’`]/g, " ")
    .replace(/[^a-z0-9\s]/g, " ")
    .replace(/\b(le|la|les|l|un|une|des|du|de|d|the|a|an)\b/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

/** Fraction of the shorter token set that both strings share, 0..1. */
function similarity(a, b) {
  const left = new Set(normalise(a).split(" ").filter(Boolean));
  const right = new Set(normalise(b).split(" ").filter(Boolean));
  if (left.size === 0 || right.size === 0) return 0;
  let shared = 0;
  for (const token of left) if (right.has(token)) shared += 1;
  return shared / Math.min(left.size, right.size);
}

/**
 * Accepts a candidate only if the title clearly matches. The author is a weaker
 * signal — editions credit translators, "et al.", or nothing at all — so it can
 * rescue a borderline title but never sink a strong one.
 */
function isPlausible(candidate, book) {
  const titleScore = similarity(candidate.title, book.titre);
  if (titleScore >= 0.85) return true;
  if (titleScore < 0.5) return false;
  return similarity(candidate.author, book.auteur) >= 0.5;
}

/** Raised for 429/403, which mean "stop asking this source", not "this book failed". */
class SourceExhausted extends Error {}

async function fetchJson(url) {
  const response = await fetch(url, {
    headers: { "User-Agent": "BiblioTech/1.0 (academic project; FST Settat)" },
  });
  if (response.status === 429 || response.status === 403) {
    throw new SourceExhausted(`HTTP ${response.status}`);
  }
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}

const OPEN_LIBRARY_FIELDS = "limit=5&fields=title,author_name,cover_i";

/** -L is ~500px on the long edge: sharp on a card, light enough for a 160-book grid. */
const coverUrl = (id) => `https://covers.openlibrary.org/b/id/${id}-L.jpg`;

/**
 * Exact search: the French title and author as catalogued locally.
 *
 * Accepts on a title match, which is the safest signal available.
 */
async function fromOpenLibrary(book) {
  const url =
    `https://openlibrary.org/search.json?${OPEN_LIBRARY_FIELDS}` +
    `&title=${encodeURIComponent(book.titre)}&author=${encodeURIComponent(book.auteur)}`;

  const data = await fetchJson(url);
  for (const doc of data.docs ?? []) {
    if (!doc.cover_i) continue;
    const candidate = { title: doc.title, author: (doc.author_name ?? []).join(" ") };
    if (isPlausible(candidate, book)) return coverUrl(doc.cover_i);
  }
  return null;
}

/**
 * Free-text search, for the very common case where the French edition is catalogued
 * without artwork but the original is not — "Le Crime de l'Orient-Express" has no
 * cover, "Murder on the Orient Express" does.
 *
 * The title cannot be used to validate a hit here: it comes back translated, so
 * "Guerre et Paix" legitimately matches "War and Peace" and shares no tokens with it.
 * The author carries the whole check instead, and a mismatch is rejected outright —
 * which is why a work catalogued under a transliterated author (Лев Толстой) is left
 * with its generated cover rather than risking the wrong book's artwork.
 */
async function fromOpenLibraryLoose(book) {
  const query = `${book.titre} ${book.auteur}`;
  const url =
    `https://openlibrary.org/search.json?${OPEN_LIBRARY_FIELDS}` +
    `&q=${encodeURIComponent(query)}`;

  const data = await fetchJson(url);
  for (const doc of data.docs ?? []) {
    if (!doc.cover_i) continue;
    const author = (doc.author_name ?? []).join(" ");
    if (similarity(author, book.auteur) >= 0.5) return coverUrl(doc.cover_i);
  }
  return null;
}

async function fromGoogleBooks(book) {
  const query = `intitle:${book.titre} inauthor:${book.auteur}`;
  const url =
    "https://www.googleapis.com/books/v1/volumes?maxResults=5&printType=books" +
    `&q=${encodeURIComponent(query)}`;

  const data = await fetchJson(url);
  for (const item of data.items ?? []) {
    const info = item.volumeInfo ?? {};
    const link = info.imageLinks?.thumbnail ?? info.imageLinks?.smallThumbnail;
    if (!link) continue;
    const candidate = { title: info.title, author: (info.authors ?? []).join(" ") };
    if (isPlausible(candidate, book)) {
      // Google serves http:// and a curled-page overlay by default; https and
      // zoom=1 give a flat, secure image.
      return link.replace(/^http:/, "https:").replace(/&edge=curl/, "") + "&zoom=1";
    }
  }
  return null;
}

async function main() {
  await sql`ALTER TABLE livre ADD COLUMN IF NOT EXISTS image_url TEXT`;

  const books = REFRESH_ALL
    ? await sql`SELECT id_livre, titre, auteur FROM livre ORDER BY id_livre`
    : await sql`SELECT id_livre, titre, auteur FROM livre
                WHERE image_url IS NULL OR image_url = '' ORDER BY id_livre`;

  if (books.length === 0) {
    console.log("Every book already has a cover. Use --all to re-resolve them.");
    return;
  }

  console.log(`Resolving covers for ${books.length} book(s)…\n`);
  let found = 0;
  let missed = 0;

  // Ordered most to least precise: an exact title match beats a translated-edition
  // match, which beats Google's looser index.
  const SOURCES = [
    { name: "openlibrary", resolve: fromOpenLibrary, available: true },
    { name: "openlibrary/loose", resolve: fromOpenLibraryLoose, available: true },
    { name: "google", resolve: fromGoogleBooks, available: true },
  ];

  for (const row of books) {
    const book = { titre: row.titre, auteur: row.auteur };
    const label = `${book.titre} — ${book.auteur}`;
    let url = null;
    let source = "";

    for (const entry of SOURCES) {
      if (!entry.available) continue;
      try {
        url = await entry.resolve(book);
        if (url) {
          source = entry.name;
          break;
        }
      } catch (error) {
        if (error instanceof SourceExhausted) {
          // Rate-limited or blocked: every later book would hit the same wall, so
          // retire the source instead of logging the same failure 160 times.
          entry.available = false;
          console.warn(`  ! ${entry.name} rate-limited (${error.message}) — skipping it for the rest of this run`);
        } else {
          // A single upstream failure must not abandon the remaining books.
          console.warn(`  ! ${entry.name} failed for "${book.titre}": ${error.message}`);
        }
      }
      await sleep(THROTTLE_MS);
    }

    if (url) {
      await sql`UPDATE livre SET image_url = ${url} WHERE id_livre = ${row.id_livre}`;
      found += 1;
      console.log(`  ✓ ${label}  [${source}]`);
    } else {
      missed += 1;
      console.log(`  · ${label}  — no match, keeping the generated cover`);
    }

    await sleep(THROTTLE_MS);
  }

  console.log(`\nDone. ${found} cover(s) found, ${missed} left as generated covers.`);
}

main().catch((error) => {
  console.error("\nCover fetch failed:", error.message);
  process.exit(1);
});
