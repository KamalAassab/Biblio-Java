package Interfaces;

import java.util.ArrayList;

public interface Catalogue {
    void ajouterLivre(Livre livre);
    Livre rechercheLivre(String titre);
    ArrayList<Livre> rechercheParAuteur(String auteur);
    ArrayList<Livre> rechercheParGenre(String genre);
    void afficherCatalogue();
}
