public class Video extends Document {

    //Attribut specifique
    private int duree;


    //Constructeur avec parametres
    public Video (int id, String titre, String auteur, String genre, int duree) {
        super(id, titre, auteur, genre);
        this.duree = duree;
    }

    //Getter et Setter
    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

}