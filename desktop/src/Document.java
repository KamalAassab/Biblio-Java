public class Document {
    //Attributs
    private int id;
    private String titre;
    private String auteur;
    private String genre;

    //Constructeur avec paramètres
    public Document(int id, String titre, String auteur, String genre) {
        this.id = id;
        this.titre = titre;
        this.auteur = auteur;
        this.genre = genre;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getAuteur() {
        return auteur;
    }

    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }
}
