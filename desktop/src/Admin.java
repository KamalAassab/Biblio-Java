public class Admin extends Utilisateur {

    //Constructeur paramétré
    public Admin(int id, String nom, String motDePasse, int numero, String email,Catalogue catalogue,Livre livre) {
        super(id,nom, motDePasse, numero, email,catalogue, livre);
        super.setRole("Admin");
    }

    // Méthode ajouterLivre
    public void ajouterLivre(Livre livre){

        getCatalogue().ajouterLivre(livre);
    }

    // Méthode afficherInfos
    @Override
    public void afficherInfos() {
        System.out.println("l'admin numero " + getId() + " nommé " + getNom() + ", son email: " + getEmail() + " et son numero de telephone: " + getNumero());
    }
}
