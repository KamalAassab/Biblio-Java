package Interfaces;
import java.util.ArrayList;
public interface Utilisateur {
        void sInscrire();
        void seConnecter();
        void commenter(Livre livre, String commentaire);
        void noter(Livre livre, int note);
        ArrayList<Livre> creerListePersonnalise(Livre livre);
        void ajouterEmprunt(Livre livre);
        void rechercheLivre(String titre);
        void rechercheParAuteur(String auteur);
        void rechercheParGenre(String genre);
    }

