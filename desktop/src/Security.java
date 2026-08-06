import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Password hashing and login throttling.
 *
 * <p>Uses PBKDF2-HMAC-SHA256, which ships with the JDK — no third-party jar, so
 * the plain {@code javac} build and the packaged .exe keep working unchanged.
 *
 * <p>Stored format is self-describing so the work factor can be raised later
 * without invalidating existing hashes:
 * <pre>pbkdf2$&lt;iterations&gt;$&lt;salt-b64url&gt;$&lt;hash-b64url&gt;</pre>
 *
 * <p>Rows written before hashing existed are stored as bare plaintext. Those are
 * still accepted at login exactly once, then transparently upgraded — see
 * {@link #needsUpgrade(String)} and {@code DatabaseConnection.authentifier}.
 */
public final class Security {

    private Security() {}

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2$";
    /** OWASP-recommended floor for PBKDF2-HMAC-SHA256 (Password Storage Cheat Sheet, 2023). */
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    /** Precomputed hash used to burn equivalent CPU when an account does not exist. */
    private static final String DUMMY_HASH = hash("$never-a-real-password$".toCharArray());

    // ── Hashing ──────────────────────────────────────────────────────────────

    /** Hashes a password with a fresh random salt. The char[] is zeroed before returning. */
    public static String hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        RNG.nextBytes(salt);
        byte[] key = derive(password, salt, ITERATIONS);
        return PREFIX + ITERATIONS + "$" + B64.encodeToString(salt) + "$" + B64.encodeToString(key);
    }

    public static String hash(String password) {
        char[] chars = password == null ? new char[0] : password.toCharArray();
        try {
            return hash(chars);
        } finally {
            Arrays.fill(chars, '\0');
        }
    }

    /**
     * Verifies a password against a stored value in constant time.
     *
     * <p>Accepts both the modern {@code pbkdf2$...} format and legacy plaintext,
     * so existing accounts keep working through the migration.
     */
    public static boolean verify(String password, String stored) {
        if (password == null) password = "";
        if (stored == null || stored.isEmpty()) {
            // Still do the work, so a missing account is not faster than a wrong password.
            verifyHashed(password, DUMMY_HASH);
            return false;
        }
        if (!stored.startsWith(PREFIX)) {
            // Legacy plaintext row. Compare without short-circuiting on length.
            return MessageDigest.isEqual(
                    password.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return verifyHashed(password, stored);
    }

    private static boolean verifyHashed(String password, String stored) {
        String[] parts = stored.split("\\$");
        // parts = ["pbkdf2", iterations, salt, hash]
        if (parts.length != 4) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = B64D.decode(parts[2]);
            byte[] expected = B64D.decode(parts[3]);
            char[] chars = password.toCharArray();
            byte[] actual;
            try {
                actual = derive(chars, salt, iterations);
            } finally {
                Arrays.fill(chars, '\0');
            }
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** True when a stored value is legacy plaintext and should be re-hashed after a successful login. */
    public static boolean needsUpgrade(String stored) {
        return stored != null && !stored.isEmpty() && !stored.startsWith(PREFIX);
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            // Both possible causes (missing algorithm, invalid spec) are unrecoverable
            // configuration faults — failing closed is the only safe option.
            throw new IllegalStateException("Password hashing unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    // ── Login throttling ─────────────────────────────────────────────────────

    /**
     * Per-identifier attempt limiter with exponential backoff.
     *
     * <p>Deliberately keyed on the submitted username rather than a global counter,
     * so one attacker hammering a single account cannot lock every other user out.
     */
    public static final class Throttle {
        private static final int FREE_ATTEMPTS = 5;
        private static final long BASE_DELAY_MS = 2_000L;
        private static final long MAX_DELAY_MS = 5 * 60_000L;
        private static final Map<String, int[]> FAILURES = new HashMap<>();
        private static final Map<String, Long> LOCKED_UNTIL = new HashMap<>();

        private Throttle() {}

        /** Milliseconds the caller must wait before another attempt is allowed; 0 when free to proceed. */
        public static synchronized long remainingLockMs(String key) {
            Long until = LOCKED_UNTIL.get(normalise(key));
            if (until == null) return 0L;
            long left = until - System.currentTimeMillis();
            return Math.max(0L, left);
        }

        public static synchronized void recordFailure(String key) {
            String k = normalise(key);
            int[] count = FAILURES.computeIfAbsent(k, x -> new int[1]);
            count[0]++;
            if (count[0] > FREE_ATTEMPTS) {
                int over = count[0] - FREE_ATTEMPTS;
                long delay = Math.min(MAX_DELAY_MS, BASE_DELAY_MS * (1L << Math.min(over - 1, 10)));
                LOCKED_UNTIL.put(k, System.currentTimeMillis() + delay);
            }
        }

        public static synchronized void recordSuccess(String key) {
            String k = normalise(key);
            FAILURES.remove(k);
            LOCKED_UNTIL.remove(k);
        }

        private static String normalise(String key) {
            return key == null ? "" : key.trim().toLowerCase();
        }
    }

    // ── Redaction ────────────────────────────────────────────────────────────

    /** Strips credentials out of a connection string so it is safe to log. */
    public static String redact(String connectionString) {
        if (connectionString == null) return "";
        return connectionString
                .replaceAll("(?i)password=[^&;\\s]*", "password=***")
                .replaceAll("://([^:/@]+):([^@]+)@", "://$1:***@");
    }
}
