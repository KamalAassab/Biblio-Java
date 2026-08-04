import java.time.LocalDate;

public class Emprunt {
    //Attributs
    private int id;
    private Utilisateur utilisateur;
    private Livre livre;
    private LocalDate dateEmprunts;
    private LocalDate dateRetour; // Date de retour supposée
    private LocalDate dateRetourLivre; // Date de retour que l'utilisateur a renseigné

    // constructeur avec paramètres
    public Emprunt(int id, Utilisateur utilisateur, Livre livre, LocalDate dateEmprunts) {
        this.id = id;
        this.utilisateur = utilisateur;
        this.livre = livre;
        this.dateEmprunts = dateEmprunts;
        this.dateRetour = dateEmprunts.plusDays(20);
        this.dateRetourLivre = null;
    }

    // Methode pour calculer le retard
    public int calculeDeRetard() {
        if (dateRetourLivre != null) { // Si la date de retour du livre est renseignée

            if (dateRetourLivre.compareTo(dateRetour) <= 0) { // Si la date de retour du livre est avant la date de retour supposée
                return 0; // pas de retard

            } else { // Si la date de retour du livre est apres la date de retour supposée

                return (int) (dateRetourLivre.toEpochDay() - dateRetour.toEpochDay()); // Retourne le nombre de jours de retard
            }
        }
        else{ // Si la date de retour du livre n'est pas renseignée
                if(LocalDate.now().compareTo(dateRetour)<=0) { // Si la date actuelle est avant la date de retour

                    return 0; // pas de retard

                } else { // Si la date actuelle est apres la date de retour

                    return (int) (LocalDate.now().toEpochDay() - dateRetour.toEpochDay());
                }
            }
    }


    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Livre getLivre() {
        return livre;
    }

    public void setLivre(Livre livre) {
        this.livre = livre;
    }

    public LocalDate getDateEmprunts() {
        return dateEmprunts;
    }

    public void setDateEmprunts(LocalDate dateEmprunts) {
        this.dateEmprunts = dateEmprunts;
    }

    public LocalDate getDateRetour() {
        return dateRetour;
    }

    public void setDateRetour(LocalDate dateRetour) {
        this.dateRetour = dateRetour;
    }

    public LocalDate getDateRetourLivre() {
        return dateRetourLivre;
    }
    public void setDateRetourLivre(LocalDate dateRetourLivre) {
        this.dateRetourLivre = dateRetourLivre;
    }
}
