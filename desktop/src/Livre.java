public class Livre extends Document{

    // Attributs
    private String resume;
    private boolean disponibilite;

    /**
     * URL de la couverture, renseignée par `npm run db:covers` côté web.
     * Null quand aucune illustration n'a été trouvée : l'application dessine alors
     * la couverture générée (dégradé + titre).
     *
     * Volontairement hors du constructeur, qui reste celui du sujet.
     */
    private String imageUrl;

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}
