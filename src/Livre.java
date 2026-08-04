public class Livre extends Document{

    // Attributs
    private String resume;
    private boolean disponibilite;

    // Constructeur avec paramètres
    public Livre(int id, String titre, String auteur, String genre, String resume, boolean disponibilite) {
        super(id, titre, auteur, genre);
        this.resume = resume;
        this.disponibilite = disponibilite;
    }

    // Methode afficherDetaille de Livre
    public void afficherDetaille() {
        System.out.println("Le livre de numéro: " + getId() + " intitulé: " + getTitre() + " auteur: " + getAuteur() + " genre: " + getGenre() + " resume: " + getResume());
    }

    //Getters et Setters
    public String getResume() {
        return resume;
    }

    @Override
    public String toString() {
        return getTitre() + " — " + getAuteur();
    }
    public void setResume(String resume) {
        this.resume = resume;
    }

    // estDisponible : retourne vrai si le livre est disponible et faux sinon
    public boolean estDisponible() {
        return disponibilite;
    }

    public void setDisponibilite(boolean disponibilite) {
        this.disponibilite = disponibilite;
    }

}
