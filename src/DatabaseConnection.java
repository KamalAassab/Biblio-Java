import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;

public class DatabaseConnection {

    private static final String APP_NAME = "Biblio-Java";

    // Credentials are NOT stored in source code.
    // The app reads DATABASE_URL from the environment first, then from the per-user AppData file.
    public static Connection getConnection() throws SQLException {
        String rawUrl = readDatabaseUrl();
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new SQLException("DATABASE_URL n'est pas définie. Créez un fichier .env dans le dossier de l' application ou dans %LOCALAPPDATA%\\Biblio-Java\\database.url.");
        }
        return DriverManager.getConnection(toJdbcUrl(rawUrl));
    }

    private static String readDatabaseUrl() {
        String envUrl = System.getenv("DATABASE_URL");
        if (envUrl != null && !envUrl.isBlank()) return envUrl.trim();

        // Check for .env file in the app folder
        Path localEnv = Paths.get(System.getProperty("user.dir"), ".env");
        String localUrl = readUrlFromFile(localEnv);
        if (localUrl != null) return localUrl;

        Path config = getDatabaseUrlFile();
        if (Files.isRegularFile(config)) {
            return readUrlFromFile(config);
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

    // Converts a libpq-style URL (postgresql://user:pass@host/db?params) into a JDBC URL.
    private static String toJdbcUrl(String raw) {
        if (!raw.startsWith("postgresql://") && !raw.startsWith("postgres://")) {
            return raw; // already a JDBC URL
        }
        String rest = raw.substring(raw.indexOf("://") + 3);
        String user = null;
        String password = null;
        int at = rest.lastIndexOf('@');
        if (at >= 0) {
            String creds = rest.substring(0, at);
            int colon = creds.indexOf(':');
            if (colon >= 0) {
                user = creds.substring(0, colon);
                password = creds.substring(colon + 1);
            } else {
                user = creds;
            }
            rest = rest.substring(at + 1);
        }
        String params = "";
        int q = rest.indexOf('?');
        if (q >= 0) {
            params = rest.substring(q + 1);
            rest = rest.substring(0, q);
        }
        // channel_binding is a libpq-only option, not supported by the JDBC driver.
        params = params.replaceAll("(^|&)channel_binding=[^&]*", "");

        StringBuilder url = new StringBuilder("jdbc:postgresql://").append(rest);
        boolean first = params.isEmpty();
        if (!params.isEmpty()) {
            url.append('?').append(params);
        }
        if (user != null) {
            url.append(first ? '?' : '&').append("user=").append(user);
            first = false;
        }
        if (password != null) {
            url.append(first ? '?' : '&').append("password=").append(password);
        }
        return url.toString();
    }

    // Crée les tables si elles n'existent pas (Neon / PostgreSQL).
    public static void ensureSchema() throws SQLException {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS livre (" +
                "id_livre INTEGER PRIMARY KEY, titre VARCHAR(255) NOT NULL, auteur VARCHAR(255) NOT NULL," +
                "genre VARCHAR(100), resume_livre TEXT, disponibilite BOOLEAN)",
            "CREATE TABLE IF NOT EXISTS utilisateur (" +
                "id_utilisateur INTEGER PRIMARY KEY, nom VARCHAR(255) NOT NULL, motDePasse VARCHAR(255)," +
                "numero INTEGER, email VARCHAR(255), role_utilisateur VARCHAR(50))",
            "CREATE TABLE IF NOT EXISTS admin (" +
                "id_admin INTEGER PRIMARY KEY, id_utilisateur INTEGER REFERENCES utilisateur(id_utilisateur))",
            "CREATE TABLE IF NOT EXISTS lecteur (" +
                "id_lecteur INTEGER PRIMARY KEY, id_utilisateur INTEGER REFERENCES utilisateur(id_utilisateur))",
            "CREATE TABLE IF NOT EXISTS emprunt (" +
                "id_emprunt INTEGER PRIMARY KEY, id_utilisateur INTEGER, id_livre INTEGER, dateEmprunts DATE, dateRetour DATE)",
            "CREATE TABLE IF NOT EXISTS reservation (" +
                "id_reservation INTEGER PRIMARY KEY, id_utilisateur INTEGER, dateReservation DATE)"
        };
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            for (String s : ddl) st.executeUpdate(s);
        }
    }

    // Ajoute des données de démonstration si la base est vide.
    public static void seedIfEmpty() throws SQLException {
        if (getLivres().isEmpty()) {
            insertLivre(new Livre(1, "La Boîte à Merveilles", "Ahmed Sefrioui", "autobiographique", "Ce roman raconte l'enfance de l'auteur à Fès, au Maroc.", true));
            insertLivre(new Livre(2, "Antigone", "Jean Anouilh", "Tragédie", "Antigone défie le roi Créon en enterrant son frère Polynice.", true));
            insertLivre(new Livre(3, "Le Dernier Jour d'un Condamné", "Victor Hugo", "Roman à thèse", "Un condamné à mort réfléchit à l'inhumanité de la peine de mort.", false));
            insertLivre(new Livre(4, "L'Étranger", "Albert Camus", "Roman", "Meursault, indifférent, commet un meurtre et est jugé.", true));
            insertLivre(new Livre(5, "Les Misérables", "Victor Hugo", "Roman historique", "La vie de Jean Valjean dans la France du XIXe siècle.", true));
            insertLivre(new Livre(6, "Harry Potter à l'école des sorciers", "J.K. Rowling", "Fantastique", "Le jeune sorcier découvre Poudlard.", false));
        }
        // S'assure que les comptes de démonstration existent toujours.
        boolean hasDemoAdmin = false;
        boolean hasDemoLecteur = false;
        for (Utilisateur u : getUtilisateurs()) {
            if ("admin".equals(u.getNom())) hasDemoAdmin = true;
            if ("lecteur".equals(u.getNom())) hasDemoLecteur = true;
        }
        if (!hasDemoAdmin) {
            int id = nextId("utilisateur", "id_utilisateur");
            Admin admin = new Admin(id, "admin", "admin123", 612345678, "admin@biblio.ma", null, null);
            insertAdmin(admin, admin);
        }
        if (!hasDemoLecteur) {
            int id = nextId("utilisateur", "id_utilisateur");
            Lecteur lecteur = new Lecteur(id, "lecteur", "lecteur123", 987654321, "lecteur@biblio.ma", null, null);
            insertLecteur(lecteur, lecteur);
        }
    }

    // Retourne le prochain identifiant disponible d'une table.
    public static int nextId(String table, String column) {
        String query = "SELECT COALESCE(MAX(" + column + "), 0) + 1 FROM " + table;
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            ResultSet rs = st.executeQuery(query);
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Erreur nextId: " + e.getMessage());
        }
        return 1;
    }

    // Méthodes d'insertion

    public static void insertLivre(Livre livre) {
        String query = "INSERT INTO livre (id_livre, titre, auteur, genre, resume_livre, disponibilite) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, livre.getId());
            preparedStatement.setString(2, livre.getTitre());
            preparedStatement.setString(3, livre.getAuteur());
            preparedStatement.setString(4, livre.getGenre());
            preparedStatement.setString(5, livre.getResume());
            preparedStatement.setBoolean(6, livre.estDisponible());
            preparedStatement.executeUpdate();
            System.out.println("Le livre est insérré avec succé.");
        } catch (SQLException e) {
            System.out.println("Erreur d'insertion du livre: " + e.getMessage());
        }
    }

    public static void updateLivre(Livre livre) {
        String query = "UPDATE livre SET titre = ?, auteur = ?, genre = ?, resume_livre = ?, disponibilite = ? WHERE id_livre = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, livre.getTitre());
            ps.setString(2, livre.getAuteur());
            ps.setString(3, livre.getGenre());
            ps.setString(4, livre.getResume());
            ps.setBoolean(5, livre.estDisponible());
            ps.setInt(6, livre.getId());
            ps.executeUpdate();
            System.out.println("Livre modifié avec succé.");
        } catch (SQLException e) {
            System.out.println("Erreur de modification du livre: " + e.getMessage());
        }
    }

    public static void deleteLivre(int id) {
        String query = "DELETE FROM livre WHERE id_livre = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Livre supprimé avec succé.");
        } catch (SQLException e) {
            System.out.println("Erreur de suppression du livre: " + e.getMessage());
        }
    }

    public static void insertUtilisateur(Utilisateur utilisateur) {
        String query = "INSERT INTO utilisateur (id_utilisateur, nom, motDePasse, numero, email, role_utilisateur) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, utilisateur.getId());
            preparedStatement.setString(2, utilisateur.getNom());
            preparedStatement.setString(3, utilisateur.getMotdepasse());
            preparedStatement.setInt(4, utilisateur.getNumero());
            preparedStatement.setString(5, utilisateur.getEmail());
            preparedStatement.setString(6, utilisateur.getRole());
            preparedStatement.executeUpdate();
            System.out.println("Utilisateur est insérré avec succé.");
        } catch (SQLException e) {
            System.out.println("Erreur d'insertion d'utilisateur: " + e.getMessage());
        }
    }

    public static void insertAdmin(Admin admin, Utilisateur utilisateur) {
        insertUtilisateur(admin);
        String query = "INSERT INTO admin (id_admin, id_utilisateur) VALUES (?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, admin.getId());
            preparedStatement.setInt(2, utilisateur.getId());
            preparedStatement.executeUpdate();
            System.out.println("Admin est insérré avec succé.");
        } catch (SQLException e) {
            System.out.println("Erreur d'insertion d'admin: " + e.getMessage());
        }
    }

    public static void insertLecteur(Lecteur lecteur, Utilisateur utilisateur) {
        insertUtilisateur(lecteur);
        String query = "INSERT INTO lecteur (id_lecteur, id_utilisateur) VALUES (?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, lecteur.getId());
            preparedStatement.setInt(2, utilisateur.getId());
            preparedStatement.executeUpdate();
            System.out.println("Lecteur est insérré avec succé.");
        } catch (SQLException e) {
            System.out.println("Erreur d'insertion du lecteur: " + e.getMessage());
        }
    }

    public static void insertEmprunt(Emprunt emprunt) {
        String query = "INSERT INTO emprunt (id_emprunt, id_utilisateur, id_livre, dateEmprunts, dateRetour) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, emprunt.getId());
            preparedStatement.setInt(2, emprunt.getUtilisateur().getId());
            preparedStatement.setInt(3, emprunt.getLivre().getId());
            preparedStatement.setDate(4, java.sql.Date.valueOf(emprunt.getDateEmprunts()));
            preparedStatement.setDate(5, java.sql.Date.valueOf(emprunt.getDateRetour()));
            preparedStatement.executeUpdate();
            System.out.println("Emprunt est insérré avec succé.");
        } catch (SQLException e) {
            System.out.println("Erreur d'insertion d'emprunt: " + e.getMessage());
        }
    }

    public static void insertReservation(Reservation reservation, Utilisateur utilisateur) {
        String query = "INSERT INTO reservation (id_reservation, id_utilisateur, dateReservation) VALUES (?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, reservation.getId_reservation());
            preparedStatement.setInt(2, utilisateur.getId());
            preparedStatement.setDate(3, java.sql.Date.valueOf(reservation.getDateReservation()));
            preparedStatement.executeUpdate();
            System.out.println("Reservation est insérré avec succé.");
        } catch (SQLException e) {
            System.out.println("Erreur d'insertion d'une reservation: " + e.getMessage());
        }
    }

    // Méthodes de lecture

    public static ArrayList<Livre> getLivres() {
        ArrayList<Livre> livres = new ArrayList<>();
        String query = "SELECT id_livre, titre, auteur, genre, resume_livre, disponibilite FROM livre ORDER BY id_livre";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                livres.add(new Livre(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getBoolean(6)));
            }
        } catch (SQLException e) {
            System.out.println("Erreur lecture livres: " + e.getMessage());
        }
        return livres;
    }

    public static ArrayList<Utilisateur> getUtilisateurs() {
        ArrayList<Utilisateur> result = new ArrayList<>();
        String query = "SELECT id_utilisateur, nom, motDePasse, numero, email, role_utilisateur FROM utilisateur ORDER BY id_utilisateur";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt(1);
                String nom = rs.getString(2);
                String mdp = rs.getString(3);
                int num = rs.getInt(4);
                String email = rs.getString(5);
                String role = rs.getString(6);
                if ("Admin".equals(role)) {
                    result.add(new Admin(id, nom, mdp, num, email, null, null));
                } else {
                    result.add(new Lecteur(id, nom, mdp, num, email, null, null));
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur lecture utilisateurs: " + e.getMessage());
        }
        return result;
    }

    public static ArrayList<Lecteur> getLecteurs() {
        ArrayList<Lecteur> result = new ArrayList<>();
        for (Utilisateur u : getUtilisateurs()) {
            if (u instanceof Lecteur) result.add((Lecteur) u);
        }
        return result;
    }

    public static Utilisateur authentifier(String nom, String motDePasse) {
        String query = "SELECT id_utilisateur, nom, motDePasse, numero, email, role_utilisateur FROM utilisateur WHERE nom = ? AND motDePasse = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(query)) {
            ps.setString(1, nom);
            ps.setString(2, motDePasse);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    int num = rs.getInt(4);
                    String email = rs.getString(5);
                    String role = rs.getString(6);
                    if ("Admin".equals(role)) {
                        return new Admin(id, nom, motDePasse, num, email, null, null);
                    }
                    return new Lecteur(id, nom, motDePasse, num, email, null, null);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur authentification: " + e.getMessage());
        }
        return null;
    }

    public static ArrayList<Emprunt> getEmprunts() {
        ArrayList<Emprunt> result = new ArrayList<>();
        String query = "SELECT e.id_emprunt, u.id_utilisateur, u.nom, l.id_livre, l.titre, e.dateEmprunts, e.dateRetour " +
                       "FROM emprunt e JOIN utilisateur u ON e.id_utilisateur = u.id_utilisateur " +
                       "JOIN livre l ON e.id_livre = l.id_livre ORDER BY e.id_emprunt";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Lecteur user = new Lecteur(rs.getInt(2), rs.getString(3), "", 0, "", null, null);
                Livre livre = new Livre(rs.getInt(4), rs.getString(5), "", "", "", false);
                LocalDate dEmprunt = rs.getDate(6) != null ? rs.getDate(6).toLocalDate() : LocalDate.now();
                Emprunt e = new Emprunt(rs.getInt(1), user, livre, dEmprunt);
                if (rs.getDate(7) != null) e.setDateRetour(rs.getDate(7).toLocalDate());
                result.add(e);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lecture emprunts: " + e.getMessage());
        }
        return result;
    }

    public static ArrayList<Reservation> getReservations() {
        ArrayList<Reservation> result = new ArrayList<>();
        String query = "SELECT r.id_reservation, u.id_utilisateur, u.nom, r.dateReservation " +
                       "FROM reservation r JOIN utilisateur u ON r.id_utilisateur = u.id_utilisateur ORDER BY r.id_reservation";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Lecteur user = new Lecteur(rs.getInt(2), rs.getString(3), "", 0, "", null, null);
                LocalDate date = rs.getDate(4) != null ? rs.getDate(4).toLocalDate() : LocalDate.now();
                result.add(new Reservation(user, date, rs.getInt(1)));
            }
        } catch (SQLException e) {
            System.out.println("Erreur lecture reservations: " + e.getMessage());
        }
        return result;
    }
}
