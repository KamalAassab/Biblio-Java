import java.time.LocalDate;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        // Creation d'un catalogue
        Catalogue catalogue = new Catalogue();

        // Creation des livres
        Livre livre1 = new Livre(1, "La Boîte à Merveilles", "Ahmed Sefrioui", "autobiographique", "Ce roman raconte l'enfance de l'auteur à Fès, au Maroc.", true);
        Livre livre2 = new Livre(2, "Antigone", "Jean Anouilh", "Tragédie", "Antigone défie le roi Créon en enterrant son frère Polynice, condamné comme traître.", true);
        Livre livre3 = new Livre(3, "Le Dernier Jour d'un Condamné", "Victor Hugo", "Roman à thèse", "Un condamné à mort réfléchit à sa peur et à l'inhumanité de la peine de mort.", false);

        // Insertion des livres dans le catalogue
        catalogue.ajouterLivre(livre1);
        DatabaseConnection.insertLivre(livre1);

        catalogue.ajouterLivre(livre2);
        DatabaseConnection.insertLivre(livre2);

        catalogue.ajouterLivre(livre3);
        DatabaseConnection.insertLivre(livre3);

        // Creation d'un admin
        Admin admin = new Admin(1, "EL MOSTAFA ", "POO_JAVA", 123456789, "makroum.fsts@gmail.com", catalogue, null);
        DatabaseConnection.insertAdmin(admin, admin);

        // Admin creer un livre
        Livre livre4 = new Livre(4, "L'Étranger", "Albert Camus", "Roman", "Meursault, indifférent, commet un meurtre et est jugé, remettant en question le sens de la vie.", true);
        admin.ajouterLivre(livre4); // Ajoute le livre au catalogue
        DatabaseConnection.insertLivre(livre4); // Inserer le livre dans la base de donnée

        // l'Amdin va rechercher un livre par titre
        Livre livreRechercher = catalogue.rechercheLivre("Anitgone");
        if (livreRechercher != null) {
            System.out.println("livre trouvé: " + livreRechercher.getTitre());
        } else {
            System.out.println("Livre non trouvé.");
        }

        // l'Admin va rechercher un livre par auteur
        ArrayList<Livre> livresParAuteur = catalogue.rechercheParAuteur("Victor Hugo");
        System.out.println("Les livres de Victor Hugo:");
        for (Livre livre : livresParAuteur) {
            System.out.println(livre.getTitre());
        }

        // Creation d'un lecteur
        Lecteur lecteur = new Lecteur(2, "Nissrin", "SITD_POO", 987654321, "nissrin.fsts@gmail.com", catalogue, null);
        DatabaseConnection.insertLecteur(lecteur, lecteur);

        // Lecteur chercher un livre par genre
        ArrayList<Livre> livreParGenre = catalogue.rechercheParGenre("Tragédie");
        System.out.println("livres de genre Tragédie:");
        for (Livre livre : livreParGenre) {
            System.out.println(livre.getTitre());
        }

        // Lecteur ajouter un commentaire et une note sur un livre
        lecteur.commenter(livre1, "Interessant !");
        lecteur.noter(livre1, 8);

        // Creation d'un emprunt
        Emprunt emprunt = new Emprunt(5, lecteur, livre1, LocalDate.now());
        lecteur.ajouterEmprunt(livre1);
        DatabaseConnection.insertEmprunt(emprunt);
        System.out.println("Emprunt created for: " + emprunt.getLivre().getTitre());

        // Creation d'une reservation
        Reservation reservation = new Reservation(lecteur, LocalDate.now(), 1);
        reservation.afficherInfosReservation();
        DatabaseConnection.insertReservation(reservation, lecteur);

        //Afficher le catalogue
        System.out.println("Catalogue Actuel:");
        catalogue.afficherCatalogue();

    }
}