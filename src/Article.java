public class Article extends Document {

    //Attribut specifique
    private int numArticle;

    //Constructeur avec parametres
    public Article(int id, String titre, String auteur, String genre, int numArticle) {
        super(id, titre, auteur, genre);
        this.numArticle = numArticle;
    }

    //Getter et Setter
    public int getNumArticle() {

        return numArticle;
    }

    public void setNumArticle(int numArticle) {

        this.numArticle = numArticle;
    }
}
