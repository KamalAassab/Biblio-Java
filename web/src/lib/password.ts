import { pbkdf2, randomBytes, timingSafeEqual } from "node:crypto";
import { promisify } from "node:util";

const pbkdf2Async = promisify(pbkdf2);

/**
 * Password hashing, byte-compatible with the Java desktop client.
 *
 * Both applications read the same `utilisateur` table, so the stored format has to
 * match exactly. The desktop side uses `Security.java`; keep the two in step:
 *
 *   pbkdf2$<iterations>$<salt base64url>$<hash base64url>
 *
 * PBKDF2-HMAC-SHA256, 210 000 iterations, 16-byte salt, 32-byte derived key
 * (OWASP Password Storage Cheat Sheet).
 */

const PREFIX = "pbkdf2";
const ITERATIONS = 210_000;
const SALT_BYTES = 16;
const KEY_BYTES = 32;
const DIGEST = "sha256";

/** Base64url without padding, matching Java's `Base64.getUrlEncoder().withoutPadding()`. */
function toBase64Url(buffer: Buffer): string {
  return buffer.toString("base64url");
}

function fromBase64Url(value: string): Buffer {
  return Buffer.from(value, "base64url");
}

export async function hashPassword(password: string): Promise<string> {
  const salt = randomBytes(SALT_BYTES);
  const derived = await pbkdf2Async(password, salt, ITERATIONS, KEY_BYTES, DIGEST);
  return `${PREFIX}$${ITERATIONS}$${toBase64Url(salt)}$${toBase64Url(derived)}`;
}

/**
 * Verifies a password in constant time.
 *
 * Accepts legacy plaintext rows so accounts created before hashing existed still work;
 * callers should re-hash those on the next successful sign-in (see `needsUpgrade`).
 */
export async function verifyPassword(password: string, stored: string | null): Promise<boolean> {
  if (!stored) {
    // Burn equivalent CPU so a missing account is not measurably faster than a wrong password.
    await pbkdf2Async(password, "absent-account-timing-guard", ITERATIONS, KEY_BYTES, DIGEST);
    return false;
  }

  if (!stored.startsWith(`${PREFIX}$`)) {
    return safeEqual(Buffer.from(password, "utf8"), Buffer.from(stored, "utf8"));
  }

  const parts = stored.split("$");
  if (parts.length !== 4) return false;

  const iterations = Number.parseInt(parts[1], 10);
  if (!Number.isFinite(iterations) || iterations < 1000) return false;

  let salt: Buffer;
  let expected: Buffer;
  try {
    salt = fromBase64Url(parts[2]);
    expected = fromBase64Url(parts[3]);
  } catch {
    return false;
  }

  const actual = await pbkdf2Async(password, salt, iterations, expected.length, DIGEST);
  return safeEqual(expected, actual);
}

/** True when a stored value is legacy plaintext and should be re-hashed. */
export function needsUpgrade(stored: string | null): boolean {
  return !!stored && stored.length > 0 && !stored.startsWith(`${PREFIX}$`);
}

function safeEqual(a: Buffer, b: Buffer): boolean {
  // timingSafeEqual throws on length mismatch, which would itself leak length.
  // Comparing fixed-size digests of both sides keeps the comparison uniform.
  if (a.length !== b.length) {
    const padded = Buffer.alloc(Math.max(a.length, b.length));
    const other = Buffer.alloc(padded.length);
    a.copy(padded);
    b.copy(other);
    timingSafeEqual(padded, other);
    return false;
  }
  return timingSafeEqual(a, b);
}
