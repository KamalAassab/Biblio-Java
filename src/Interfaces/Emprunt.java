package Interfaces;

import java.time.LocalDate;

public interface Emprunt {
    int getId();
    Utilisateur getUtilisateur();
    Livre getLivre();
    LocalDate getDateEmprunts();
    LocalDate getDateRetour();
    int calculeDeRetard();
}
