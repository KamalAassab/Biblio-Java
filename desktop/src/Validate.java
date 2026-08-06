import java.util.regex.Pattern;

/**
 * Input validation and sanitisation.
 *
 * <p>Every value that reaches the database passes through here first. SQL injection
 * is already prevented by the prepared statements in {@code DatabaseConnection};
 * this layer guards the rest: oversized payloads, control characters, homoglyph
 * and bidi-override tricks in display names, and malformed contact details.
 */
public final class Validate {

    private Validate() {}

    public static final int MAX_NAME = 80;
    public static final int MAX_TITLE = 200;
    public static final int MAX_AUTHOR = 120;
    public static final int MAX_GENRE = 60;
    public static final int MAX_SUMMARY = 4_000;
    public static final int MAX_EMAIL = 254;
    public static final int MIN_PASSWORD = 8;
    public static final int MAX_PASSWORD = 128;

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");
    private static final Pattern USERNAME =
            Pattern.compile("^[\\p{L}\\p{N} ._'-]{3,80}$");

    /** Thrown when a value fails validation. The message is an {@link I18n} key. */
    public static class Invalid extends RuntimeException {
        private final String key;

        public Invalid(String key) {
            super(key);
            this.key = key;
        }

        public String key() {
            return key;
        }

        /** Localised, user-facing message. */
        @Override
        public String getLocalizedMessage() {
            return I18n.t(key);
        }
    }

    /**
     * Strips characters that have no business in a stored field: C0/C1 control
     * codes, Unicode bidirectional overrides (used to disguise text), and zero-width
     * joiners. Collapses runs of whitespace and trims.
     */
    public static String clean(String raw) {
        if (raw == null) return "";
        String s = raw.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                      .replaceAll("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF]", "")
                      .replaceAll("\\s+", " ")
                      .trim();
        return s;
    }

    /** Like {@link #clean} but preserves newlines, for multi-line fields such as summaries. */
    public static String cleanMultiline(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                  .replaceAll("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF]", "")
                  .replaceAll("[ \t]+", " ")
                  .replaceAll("(\r?\n){3,}", "\n\n")
                  .trim();
    }

    public static String required(String raw, int max, String errorKey) {
        String s = clean(raw);
        if (s.isEmpty() || s.length() > max) throw new Invalid(errorKey);
        return s;
    }

    public static String optional(String raw, int max, String errorKey) {
        String s = clean(raw);
        if (s.length() > max) throw new Invalid(errorKey);
        return s;
    }

    public static String username(String raw) {
        String s = clean(raw);
        if (!USERNAME.matcher(s).matches()) throw new Invalid("error.username.invalid");
        return s;
    }

    public static String email(String raw) {
        String s = clean(raw).toLowerCase();
        if (s.isEmpty()) return "";
        if (s.length() > MAX_EMAIL || !EMAIL.matcher(s).matches()) throw new Invalid("error.email.invalid");
        return s;
    }

    /**
     * Enforces a password policy strong enough to matter without being hostile:
     * at least 8 characters and at least three of {lowercase, uppercase, digit, symbol}.
     */
    public static String password(String raw) {
        if (raw == null) raw = "";
        if (raw.length() < MIN_PASSWORD || raw.length() > MAX_PASSWORD) {
            throw new Invalid("error.password.length");
        }
        int classes = 0;
        if (raw.matches(".*[a-z].*")) classes++;
        if (raw.matches(".*[A-Z].*")) classes++;
        if (raw.matches(".*[0-9].*")) classes++;
        if (raw.matches(".*[^A-Za-z0-9].*")) classes++;
        if (classes < 3) throw new Invalid("error.password.weak");
        return raw;
    }

    /** Parses a Moroccan-style phone number into the int column the schema uses. */
    public static int phone(String raw) {
        String digits = clean(raw).replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        if (digits.length() > 9) digits = digits.substring(digits.length() - 9);
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            throw new Invalid("error.phone.invalid");
        }
    }

    /** Guards search input: bounded length, no wildcards that would force a full scan. */
    public static String searchTerm(String raw) {
        String s = clean(raw);
        if (s.length() > 120) s = s.substring(0, 120);
        return s.replace("%", "").replace("_", "");
    }
}
