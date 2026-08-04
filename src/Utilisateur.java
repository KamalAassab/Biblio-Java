import java.util.ArrayList;

public abstract class Utilisateur {

    // Attributs
    private int id;
    private String nom;
    private String motDePasse;
    private int numero;
    private String email;
    private ArrayList<Livre> historiqueEmprunts; // Liste d'historiques des livres empruntés
    private ArrayList<String> commentaires; // Liste des commentaires
    private ArrayList<Integer> notes; // Liste des notes
    private Catalogue catalogue;
    private Livre livre;
    private String role;


    // Constructeur avec paramètres
    public Utilisateur(int id, String nom, String motDePasse, int numero, String email,Catalogue catalogue,Livre livre ) {
        this.id = id; // Corrected this line to assign the id
        this.nom = nom;
        this.motDePasse = motDePasse;
        this.numero = numero;
        this.email = email;
        this.catalogue = catalogue;
        this.livre = livre;
        this.historiqueEmprunts = new ArrayList<>();
        this.commentaires = new ArrayList<>(); // initialiser commentaires
        this.notes = new ArrayList<>(); // Initialiser notes
        this.role = role;
    }

    // Méthode pour commenter un livre
    public void commenter(Livre livre, String commentaire) {
        commentaires.add(commentaire); // Stocker le commentaire
        System.out.println(nom + " a commenté sur le livre " + livre.getTitre() + " : " + commentaire);
    }

    // Méthode pour noter un livre
    public void noter(Livre livre, int note) {
        notes.add(note); // Stocker la note
        System.out.println(nom + " a noté le livre " + livre.getTitre() + " : " + note);
    }

    // Méthode ajouterEmprunt pour ajouter un emprunt
    public void ajouterEmprunt(Livre livre) {
        historiqueEmprunts.add(livre);
    }

    // Méthode creerListePersonnalise pour créer une liste personnalisée
    public ArrayList<Livre> creerListePersonnalise(Livre livre) {
        ArrayList<Livre> listePersonnalise = new ArrayList<>();
        listePersonnalise.add(livre);
        return listePersonnalise;
    }

    // Méthode sInscrire
    public void sInscrire() {
        System.out.println("Inscription reussie pour " + nom);
        // You can add more code here to store the user data in a database, etc.
    }

    // Méthode seConnecter
    public void seConnecter() {
        // Implement connection logic here BDD
        System.out.println(nom + " vous etes connecte.");
    }

    public void rechercheLivre(String titre){
        catalogue.rechercheLivre(titre);
    }
    public void rechercheParAuteur(String auteur){
        catalogue.rechercheParAuteur(auteur);
    }
    public void rechercheParGenre(String genre){
        catalogue.rechercheParGenre(genre);
    }

    //Méthode abstract
    public abstract void afficherInfos();

    // Getters et setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getMotdepasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public ArrayList<String> getCommentaires() {
        return commentaires;
    }

    public ArrayList<Integer> getNotes() {
        return notes;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ArrayList<Livre> getHistoriqueEmprunts() {
        return historiqueEmprunts;
    }

    public void setHistoriqueEmprunts(ArrayList<Livre> historiqueEmprunts) {
        this.historiqueEmprunts = historiqueEmprunts;
    }

    public Catalogue getCatalogue() {
        return catalogue;
    }

    public void setCatalogue(Catalogue catalogue) {
        this.catalogue = catalogue;
    }

    public Livre getLivre() {
        return livre;
    }

    public void setLivre(Livre livre) {
        this.livre = livre;
    }

    public String getRole() {return role;}

    public void setRole(String role) {this.role = role;}

    @Override
    public String toString() {
        return nom;
    }
}