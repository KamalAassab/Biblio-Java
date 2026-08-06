import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Bilingual string catalogue (Français / English) for the desktop application.
 *
 * <p>Kept as an in-code map rather than .properties bundles so the packaged .exe
 * has no external resource to lose, and so a missing key fails loudly at build
 * time rather than silently rendering "???" to a user.
 *
 * <p>The chosen language persists across launches via {@link Preferences}.
 * Components register with {@link #onChange} to rebuild their labels live.
 */
public final class I18n {

    private I18n() {}

    public enum Lang {
        FR("Français", "FR"),
        EN("English", "EN");

        public final String label;
        public final String code;

        Lang(String label, String code) {
            this.label = label;
            this.code = code;
        }
    }

    private static final String PREF_KEY = "language";
    private static final List<Runnable> LISTENERS = new ArrayList<>();
    private static Lang current = loadPreference();

    // ── Public API ───────────────────────────────────────────────────────────

    public static Lang language() {
        return current;
    }

    public static boolean isFrench() {
        return current == Lang.FR;
    }

    public static void setLanguage(Lang lang) {
        if (lang == null || lang == current) return;
        current = lang;
        savePreference(lang);
        for (Runnable r : new ArrayList<>(LISTENERS)) {
            try {
                r.run();
            } catch (RuntimeException ignored) {
                // A single misbehaving listener must not block the rest of the UI from updating.
            }
        }
    }

    public static void toggleLanguage() {
        setLanguage(current == Lang.FR ? Lang.EN : Lang.FR);
    }

    /** Registers a callback fired whenever the language changes. */
    public static void onChange(Runnable listener) {
        if (listener != null) LISTENERS.add(listener);
    }

    /** Translates a key, optionally interpolating {0}, {1}… arguments. */
    public static String t(String key, Object... args) {
        Map<String, String> table = current == Lang.FR ? FR : EN;
        String value = table.get(key);
        if (value == null) value = FR.get(key);
        if (value == null) value = key;
        if (args == null || args.length == 0) {
            // Catalogue entries are written in MessageFormat syntax, where a literal
            // apostrophe is escaped as ''. Undo that when no formatting pass will run.
            return value.replace("''", "'");
        }
        return new MessageFormat(value, current == Lang.FR ? Locale.FRENCH : Locale.ENGLISH).format(args);
    }

    /** Formats a date in the conventions of the active language. */
    public static String date(java.time.LocalDate d) {
        if (d == null) return "—";
        return d.format(java.time.format.DateTimeFormatter.ofPattern(
                current == Lang.FR ? "dd/MM/yyyy" : "MMM d, yyyy",
                current == Lang.FR ? Locale.FRENCH : Locale.ENGLISH));
    }

    private static Lang loadPreference() {
        try {
            String saved = Preferences.userNodeForPackage(I18n.class).get(PREF_KEY, null);
            if (saved != null) return Lang.valueOf(saved);
        } catch (RuntimeException ignored) {
            // Preferences can be unavailable in locked-down environments; fall through to the default.
        }
        // Follow the OS locale on first run, defaulting to French for FST Settat.
        return Locale.getDefault().getLanguage().equals("en") ? Lang.EN : Lang.FR;
    }

    private static void savePreference(Lang lang) {
        try {
            Preferences.userNodeForPackage(I18n.class).put(PREF_KEY, lang.name());
        } catch (RuntimeException ignored) {
        }
    }

    // ── Catalogue ────────────────────────────────────────────────────────────

    private static final Map<String, String> FR = new LinkedHashMap<>();
    private static final Map<String, String> EN = new LinkedHashMap<>();

    private static void put(String key, String fr, String en) {
        FR.put(key, fr);
        EN.put(key, en);
    }

    static {
        // ── Identity ────────────────────────────────────────────────────────
        put("app.name", "BiblioTech", "BiblioTech");
        put("app.tagline", "Bibliothèque FST Settat", "FST Settat Library");
        put("app.university", "Faculté des Sciences et Techniques de Settat",
                             "Faculty of Sciences and Techniques, Settat");
        put("app.university.short", "FST Settat", "FST Settat");

        // ── Navigation ──────────────────────────────────────────────────────
        put("nav.dashboard", "Tableau de bord", "Dashboard");
        put("nav.catalogue", "Catalogue", "Catalogue");
        put("nav.emprunts", "Emprunts", "Loans");
        put("nav.reservations", "Réservations", "Reservations");
        put("nav.utilisateurs", "Utilisateurs", "Members");
        put("nav.profile", "Mon profil", "My profile");
        put("nav.logout", "Déconnexion", "Sign out");
        put("nav.menu.primary", "Menu", "Menu");
        put("nav.menu.account", "Compte", "Account");

        // ── Page headers ────────────────────────────────────────────────────
        put("page.dashboard.title", "Tableau de bord", "Dashboard");
        put("page.dashboard.sub", "L''essentiel de votre bibliothèque en un coup d''œil",
                                  "Your library at a glance");
        put("page.catalogue.title", "Catalogue", "Catalogue");
        put("page.catalogue.sub", "Parcourez et gérez l''ensemble du fonds documentaire",
                                  "Browse and manage the full collection");
        put("page.emprunts.title", "Emprunts", "Loans");
        put("page.emprunts.sub", "Suivez les prêts en cours et les retours attendus",
                                 "Track active loans and expected returns");
        put("page.reservations.title", "Réservations", "Reservations");
        put("page.reservations.sub", "Gérez les demandes de réservation des lecteurs",
                                     "Manage reader reservation requests");
        put("page.utilisateurs.title", "Utilisateurs", "Members");
        put("page.utilisateurs.sub", "Comptes, rôles et coordonnées des membres",
                                     "Accounts, roles and member contact details");

        // ── Login ───────────────────────────────────────────────────────────
        put("login.welcome", "Bon retour parmi nous", "Welcome back");
        put("login.subtitle", "Connectez-vous pour accéder à la bibliothèque",
                              "Sign in to access the library");
        put("login.username", "Identifiant", "Username");
        put("login.username.hint", "Votre nom d''utilisateur", "Your username");
        put("login.password", "Mot de passe", "Password");
        put("login.password.hint", "Votre mot de passe", "Your password");
        put("login.submit", "Se connecter", "Sign in");
        put("login.submitting", "Connexion…", "Signing in…");
        put("login.error.empty", "Renseignez votre identifiant et votre mot de passe.",
                                 "Enter both your username and password.");
        put("login.error.invalid", "Identifiant ou mot de passe incorrect.",
                                   "Incorrect username or password.");
        put("login.error.locked", "Trop de tentatives. Réessayez dans {0} seconde(s).",
                                  "Too many attempts. Try again in {0} second(s).");
        put("login.error.connection", "Connexion à la base impossible. Vérifiez votre configuration.",
                                      "Cannot reach the database. Check your configuration.");
        put("login.hero.title", "La bibliothèque universitaire,\nrepensée.",
                                "The university library,\nreimagined.");
        put("login.hero.sub", "Catalogue, emprunts et réservations réunis dans une interface unique et rapide.",
                              "Catalogue, loans and reservations in one fast, unified workspace.");
        put("login.demo.title", "Comptes de démonstration", "Demo accounts");
        put("login.demo.admin", "Administrateur", "Administrator");
        put("login.demo.reader", "Lecteur", "Reader");

        // ── Dashboard ───────────────────────────────────────────────────────
        put("dash.greeting.morning", "Bonjour, {0}", "Good morning, {0}");
        put("dash.greeting.afternoon", "Bon après-midi, {0}", "Good afternoon, {0}");
        put("dash.greeting.evening", "Bonsoir, {0}", "Good evening, {0}");
        put("dash.stat.books", "Livres au catalogue", "Books in catalogue");
        put("dash.stat.available", "Disponibles", "Available now");
        put("dash.stat.loans", "Emprunts en cours", "Active loans");
        put("dash.stat.reservations", "Réservations", "Reservations");
        put("dash.stat.members", "Membres inscrits", "Registered members");
        put("dash.stat.overdue", "Retards", "Overdue");
        put("dash.actions.title", "Actions rapides", "Quick actions");
        put("dash.action.browse", "Parcourir le catalogue", "Browse catalogue");
        put("dash.action.browse.sub", "Rechercher un ouvrage", "Find a title");
        put("dash.action.addBook", "Ajouter un livre", "Add a book");
        put("dash.action.addBook.sub", "Enrichir le fonds", "Grow the collection");
        put("dash.action.newLoan", "Nouvel emprunt", "New loan");
        put("dash.action.newLoan.sub", "Enregistrer un prêt", "Record a loan");
        put("dash.action.newReservation", "Nouvelle réservation", "New reservation");
        put("dash.action.newReservation.sub", "Réserver pour un lecteur", "Reserve for a reader");
        put("dash.recent.title", "Ajouts récents", "Recently added");
        put("dash.viewAll", "Tout voir", "View all");
        put("dash.recent.empty", "Aucune activité pour le moment", "Nothing has happened yet");
        put("dash.availability", "{0} sur {1} disponibles", "{0} of {1} available");

        // ── Catalogue ───────────────────────────────────────────────────────
        put("cat.search", "Rechercher par titre, auteur ou genre…",
                          "Search by title, author or genre…");
        put("cat.filter.all", "Tous", "All");
        put("cat.filter.available", "Disponibles", "Available");
        put("cat.filter.borrowed", "Empruntés", "On loan");
        put("cat.add", "Ajouter un livre", "Add book");
        put("cat.count", "{0} livre(s)", "{0} book(s)");
        put("cat.empty.title", "Aucun livre trouvé", "No books found");
        put("cat.empty.sub", "Essayez un autre terme de recherche ou ajustez les filtres.",
                             "Try a different search term or adjust the filters.");
        put("cat.empty.none", "Le catalogue est vide", "The catalogue is empty");
        put("cat.empty.none.sub", "Ajoutez votre premier ouvrage pour démarrer.",
                                  "Add your first title to get started.");

        // ── Book ────────────────────────────────────────────────────────────
        put("book.available", "Disponible", "Available");
        put("book.borrowed", "Emprunté", "On loan");
        put("book.title", "Titre", "Title");
        put("book.author", "Auteur", "Author");
        put("book.genre", "Genre", "Genre");
        put("book.summary", "Résumé", "Summary");
        put("book.status", "Statut", "Status");
        put("book.add.title", "Ajouter un livre", "Add a book");
        put("book.add.sub", "Renseignez les informations de l''ouvrage",
                            "Fill in the details of the title");
        put("book.edit.title", "Modifier le livre", "Edit book");
        put("book.edit.sub", "Mettez à jour les informations de l''ouvrage",
                             "Update the details of this title");
        put("book.view.title", "Détails du livre", "Book details");
        put("book.placeholder.title", "ex. Le Petit Prince", "e.g. The Little Prince");
        put("book.placeholder.author", "ex. Antoine de Saint-Exupéry", "e.g. Antoine de Saint-Exupéry");
        put("book.placeholder.genre", "ex. Roman", "e.g. Novel");
        put("book.placeholder.summary", "Quelques lignes sur l''ouvrage…",
                                        "A few lines about this title…");

        // ── Loans ───────────────────────────────────────────────────────────
        put("loan.new", "Nouvel emprunt", "New loan");
        put("loan.reader", "Lecteur", "Reader");
        put("loan.book", "Livre", "Book");
        put("loan.borrowedOn", "Emprunté le", "Borrowed on");
        put("loan.dueOn", "Retour prévu", "Due on");
        put("loan.status.active", "En cours", "Active");
        put("loan.status.overdue", "En retard", "Overdue");
        put("loan.status.returned", "Rendu", "Returned");
        put("loan.dueIn", "Dans {0} jour(s)", "In {0} day(s)");
        put("loan.overdueBy", "{0} jour(s) de retard", "{0} day(s) overdue");
        put("loan.dueToday", "Aujourd''hui", "Today");
        put("loan.empty.title", "Aucun emprunt", "No loans yet");
        put("loan.empty.sub", "Les prêts enregistrés apparaîtront ici.",
                              "Recorded loans will show up here.");
        put("loan.new.sub", "Sélectionnez un lecteur et un ouvrage disponible",
                            "Pick a reader and an available title");
        put("loan.duration", "Durée du prêt", "Loan period");
        put("loan.duration.days", "{0} jours", "{0} days");

        // ── Reservations ────────────────────────────────────────────────────
        put("res.new", "Nouvelle réservation", "New reservation");
        put("res.reader", "Lecteur", "Reader");
        put("res.date", "Date de réservation", "Reservation date");
        put("res.empty.title", "Aucune réservation", "No reservations");
        put("res.empty.sub", "Les demandes des lecteurs apparaîtront ici.",
                             "Reader requests will show up here.");
        put("res.new.sub", "Enregistrez une demande au nom d''un lecteur",
                           "Record a request on behalf of a reader");

        // ── Members ─────────────────────────────────────────────────────────
        put("user.name", "Nom", "Name");
        put("user.email", "E-mail", "Email");
        put("user.phone", "Téléphone", "Phone");
        put("user.role", "Rôle", "Role");
        put("user.role.admin", "Administrateur", "Administrator");
        put("user.role.reader", "Lecteur", "Reader");
        put("user.empty.title", "Aucun utilisateur", "No members");
        put("user.empty.sub", "Les comptes créés apparaîtront ici.",
                              "Created accounts will show up here.");

        // ── Common actions ──────────────────────────────────────────────────
        put("action.save", "Enregistrer", "Save");
        put("action.cancel", "Annuler", "Cancel");
        put("action.delete", "Supprimer", "Delete");
        put("action.edit", "Modifier", "Edit");
        put("action.view", "Consulter", "View");
        put("action.close", "Fermer", "Close");
        put("action.confirm", "Confirmer", "Confirm");
        put("action.refresh", "Actualiser", "Refresh");
        put("action.search", "Rechercher", "Search");
        put("action.create", "Créer", "Create");
        put("action.back", "Retour", "Back");
        put("action.language", "Langue", "Language");
        put("action.select", "Sélectionner…", "Select…");

        // ── Feedback ────────────────────────────────────────────────────────
        put("toast.book.added", "Livre ajouté au catalogue", "Book added to the catalogue");
        put("toast.book.updated", "Livre mis à jour", "Book updated");
        put("toast.book.deleted", "Livre supprimé", "Book deleted");
        put("toast.loan.created", "Emprunt enregistré", "Loan recorded");
        put("toast.loan.returned", "Retour enregistré", "Return recorded");
        put("toast.res.created", "Réservation enregistrée", "Reservation recorded");
        put("toast.res.deleted", "Réservation annulée", "Reservation cancelled");
        put("toast.error", "Une erreur est survenue", "Something went wrong");
        put("toast.saved", "Modifications enregistrées", "Changes saved");
        put("toast.language", "Langue : {0}", "Language: {0}");

        put("confirm.delete.book.title", "Supprimer ce livre ?", "Delete this book?");
        put("confirm.delete.book.body",
                "« {0} » sera définitivement retiré du catalogue. Cette action est irréversible.",
                "“{0}” will be permanently removed from the catalogue. This cannot be undone.");
        put("confirm.delete.res.title", "Annuler cette réservation ?", "Cancel this reservation?");
        put("confirm.delete.res.body", "La réservation sera définitivement supprimée.",
                                       "The reservation will be permanently deleted.");
        put("confirm.delete.user.title", "Supprimer ce compte ?", "Delete this account?");
        put("confirm.delete.user.body",
                "Le compte de « {0} » ainsi que ses emprunts et réservations seront supprimés.",
                "The account for “{0}”, along with its loans and reservations, will be deleted.");
        put("confirm.logout.title", "Se déconnecter ?", "Sign out?");
        put("confirm.logout.body", "Vous reviendrez à l''écran de connexion.",
                                   "You'll be returned to the sign-in screen.");
        put("confirm.return.title", "Enregistrer le retour ?", "Record this return?");
        put("confirm.return.body", "« {0} » redeviendra disponible au catalogue.",
                                   "“{0}” will become available in the catalogue again.");
        put("toast.noAlerts", "Aucun retard à signaler", "No overdue loans");

        // ── Profile ─────────────────────────────────────────────────────────
        put("profile.title", "Mon profil", "My profile");
        put("profile.sub", "Gérez vos informations et votre mot de passe",
                           "Manage your details and password");
        put("profile.details", "Informations personnelles", "Personal details");
        put("profile.security", "Sécurité", "Security");
        put("profile.password.current", "Mot de passe actuel", "Current password");
        put("profile.password.new", "Nouveau mot de passe", "New password");
        put("profile.password.confirm", "Confirmer le mot de passe", "Confirm password");
        put("profile.password.change", "Modifier le mot de passe", "Change password");
        put("profile.password.mismatch", "Les deux mots de passe ne correspondent pas.",
                                         "The two passwords do not match.");
        put("profile.password.wrong", "Le mot de passe actuel est incorrect.",
                                      "Your current password is incorrect.");
        put("profile.password.changed", "Mot de passe mis à jour", "Password updated");
        put("profile.updated", "Profil mis à jour", "Profile updated");
        put("profile.memberSince", "Membre depuis", "Member since");
        put("profile.activity", "Votre activité", "Your activity");
        put("profile.loans", "Emprunts", "Loans");
        put("profile.reservations", "Réservations", "Reservations");
        put("profile.hint.password",
                "8 caractères minimum, en combinant au moins trois types : minuscule, majuscule, chiffre, symbole.",
                "At least 8 characters, mixing at least three of: lowercase, uppercase, digit, symbol.");

        // ── Errors ──────────────────────────────────────────────────────────
        put("error.title.required", "Le titre est obligatoire (200 caractères maximum).",
                                    "A title is required (200 characters maximum).");
        put("error.author.required", "L''auteur est obligatoire (120 caractères maximum).",
                                     "An author is required (120 characters maximum).");
        put("error.genre.invalid", "Le genre ne peut dépasser 60 caractères.",
                                   "Genre cannot exceed 60 characters.");
        put("error.summary.invalid", "Le résumé ne peut dépasser 4 000 caractères.",
                                     "Summary cannot exceed 4,000 characters.");
        put("error.username.invalid",
                "L''identifiant doit contenir entre 3 et 80 caractères (lettres, chiffres, espace, . _ - ').",
                "Username must be 3–80 characters (letters, digits, space, . _ - ').");
        put("error.email.invalid", "Adresse e-mail invalide.", "Invalid email address.");
        put("error.phone.invalid", "Numéro de téléphone invalide.", "Invalid phone number.");
        put("error.password.length", "Le mot de passe doit contenir entre 8 et 128 caractères.",
                                     "Password must be between 8 and 128 characters.");
        put("error.password.weak",
                "Le mot de passe doit combiner au moins trois types : minuscule, majuscule, chiffre, symbole.",
                "Password must mix at least three of: lowercase, uppercase, digit, symbol.");
        put("error.notAllowed", "Vous n''avez pas les droits pour cette action.",
                                "You don't have permission for this action.");
        put("error.selectRow", "Sélectionnez d''abord une ligne.", "Select a row first.");
        put("error.cannotDeleteSelf", "Vous ne pouvez pas supprimer votre propre compte.",
                                      "You cannot delete your own account.");
        put("error.date.invalid", "Date invalide. Format attendu : AAAA-MM-JJ.",
                                  "Invalid date. Expected format: YYYY-MM-DD.");
        put("toast.user.deleted", "Compte supprimé", "Account deleted");
        put("error.reader.required", "Sélectionnez un lecteur.", "Select a reader.");
        put("error.book.required", "Sélectionnez un livre.", "Select a book.");
        put("error.book.unavailable", "Ce livre est déjà emprunté.", "This book is already on loan.");
        put("error.db", "La base de données est injoignable.", "The database is unreachable.");

        // ── Database setup ──────────────────────────────────────────────────
        put("db.missing.title", "Configuration requise", "Configuration required");
        put("db.missing.body",
                "Aucune connexion à la base n''est configurée.\n\nDéfinissez la variable d''environnement "
                        + "DATABASE_URL, ou créez un fichier .env à la racine de l''application.",
                "No database connection is configured.\n\nSet the DATABASE_URL environment variable, "
                        + "or create a .env file next to the application.");

        // ── Credits ─────────────────────────────────────────────────────────
        put("credit.eyebrow", "Réalisé par", "Built by");
        put("credit.builtBy", "Conçu et développé par Kamal Aassab",
                              "Designed and built by Kamal Aassab");
        put("credit.portfolio", "kamal-aassab.vercel.app", "kamal-aassab.vercel.app");
        put("credit.project", "Projet académique — {0}", "Academic project — {0}");
        put("credit.about", "À propos", "About");
        put("credit.about.body",
                "BiblioTech est une application de gestion de bibliothèque universitaire réalisée "
                        + "dans le cadre d''un projet Java à la Faculté des Sciences et Techniques de Settat.",
                "BiblioTech is a university library management application built as a Java project "
                        + "at the Faculty of Sciences and Techniques, Settat.");
    }
}
