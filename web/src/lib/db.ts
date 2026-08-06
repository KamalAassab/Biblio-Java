import { neon } from "@neondatabase/serverless";

/**
 * The single database entry point.
 *
 * This module must never be imported from a Client Component. `DATABASE_URL` is a
 * server-only environment variable (no `NEXT_PUBLIC_` prefix), so an accidental client
 * import fails the build rather than shipping the connection string to the browser.
 */

const rawConnectionString = process.env.DATABASE_URL;

if (!rawConnectionString) {
  throw new Error(
    "DATABASE_URL is not set. Copy .env.local.example to .env.local and fill it in, " +
      "or add it to your Vercel project's environment variables.",
  );
}

/**
 * Accepts either connection-string form.
 *
 * Neon's console and the desktop client hand out different shapes: the Java app is
 * usually configured with a JDBC URL (`jdbc:postgresql://host/db?user=…&password=…`),
 * while `neon()` only understands the libpq form. Since the same value gets copied
 * between the two, normalise here rather than making that a documented footgun.
 */
export function toLibpqUrl(raw: string): string {
  const trimmed = raw.trim().replace(/^["']|["']$/g, "");
  if (!trimmed.startsWith("jdbc:")) return trimmed;

  const withoutPrefix = trimmed.slice("jdbc:".length);
  const url = new URL(withoutPrefix);

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

/**
 * Tagged-template query helper. Interpolated values are always sent as bound
 * parameters, never concatenated into SQL, so `sql\`… WHERE nom = ${input}\`` is safe.
 */
export const sql = neon(toLibpqUrl(rawConnectionString));

/** True when running the public demo, which applies extra guardrails. */
export const isDemo = process.env.DEMO_MODE === "true";

/**
 * Rows the public demo refuses to mutate.
 *
 * Without this the seeded demo accounts could be renamed or deleted by any visitor,
 * and the next person to open the live link would find a broken sign-in screen.
 */
export const PROTECTED_USERNAMES = ["admin", "lecteur"];

export function assertDemoMutationAllowed(username?: string | null) {
  if (!isDemo) return;
  if (username && PROTECTED_USERNAMES.includes(username.toLowerCase())) {
    throw new DemoRestrictionError();
  }
}

export class DemoRestrictionError extends Error {
  constructor() {
    super("This action is disabled on the public demo.");
    this.name = "DemoRestrictionError";
  }
}
