import java.util.ArrayList;

public class Catalogue {
    // Attributs
    private ArrayList<Livre> listeLivres;

    // Constructeur par défaut
    public Catalogue() {

        this.listeLivres = new ArrayList<>();
    }

    // Constructeur avec paramètres
    public Catalogue(ArrayList<Livre> listeLivres) {

        this.listeLivres = listeLivres;
    }

    // Méthode rechercheLivre par titre
    public Livre rechercheLivre(String titre) {
        for (Livre livre : listeLivres) {
            if (livre.getTitre().equals(titre)) {
                return livre;
            }
        }
        return null;
    }


    // Méthode rechercheParAuteur
    public ArrayList<Livre> rechercheParAuteur(String auteur) {
        ArrayList<Livre> livresTrouves = new ArrayList<>();
        for (Livre livre : listeLivres) {
            if (livre.getAuteur().equals(auteur)) {
                livresTrouves.add(livre);
            }
        }
        return livresTrouves;
    }

    // Méthode rechercheParGenre
    public ArrayList<Livre> rechercheParGenre(String genre) {
        ArrayList<Livre> livresTrouves = new ArrayList<>();
        for (Livre livre : listeLivres) {
            if (livre.getGenre().equals(genre)) {
                livresTrouves.add(livre);
            }
        }
        return livresTrouves;
    }

    // Méthode ajouterLivre
    public void ajouterLivre(Livre livre) {

        listeLivres.add(livre);
    }


    // Méthode afficherCatalogue
    public void afficherCatalogue() {
        for (Livre livre : listeLivres) {
            livre.afficherDetaille();
        }
    }

}
