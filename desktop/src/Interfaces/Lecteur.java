package Interfaces;

public interface Lecteur extends Utilisateur {
    void emprunterLivre(Livre livre);
    void retournerLivre(Livre livre);
}
