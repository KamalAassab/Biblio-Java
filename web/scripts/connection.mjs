/**
 * Connection-string handling shared by the maintenance scripts.
 *
 * This mirrors `toLibpqUrl` in ../src/lib/db.ts. The duplication is deliberate: the
 * scripts run under plain Node with no TypeScript build step, so they cannot import
 * the app's module. Keep the two in sync — a change to one belongs in the other.
 */

import { neon } from "@neondatabase/serverless";

/**
 * Accepts either connection-string form.
 *
 * Neon's console and the desktop client hand out different shapes: the Java app is
 * usually configured with a JDBC URL (`jdbc:postgresql://host/db?user=…&password=…`),
 * while `neon()` only understands the libpq form. The same value tends to get copied
 * between the two, so normalise rather than making it a documented footgun.
 */
export function toLibpqUrl(raw) {
  const trimmed = raw.trim().replace(/^["']|["']$/g, "");
  if (!trimmed.startsWith("jdbc:")) return trimmed;

  const url = new URL(trimmed.slice("jdbc:".length));
  const user = url.searchParams.get("user");
  const password = url.searchParams.get("password");
  url.searchParams.delete("user");
  url.searchParams.delete("password");
  // libpq-only options the driver rejects.
  url.searchParams.delete("channelBinding");
  url.searchParams.delete("channel_binding");

  if (user) url.username = encodeURIComponent(user);
  if (password) url.password = encodeURIComponent(password);
  if (!url.searchParams.has("sslmode")) url.searchParams.set("sslmode", "require");

  return url.toString();
}

/** A configured `sql` tag, or a clear exit if DATABASE_URL is missing. */
export function connect(scriptName) {
  const raw = process.env.DATABASE_URL;
  if (!raw) {
    console.error(`DATABASE_URL is not set. Run with: npm run ${scriptName}`);
    process.exit(1);
  }
  return neon(toLibpqUrl(raw));
}
