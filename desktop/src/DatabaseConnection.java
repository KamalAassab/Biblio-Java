import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Data access layer for the Neon / PostgreSQL backing store.
 *
 * <p>Three things this layer is responsible for beyond plain CRUD:
 * <ul>
 *   <li><b>Credentials never live in source.</b> The connection string is read from
 *       {@code DATABASE_URL}, then a local {@code .env}, then the per-user AppData file.</li>
 *   <li><b>Connections are pooled.</b> Neon sits behind TLS across the network; opening a
 *       fresh connection per query cost ~300 ms each. Borrowed connections are handed out
 *       as proxies whose {@code close()} returns them to the pool, so every existing
 *       try-with-resources call site keeps working unchanged.</li>
 *   <li><b>Passwords are hashed.</b> See {@link Security}. Legacy plaintext rows are
 *       upgraded transparently on the owner's next successful login.</li>
 * </ul>
 */
public class DatabaseConnection {

    private static final String APP_NAME = "Biblio-Java";

    /** Neon's free tier is connection-limited; a small pool is plenty for a desktop client. */
    private static final int MAX_POOL = 4;
    private static final int VALIDATION_TIMEOUT_SECONDS = 2;
    private static final int QUERY_TIMEOUT_SECONDS = 15;

    private static final Deque<Connection> IDLE = new ArrayDeque<>();
    private static String cachedUrl;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConnection::shutdown));
    }

    // ── Connection handling ──────────────────────────────────────────────────

    /**
     * Returns a pooled connection. Callers should keep using try-with-resources —
     * {@code close()} on the returned proxy recycles rather than destroys it.
     */
    public static Connection getConnection() throws SQLException {
        Connection real = take();
        return wrap(real);
    }

    private static synchronized Connection take() throws SQLException {
        while (!IDLE.isEmpty()) {
            Connection c = IDLE.pop();
            try {
                if (c.isValid(VALIDATION_TIMEOUT_SECONDS)) return c;
                c.close();
            } catch (SQLException ignored) {
                // Stale connection — drop it and try the next one.
            }
        }
        return open();
    }

    private static synchronized void give(Connection real) {
        try {
            if (real == null || real.isClosed()) return;
            if (IDLE.size() >= MAX_POOL) {
                real.close();
                return;
            }
            if (!real.getAutoCommit()) real.rollback();
            IDLE.push(real);
        } catch (SQLException ignored) {
            try {
                real.close();
            } catch (SQLException ignored2) {
            }
        }
    }

    private static synchronized void shutdown() {
        while (!IDLE.isEmpty()) {
            try {
                IDLE.pop().close();
            } catch (SQLException ignored) {
            }
        }
    }

    /** Wraps a real connection so {@code close()} returns it to the pool instead of closing it. */
    private static Connection wrap(Connection real) {
        return (Connection) Proxy.newProxyInstance(
                DatabaseConnection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new InvocationHandler() {
                    private boolean released;

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String name = method.getName();
                        if ("close".equals(name)) {
                            if (!released) {
                                released = true;
                                give(real);
                            }
                            return null;
                        }
                        if ("isClosed".equals(name)) return released || real.isClosed();
                        if (released) throw new SQLException("Connection already returned to the pool");
                        try {
                            return method.invoke(real, args);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            throw e.getCause();
                        }
                    }
                });
    }

    private static Connection open() throws SQLException {
        String raw = readDatabaseUrl();
        if (raw == null || raw.isBlank()) {
            throw new SQLException(I18n.t("db.missing.body"));
        }
        Connection c = DriverManager.getConnection(toJdbcUrl(raw));
        c.setAutoCommit(true);
        return c;
    }

    /** True when a usable connection string is configured — used to show setup guidance early. */
    public static boolean isConfigured() {
        String raw = readDatabaseUrl();
        return raw != null && !raw.isBlank();
    }

    private static String readDatabaseUrl() {
        if (cachedUrl != null) return cachedUrl;

        String envUrl = System.getenv("DATABASE_URL");
        if (envUrl != null && !envUrl.isBlank()) return cachedUrl = envUrl.trim();

        // Found whether the app was launched from the repository root or from desktop/.
        Path localEnv = Resources.findPath(".env");
        String localUrl = localEnv == null ? null : readUrlFromFile(localEnv);
        if (localUrl != null) return cachedUrl = localUrl;

        Path config = getDatabaseUrlFile();
        if (Files.isRegularFile(config)) {
            String fromConfig = readUrlFromFile(config);
            if (fromConfig != null) return cachedUrl = fromConfig;
        }
        return null;
    }

    private static String readUrlFromFile(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                if (trimmed.startsWith("DATABASE_URL=")) {
                    trimmed = trimmed.substring("DATABASE_URL=".length()).trim();
                }
                // Tolerate quoted values, which shells and editors add freely.
                if (trimmed.length() > 1
                        && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                         || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
                    trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
                }
                if (!trimmed.isEmpty()) return trimmed;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static Path getDatabaseUrlFile() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isBlank()) {
            localAppData = Paths.get(System.getProperty("user.home"), "AppData", "Local").toString();
        }
        return Paths.get(localAppData, APP_NAME, "database.url");
    }

    /**
     * Converts a libpq-style URL into a JDBC URL, and guarantees TLS is on.
     *
     * <p>An unencrypted connection to a hosted database would put credentials and reader
     * records on the wire in clear text, so {@code sslmode} is forced to at least
     * {@code require} regardless of what the supplied URL asked for.
     */
    private static String toJdbcUrl(String raw) {
        String result;
        if (!raw.startsWith("postgresql://") && !raw.startsWith("postgres://")) {
            result = raw; // already a JDBC URL
        } else {
            String rest = raw.substring(raw.indexOf("://") + 3);
            String user = null;
            String password = null;
            int at = rest.lastIndexOf('@');
            if (at >= 0) {
                String creds = rest.substring(0, at);
                int colon = creds.indexOf(':');
                if (colon >= 0) {
                    user = urlDecode(creds.substring(0, colon));
                    password = urlDecode(creds.substring(colon + 1));
                } else {
                    user = urlDecode(creds);
                }
                rest = rest.substring(at + 1);
            }
            String params = "";
            int q = rest.indexOf('?');
            if (q >= 0) {
                params = rest.substring(q + 1);
                rest = rest.substring(0, q);
            }
            // channel_binding is a libpq-only option the JDBC driver rejects.
            params = params.replaceAll("(^|&)channel_binding=[^&]*", "");
            params = params.replaceAll("(^|&)channelBinding=[^&]*", "");
            params = params.replaceAll("^&+", "");

            StringBuilder url = new StringBuilder("jdbc:postgresql://").append(rest);
            boolean first = params.isEmpty();
            if (!params.isEmpty()) url.append('?').append(params);
            if (user != null) {
                url.append(first ? '?' : '&').append("user=").append(user);
                first = false;
            }
            if (password != null) {
                url.append(first ? '?' : '&').append("password=").append(password);
            }
            result = url.toString();
        }

        if (!result.matches("(?i).*[?&]sslmode=.*")) {
            result += (result.contains("?") ? "&" : "?") + "sslmode=require";
        } else {
            result = result.replaceAll("(?i)([?&])sslmode=(disable|allow|prefer)\\b", "$1sslmode=require");
        }
        return result;
    }

    private static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return s;
        }
    }

    private static PreparedStatement prepare(Connection c, String sql) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        // A hung query must not freeze the UI thread indefinitely.
        ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
        return ps;
    }

    private static void log(String context, SQLException e) {
        System.err.println("[db] " + context + ": " + Security.redact(e.getMessage()));
    }

    // ── Schema ───────────────────────────────────────────────────────────────

    /** Creates tables if absent, then applies forward-only migrations. Safe to run repeatedly. */
    public static void ensureSchema() throws SQLException {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS livre (" +
                "id_livre INTEGER PRIMARY KEY, titre VARCHAR(255) NOT NULL, auteur VARCHAR(255) NOT NULL," +
                "genre VARCHAR(100), resume_livre TEXT, disponibilite BOOLEAN DEFAULT TRUE)",
            "CREATE TABLE IF NOT EXISTS utilisateur (" +
                "id_utilisateur INTEGER PRIMARY KEY, nom VARCHAR(255) NOT NULL, motDePasse VARCHAR(255)," +
                "numero INTEGER, email VARCHAR(255), role_utilisateur VARCHAR(50))",
            "CREATE TABLE IF NOT EXISTS admin (" +
                "id_admin INTEGER PRIMARY KEY, id_utilisateur INTEGER REFERENCES utilisateur(id_utilisateur))",
            "CREATE TABLE IF NOT EXISTS lecteur (" +
                "id_lecteur INTEGER PRIMARY KEY, id_utilisateur INTEGER REFERENCES utilisateur(id_utilisateur))",
            "CREATE TABLE IF NOT EXISTS emprunt (" +
                "id_emprunt INTEGER PRIMARY KEY, id_utilisateur INTEGER, id_livre INTEGER," +
                "dateEmprunts DATE, dateRetour DATE)",
            "CREATE TABLE IF NOT EXISTS reservation (" +
                "id_reservation INTEGER PRIMARY KEY, id_utilisateur INTEGER, dateReservation DATE)"
        };

        // Applied after the base tables exist. Each is independently optional: a failure
        // means the constraint is already present or the data does not permit it yet,
        // and must not abort the rest of the run.
        String[] migrations = {
            // Track the real return date, distinct from the due date.
            "ALTER TABLE emprunt ADD COLUMN IF NOT EXISTS date_retour_livre DATE",
            // Referential integrity that the original schema left off.
            "ALTER TABLE emprunt ADD CONSTRAINT emprunt_livre_fk " +
                "FOREIGN KEY (id_livre) REFERENCES livre(id_livre) ON DELETE CASCADE",
            "ALTER TABLE emprunt ADD CONSTRAINT emprunt_user_fk " +
                "FOREIGN KEY (id_utilisateur) REFERENCES utilisateur(id_utilisateur) ON DELETE CASCADE",
            "ALTER TABLE reservation ADD CONSTRAINT reservation_user_fk " +
                "FOREIGN KEY (id_utilisateur) REFERENCES utilisateur(id_utilisateur) ON DELETE CASCADE",
            // One account per username — otherwise login is ambiguous.
            "CREATE UNIQUE INDEX IF NOT EXISTS utilisateur_nom_unique ON utilisateur (LOWER(nom))",
            // Indexes behind the hot paths in the dashboard and catalogue.
            "CREATE INDEX IF NOT EXISTS emprunt_livre_idx ON emprunt (id_livre)",
            "CREATE INDEX IF NOT EXISTS emprunt_user_idx ON emprunt (id_utilisateur)",
            "CREATE INDEX IF NOT EXISTS emprunt_open_idx ON emprunt (date_retour_livre) WHERE date_retour_livre IS NULL",
            "CREATE INDEX IF NOT EXISTS reservation_user_idx ON reservation (id_utilisateur)",
            "CREATE INDEX IF NOT EXISTS livre_titre_idx ON livre (LOWER(titre))",
            "CREATE INDEX IF NOT EXISTS livre_auteur_idx ON livre (LOWER(auteur))"
        };

        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            for (String s : ddl) st.executeUpdate(s);
            for (String s : migrations) {
                try {
                    st.executeUpdate(s);
                } catch (SQLException ignored) {
                    // Already applied, or blocked by existing data. Both are acceptable.
                }
            }
            installSequences(st);
        }
        CACHE.invalidateAll();
    }

    /**
     * Replaces the {@code MAX(id) + 1} pattern with real sequences.
     *
     * <p>The old approach had a race: two clients reading the same maximum would both
     * try to insert the same primary key, and the loser's write was silently lost.
     */
    private static void installSequences(Statement st) {
        String[][] tables = {
            {"livre", "id_livre"},
            {"utilisateur", "id_utilisateur"},
            {"admin", "id_admin"},
            {"lecteur", "id_lecteur"},
            {"emprunt", "id_emprunt"},
            {"reservation", "id_reservation"}
        };
        for (String[] t : tables) {
            String seq = t[0] + "_" + t[1] + "_seq";
            try {
                st.executeUpdate("CREATE SEQUENCE IF NOT EXISTS " + seq);
                st.executeUpdate("SELECT setval('" + seq + "', "
                        + "COALESCE((SELECT MAX(" + t[1] + ") FROM " + t[0] + "), 0) + 1, false)");
                st.executeUpdate("ALTER TABLE " + t[0] + " ALTER COLUMN " + t[1]
                        + " SET DEFAULT nextval('" + seq + "')");
            } catch (SQLException ignored) {
            }
        }
    }

    /** Seeds demonstration content the first time the database is used. */
    public static void seedIfEmpty() throws SQLException {
        if (getLivres().isEmpty()) {
            insertLivre(new Livre(0, "La Boîte à Merveilles", "Ahmed Sefrioui", "Autobiographie",
                    "Le récit de l'enfance de l'auteur dans la médina de Fès, entre superstitions, "
                            + "voisinage bruyant et émerveillement d'un enfant de six ans.", true));
            insertLivre(new Livre(0, "Antigone", "Jean Anouilh", "Tragédie",
                    "Antigone brave l'interdit de Créon pour enterrer son frère, et choisit la mort "
                            + "plutôt que le compromis.", true));
            insertLivre(new Livre(0, "Le Dernier Jour d'un Condamné", "Victor Hugo", "Roman à thèse",
                    "Le monologue d'un homme dans ses dernières heures, écrit comme un réquisitoire "
                            + "contre la peine capitale.", false));
            insertLivre(new Livre(0, "L'Étranger", "Albert Camus", "Roman",
                    "Meursault enterre sa mère sans pleurer, tue un homme sous le soleil d'Alger, "
                            + "et se fait juger pour son indifférence autant que pour son crime.", true));
            insertLivre(new Livre(0, "Les Misérables", "Victor Hugo", "Roman historique",
                    "De Jean Valjean au bagne aux barricades de 1832 : une fresque de la misère et "
                            + "de la rédemption dans la France du XIXe siècle.", true));
            insertLivre(new Livre(0, "Harry Potter à l'école des sorciers", "J.K. Rowling", "Fantastique",
                    "Un orphelin découvre le jour de ses onze ans qu'il est sorcier, et pousse pour "
                            + "la première fois les portes de Poudlard.", false));
            insertLivre(new Livre(0, "Le Petit Prince", "Antoine de Saint-Exupéry", "Conte",
                    "Un aviateur échoué dans le désert rencontre un enfant venu d'une autre planète, "
                            + "qui lui réapprend à voir l'essentiel.", true));
            insertLivre(new Livre(0, "Candide", "Voltaire", "Conte philosophique",
                    "Chassé du paradis d'un château westphalien, Candide traverse un monde absurde "
                            + "et brutal en répétant que tout va pour le mieux.", true));
        }

        boolean hasDemoAdmin = false;
        boolean hasDemoLecteur = false;
        for (Utilisateur u : getUtilisateurs()) {
            if ("admin".equalsIgnoreCase(u.getNom())) hasDemoAdmin = true;
            if ("lecteur".equalsIgnoreCase(u.getNom())) hasDemoLecteur = true;
        }
        if (!hasDemoAdmin) {
            Admin admin = new Admin(0, "admin", Security.hash("admin123"), 612345678,
                    "admin@fsts.ac.ma", null, null);
            insertAdmin(admin, admin);
        }
        if (!hasDemoLecteur) {
            Lecteur lecteur = new Lecteur(0, "lecteur", Security.hash("lecteur123"), 987654321,
                    "lecteur@fsts.ac.ma", null, null);
            insertLecteur(lecteur, lecteur);
        }
        CACHE.invalidateAll();
    }

    /**
     * Kept for compatibility with existing call sites. New code should rely on the
     * sequence defaults and {@code INSERT … RETURNING} instead — this method cannot
     * be made race-free.
     *
     * @deprecated identifiers are now assigned by PostgreSQL sequences.
     */
    @Deprecated
    public static int nextId(String table, String column) {
        // Whitelist guards against identifier injection, since these cannot be bound as parameters.
        if (!table.matches("[a-z_]{1,32}") || !column.matches("[a-z_]{1,32}")) return 1;
        String query = "SELECT COALESCE(MAX(" + column + "), 0) + 1 FROM " + table;
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log("nextId", e);
        }
        return 1;
    }

    // ── Books ────────────────────────────────────────────────────────────────

    /** Inserts a book and returns its generated id, or -1 on failure. */
    public static int insertLivre(Livre livre) {
        String titre = Validate.required(livre.getTitre(), Validate.MAX_TITLE, "error.title.required");
        String auteur = Validate.required(livre.getAuteur(), Validate.MAX_AUTHOR, "error.author.required");
        String genre = Validate.optional(livre.getGenre(), Validate.MAX_GENRE, "error.genre.invalid");
        String resume = Validate.cleanMultiline(livre.getResume());
        if (resume.length() > Validate.MAX_SUMMARY) throw new Validate.Invalid("error.summary.invalid");

        String query = "INSERT INTO livre (titre, auteur, genre, resume_livre, disponibilite) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id_livre";
        try (Connection c = getConnection(); PreparedStatement ps = prepare(c, query)) {
            ps.setString(1, titre);
            ps.setString(2, auteur);
            ps.setString(3, genre);
            ps.setString(4, resume);
            ps.setBoolean(5, livre.estDisponible());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CACHE.invalidateAll();
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log("insertLivre", e);
        }
        return -1;
    }

    public static boolean updateLivre(Livre livre) {
        String titre = Validate.required(livre.getTitre(), Validate.MAX_TITLE, "error.title.required");
        String auteur = Validate.required(livre.getAuteur(), Validate.MAX_AUTHOR, "error.author.required");
        String genre = Validate.optional(livre.getGenre(), Validate.MAX_GENRE, "error.genre.invalid");
        String resume = Validate.cleanMultiline(livre.getResume());
        if (resume.length() > Validate.MAX_SUMMARY) throw new Validate.Invalid("error.summary.invalid");

        String query = "UPDATE livre SET titre = ?, auteur = ?, genre = ?, resume_livre = ?, "
                + "disponibilite = ? WHERE id_livre = ?";
        try (Connection c = getConnection(); PreparedStatement ps = prepare(c, query)) {
            ps.setString(1, titre);
            ps.setString(2, auteur);
            ps.setString(3, genre);
            ps.setString(4, resume);
            ps.setBoolean(5, livre.estDisponible());
            ps.setInt(6, livre.getId());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) CACHE.invalidateAll();
            return ok;
        } catch (SQLException e) {
            log("updateLivre", e);
            return false;
        }
    }

    public static boolean deleteLivre(int id) {
        try (Connection c = getConnection();
             PreparedStatement ps = prepare(c, "DELETE FROM livre WHERE id_livre = ?")) {
            ps.setInt(1, id);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) CACHE.invalidateAll();
            return ok;
        } catch (SQLException e) {
            log("deleteLivre", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<Livre> getLivres() {
        return (ArrayList<Livre>) CACHE.get("livres", () -> {
            // image_url is added by the web project's migration (`npm run db:seed`).
            // A database that predates it is still perfectly usable, so a failure here
            // retries without the column rather than leaving the catalogue empty.
            ArrayList<Livre> livres = selectLivres(true);
            return livres != null ? livres : selectLivres(false);
        });
    }

    /** Reads the catalogue, optionally including the cover column. Null on failure. */
    private static ArrayList<Livre> selectLivres(boolean withCovers) {
        ArrayList<Livre> livres = new ArrayList<>();
        String query = "SELECT id_livre, titre, auteur, genre, resume_livre, "
                + "COALESCE(disponibilite, TRUE)"
                + (withCovers ? ", image_url" : "")
                + " FROM livre ORDER BY id_livre";

        try (Connection c = getConnection(); PreparedStatement ps = prepare(c, query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Livre livre = new Livre(rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getBoolean(6));
                if (withCovers) livre.setImageUrl(rs.getString(7));
                livres.add(livre);
            }
            return livres;
        } catch (SQLException e) {
            // Only the fallback attempt is worth reporting: the first failing is the
            // expected outcome on an un-migrated database.
            if (!withCovers) log("getLivres", e);
            return null;
        }
    }

    // ── Users ────────────────────────────────────────────────────────────────

    /** Inserts a user and returns the generated id, or -1 on failure. */
    public static int insertUtilisateur(Utilisateur utilisateur) {
        String nom = Validate.username(utilisateur.getNom());
        String email = Validate.email(utilisateur.getEmail());
        String stored = utilisateur.getMotdepasse();
        // Accept an already-hashed value (used by the seeder); hash anything else.
        if (stored != null && !stored.startsWith("pbkdf2$")) stored = Security.hash(stored);

        String query = "INSERT INTO utilisateur (nom, motDePasse, numero, email, role_utilisateur) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id_utilisateur";
        try (Connection c = getConnection(); PreparedStatement ps = prepare(c, query)) {
            ps.setString(1, nom);
            ps.setString(2, stored);
            ps.setInt(3, utilisateur.getNumero());
            ps.setString(4, email);
            ps.setString(5, utilisateur.getRole());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    utilisateur.setId(id);
                    CACHE.invalidateAll();
                    return id;
                }
            }
        } catch (SQLException e) {
            log("insertUtilisateur", e);
        }
        return -1;
    }

    public static void insertAdmin(Admin admin, Utilisateur utilisateur) {
        int id = insertUtilisateur(admin);
        if (id < 0) return;
        try (Connection c = getConnection();
             PreparedStatement ps = prepare(c, "INSERT INTO admin (id_utilisateur) VALUES (?)")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            CACHE.invalidateAll();
        } catch (SQLException e) {
            log("insertAdmin", e);
        }
    }

    public static void insertLecteur(Lecteur lecteur, Utilisateur utilisateur) {
        int id = insertUtilisateur(lecteur);
        if (id < 0) return;
        try (Connection c = getConnection();
             PreparedStatement ps = prepare(c, "INSERT INTO lecteur (id_utilisateur) VALUES (?)")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            CACHE.invalidateAll();
        } catch (SQLException e) {
            log("insertLecteur", e);
        }
    }

    public static boolean deleteUtilisateur(int id) {
        try (Connection c = getConnection();
             PreparedStatement ps = prepare(c, "DELETE FROM utilisateur WHERE id_utilisateur = ?")) {
            ps.setInt(1, id);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) CACHE.invalidateAll();
            return ok;
        } catch (SQLException e) {
            log("deleteUtilisateur", e);
            return false;
        }
    }

    /** Updates the editable profile fields. Passwords are changed via {@link #changePassword}. */
    public static boolean updateProfile(int id, String nom, String email, int numero) {
        String cleanNom = Validate.username(nom);
        String cleanEmail = Validate.email(email);
        String query = "UPDATE utilisateur SET nom = ?, email = ?, numero = ? WHERE id_utilisateur = ?";
        try (Connection c = getConnection(); PreparedStatement ps = prepare(c, query)) {
            ps.setString(1, cleanNom);
            ps.setString(2, cleanEmail);
            ps.setInt(3, numero);
            ps.setInt(4, id);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) CACHE.invalidateAll();
            return ok;
        } catch (SQLException e) {
            log("updateProfile", e);
            return false;
        }
    }

    /** Changes a password after verifying the current one. */
    public static boolean changePassword(int id, String currentPassword, String newPassword) {
        Validate.password(newPassword);
        try (Connection c = getConnection();
             PreparedStatement ps = prepare(c,
                     "SELECT motDePasse FROM utilisateur WHERE id_utilisateur = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !Security.verify(currentPassword, rs.getString(1))) return false;
            }
        } catch (SQLException e) {
            log("changePassword.verify", e);
            return false;
        }
        return storePasswordHash(id, Security.hash(newPassword));
    }

    private static boolean storePasswordHash(int id, String hash) {
        try (Connection c = getConnection();
             PreparedStatement ps = prepare(c,
                     "UPDATE utilisateur SET motDePasse = ? WHERE id_utilisateur = ?")) {
            ps.setString(1, hash);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log("storePasswordHash", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<Utilisateur> getUtilisateurs() {
        return (ArrayList<Utilisateur>) CACHE.get("utilisateurs", () -> {
            ArrayList<Utilisateur> result = new ArrayList<>();
            String query = "SELECT id_utilisateur, nom, numero, email, role_utilisateur "
                    + "FROM utilisateur ORDER BY id_utilisateur";
            try (Connection c = getConnection(); PreparedStatement ps = prepare(c, query);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    String nom = rs.getString(2);
                    int num = rs.getInt(3);
                    String email = rs.getString(4);
                    String role = rs.getString(5);
                    // Password hashes are deliberately not selected — nothing in the UI needs them.
                    if ("Admin".equalsIgnoreCase(role)) {
                        result.add(new Admin(id, nom, "", num, email, null, null));
                    } else {
                        result.add(new Lecteur(id, nom, "", num, email, null, null));
                    }
                }
            } catch (SQLException e) {
                log("getUtilisateurs", e);
            }
            return result;
        });
    }

    public static ArrayList<Lecteur> getLecteurs() {
        ArrayList<Lecteur> result = new ArrayList<>();
        for (Utilisateur u : getUtilisateurs()) {
            if (u instanceof Lecteur) result.add((Lecteur) u);
        }
        return result;
    }

    // ── Authentication ───────────────────────────────────────────────────────

    /**
     * Verifies credentials and returns the matching user, or {@code null}.
     *
     * <p>Failures are deliberately indistinguishable: an unknown username performs the
     * same hashing work as a wrong password, so response time leaks nothing about which
     * accounts exist. Repeated failures are throttled per username.
     *
     * @throws IllegalStateException with a localised message when the account is locked out
     */
    public static Utilisateur authentifier(String nom, String motDePasse) {
        String key = Validate.clean(nom);
        long lock = Security.Throttle.remainingLockMs(key);
        if (lock > 0) {
            throw new IllegalStateException(
                    I18n.t("login.error.locked", Math.max(1, lock / 1000)));
        }

        String query = "SELECT id_utilisateur, nom, motDePasse, numero, email, role_utilisateur "
                + "FROM utilisateur WHERE LOWER(nom) = LOWER(?)";
        try (Connection c = getConnection(); PreparedStatement ps = prepare(c, query)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    Security.verify(motDePasse, null); // equalise timing
                    Security.Throttle.recordFailure(key);
                    return null;
                }
                int id = rs.getInt(1);
                String storedName = rs.getString(2);
                String storedHash = rs.getString(3);
                int num = rs.getInt(4);
                String email = rs.getString(5);
                String role = rs.getString(6);

                if (!Security.verify(motDePasse, storedHash)) {
                    Security.Throttle.recordFailure(key);
                    return null;
                }
                Security.Throttle.recordSuccess(key);

                // Transparently upgrade a legacy plaintext row now that we know the password.
                if (Security.needsUpgrade(storedHash)) {
                    storePasswordHash(id, Security.hash(motDePasse));
                }

                if ("Admin".equalsIgnoreCase(role)) {
                    return new Admin(id, storedName, "", num, email, null, null);
                }
                return new Lecteur(id, storedName, "", num, email, null, null);
            }
        } catch (SQLException e) {
            log("authentifier", e);
            throw new IllegalStateException(I18n.t("login.error.connection"));
        }
    }

    // ── Loans ────────────────────────────────────────────────────────────────

    /**
     * Records a loan and marks the book unavailable, in one transaction.
     *
     * <p>Both statements must land together: a loan against a book still flagged
     * available would let the same copy be lent twice.
     */
    public static int insertEmprunt(Emprunt emprunt) {
        Connection c = null;
        try {
            c = getConnection();
            c.setAutoCommit(false);

            // Re-check availability inside the transaction, locking the row.
            try (PreparedStatement check = prepare(c,
                    "SELECT COALESCE(disponibilite, TRUE) FROM livre WHERE id_livre = ? FOR UPDATE")) {
                check.setInt(1, emprunt.getLivre().getId());
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next() || !rs.getBoolean(1)) {
                        c.rollback();
                        throw new Validate.Invalid("error.book.unavailable");
                    }
                }
            }

            int id;
            try (PreparedStatement ps = prepare(c,
                    "INSERT INTO emprunt (id_utilisateur, id_livre, dateEmprunts, dateRetour) "
                            + "VALUES (?, ?, ?, ?) RETURNING id_emprunt")) {
                ps.setInt(1, emprunt.getUtilisateur().getId());
                ps.setInt(2, emprunt.getLivre().getId());
                ps.setDate(3, java.sql.Date.valueOf(emprunt.getDateEmprunts()));
                ps.setDate(4, java.sql.Date.valueOf(emprunt.getDateRetour()));
                try (ResultSet rs = ps.executeQuery()) {
                    id = rs.next() ? rs.getInt(1) : -1;
                }
            }

            try (PreparedStatement upd = prepare(c,
                    "UPDATE livre SET disponibilite = FALSE WHERE id_livre = ?")) {
                upd.setInt(1, emprunt.getLivre().getId());
                upd.executeUpdate();
            }

            c.commit();
            CACHE.invalidateAll();
            return id;
        } catch (SQLException e) {
            rollbackQuietly(c);
            log("insertEmprunt", e);
            return -1;
        } catch (RuntimeException e) {
            rollbackQuietly(c);
            throw e;
        } finally {
            restoreAndClose(c);
        }
    }

    /** Marks a loan returned and puts the book back into circulation, transactionally. */
    public static boolean returnEmprunt(int empruntId) {
        Connection c = null;
        try {
            c = getConnection();
            c.setAutoCommit(false);

            int livreId = -1;
            try (PreparedStatement ps = prepare(c,
                    "SELECT id_livre FROM emprunt WHERE id_emprunt = ? AND date_retour_livre IS NULL")) {
                ps.setInt(1, empruntId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) livreId = rs.getInt(1);
                }
            }
            if (livreId < 0) {
                c.rollback();
                return false;
            }

            try (PreparedStatement ps = prepare(c,
                    "UPDATE emprunt SET date_retour_livre = CURRENT_DATE WHERE id_emprunt = ?")) {
                ps.setInt(1, empruntId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = prepare(c,
                    "UPDATE livre SET disponibilite = TRUE WHERE id_livre = ?")) {
                ps.setInt(1, livreId);
                ps.executeUpdate();
            }

            c.commit();
            CACHE.invalidateAll();
            return true;
        } catch (SQLException e) {
            rollbackQuietly(c);
            log("returnEmprunt", e);
            return false;
        } finally {
            restoreAndClose(c);
        }
    }

    private static void rollbackQuietly(Connection c) {
        if (c == null) return;
        try {
            c.rollback();
        } catch (SQLException ignored) {
        }
    }

    private static void restoreAndClose(Connection c) {
        if (c == null) return;
        try {
            c.setAutoCommit(true);
        } catch (SQLException ignored) {
        }
        try {
            c.close();
        } catch (SQLException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<Emprunt> getEmprunts() {
        return (ArrayList<Emprunt>) CACHE.get("emprunts", () -> {
            ArrayList<Emprunt> result = new ArrayList<>();
            String query = "SELECT e.id_emprunt, u.id_utilisateur, u.nom, l.id_livre, l.titre, l.auteur, "
                    + "e.dateEmprunts, e.dateRetour, e.date_retour_livre "
                    + "FROM emprunt e JOIN utilisateur u ON e.id_utilisateur = u.id_utilisateur "
                    + "JOIN livre l ON e.id_livre = l.id_livre ORDER BY e.id_emprunt DESC";
            try (Connection c = getConnection(); PreparedStatement ps = prepare(c, query);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Lecteur user = new Lecteur(rs.getInt(2), rs.getString(3), "", 0, "", null, null);
                    Livre livre = new Livre(rs.getInt(4), rs.getString(5), rs.getString(6), "", "", false);
                    LocalDate dEmprunt = rs.getDate(7) != null ? rs.getDate(7).toLocalDate() : LocalDate.now();
                    Emprunt e = new Emprunt(rs.getInt(1), user, livre, dEmprunt);
                    if (rs.getDate(8) != null) e.setDateRetour(rs.getDate(8).toLocalDate());
                    if (rs.getDate(9) != null) e.setDateRetourLivre(rs.getDate(9).toLocalDate());
                    result.add(e);
                }
            } catch (SQLException e) {
                log("getEmprunts", e);
            }
            return result;
        });
    }

    // ── Reservations ─────────────────────────────────────────────────────────

    public static int insertReservation(Reservation reservation, Utilisateur utilisateur) {
        String query = "INSERT INTO reservation (id_utilisateur, dateReservation) VALUES (?, ?) "
                + "RETURNING id_reservation";
        try (Connection c = getConnection(); PreparedStatement ps = prepare(c, query)) {
            ps.setInt(1, utilisateur.getId());
            ps.setDate(2, java.sql.Date.valueOf(reservation.getDateReservation()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CACHE.invalidateAll();
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log("insertReservation", e);
        }
        return -1;
    }

    public static boolean deleteReservation(int id) {
        try (Connection c = getConnection();
             PreparedStatement ps = prepare(c, "DELETE FROM reservation WHERE id_reservation = ?")) {
            ps.setInt(1, id);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) CACHE.invalidateAll();
            return ok;
        } catch (SQLException e) {
            log("deleteReservation", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<Reservation> getReservations() {
        return (ArrayList<Reservation>) CACHE.get("reservations", () -> {
            ArrayList<Reservation> result = new ArrayList<>();
            String query = "SELECT r.id_reservation, u.id_utilisateur, u.nom, r.dateReservation "
                    + "FROM reservation r JOIN utilisateur u ON r.id_utilisateur = u.id_utilisateur "
                    + "ORDER BY r.id_reservation DESC";
            try (Connection c = getConnection(); PreparedStatement ps = prepare(c, query);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Lecteur user = new Lecteur(rs.getInt(2), rs.getString(3), "", 0, "", null, null);
                    LocalDate date = rs.getDate(4) != null ? rs.getDate(4).toLocalDate() : LocalDate.now();
                    result.add(new Reservation(user, date, rs.getInt(1)));
                }
            } catch (SQLException e) {
                log("getReservations", e);
            }
            return result;
        });
    }

    // ── Caching ──────────────────────────────────────────────────────────────

    /**
     * Short-lived read cache.
     *
     * <p>The dashboard alone issues four list queries every time it is shown, and each
     * one crosses the network to Neon. Holding results for a couple of seconds and
     * clearing the whole cache on any write keeps navigation instant without ever
     * showing stale data after a user action.
     */
    private static final class Cache {
        private static final long TTL_MS = 2_500L;
        private final java.util.Map<String, Object> values = new java.util.HashMap<>();
        private final java.util.Map<String, Long> stamps = new java.util.HashMap<>();

        synchronized Object get(String key, java.util.function.Supplier<Object> loader) {
            Long at = stamps.get(key);
            if (at != null && System.currentTimeMillis() - at < TTL_MS) {
                Object cached = values.get(key);
                if (cached != null) return copyOf(cached);
            }
            Object fresh = loader.get();
            values.put(key, fresh);
            stamps.put(key, System.currentTimeMillis());
            return copyOf(fresh);
        }

        /** Hands out a defensive copy so a caller sorting or filtering cannot corrupt the cache. */
        private Object copyOf(Object value) {
            if (value instanceof ArrayList<?> list) return new ArrayList<>(list);
            return value;
        }

        synchronized void invalidateAll() {
            values.clear();
            stamps.clear();
        }
    }

    private static final Cache CACHE = new Cache();

    /** Clears cached reads; call after any out-of-band change. */
    public static void invalidateCache() {
        CACHE.invalidateAll();
    }

    // ── Aggregates ───────────────────────────────────────────────────────────

    /** Dashboard counters, computed in a single round trip rather than four list loads. */
    public record Stats(int books, int available, int activeLoans, int overdue,
                        int reservations, int members) {}

    public static Stats stats() {
        String query =
                "SELECT (SELECT COUNT(*) FROM livre),"
              + " (SELECT COUNT(*) FROM livre WHERE COALESCE(disponibilite, TRUE)),"
              + " (SELECT COUNT(*) FROM emprunt WHERE date_retour_livre IS NULL),"
              + " (SELECT COUNT(*) FROM emprunt WHERE date_retour_livre IS NULL AND dateRetour < CURRENT_DATE),"
              + " (SELECT COUNT(*) FROM reservation),"
              + " (SELECT COUNT(*) FROM utilisateur)";
        try (Connection c = getConnection(); PreparedStatement ps = prepare(c, query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new Stats(rs.getInt(1), rs.getInt(2), rs.getInt(3),
                        rs.getInt(4), rs.getInt(5), rs.getInt(6));
            }
        } catch (SQLException e) {
            log("stats", e);
        }
        return new Stats(0, 0, 0, 0, 0, 0);
    }

    /** Distinct genres present in the catalogue, for the category filter row. */
    public static List<String> genres() {
        List<String> out = new ArrayList<>();
        String query = "SELECT genre, COUNT(*) FROM livre WHERE genre IS NOT NULL AND genre <> '' "
                + "GROUP BY genre ORDER BY COUNT(*) DESC, genre";
        try (Connection c = getConnection(); PreparedStatement ps = prepare(c, query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rs.getString(1));
        } catch (SQLException e) {
            log("genres", e);
        }
        return out;
    }
}
