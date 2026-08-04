import java.time.LocalDate;

public class Reservation {
    //les attributs
    private LocalDate dateReservation;
    private Utilisateur utilisateur;
    private int id_reservation;
    //constructeurs
    public Reservation(Utilisateur utilisateur,LocalDate dateReservation,  int id_reservation) {
        this.utilisateur = utilisateur;
        this.dateReservation = dateReservation;
        this.id_reservation = id_reservation;
    }

    //getters et setters
    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public LocalDate getDateReservation() {
        return dateReservation;
    }

    public void setDateReservation(LocalDate dateReservation) {
        this.dateReservation = dateReservation;
    }

    public int getId_reservation() {
        return id_reservation;
    }

    public void setId_reservation(int id_reservation) {
        this.id_reservation = id_reservation;
    }

    // Methode pour afficher les infos de la reservation
    public void afficherInfosReservation() {
        System.out.println("La reservation du numéro: " + id_reservation + " est faite par " + utilisateur.getNom() + " le " + dateReservation);
    }
}
