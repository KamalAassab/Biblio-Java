public class Lecteur extends Utilisateur{

    // Constructeur
    public Lecteur(int id, String nom, String motDePasse, int numero, String email, Catalogue catalogue, Livre livre) {
        super(id, nom, motDePasse, numero, email, catalogue,livre);
        super.setRole("Lecteur");
    }

    // Redifinir la methode afficherInfos
    @Override
    public void afficherInfos() {
        System.out.println("lecteur numero " + getId() + " nommé " + getNom() + ", son email: " + getEmail() + " et son numero de telephone: " + getNumero());
    }

}
