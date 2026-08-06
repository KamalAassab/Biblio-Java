package Interfaces;

import java.time.LocalDate;

public interface Reservation {
    int getIdReservation();
    LocalDate getDateReservation();
    Utilisateur getUtilisateur();
    void afficherInfosReservation();
}
