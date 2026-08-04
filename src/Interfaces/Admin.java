package Interfaces;

public interface Admin extends Utilisateur {
    void ajouterLivre(Livre livre);
    void supprimerLivre(Livre livre);
    void modifierLivre(Livre livre);
}
