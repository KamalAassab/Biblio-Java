import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BookDialog extends javax.swing.JDialog {
    private final boolean adminMode;
    private final boolean viewMode;
    private final Livre existing;
    private final Runnable onSaved;
    private final RoundedTextField titre = new RoundedTextField("Titre du livre");
    private final RoundedTextField auteur = new RoundedTextField("Auteur");
    private final RoundedTextField genre = new RoundedTextField("Genre");
    private final JTextArea resume = new JTextArea();
    private final TogglePill dispo = new TogglePill(true, null);
    private RoundedPanel root;
    private JLabel errorLabel;

    public static void add(Component owner, Runnable onSaved) {
        new BookDialog(owner, null, false, onSaved).setVisible(true);
    }

    public static void edit(Component owner, Livre livre, Runnable onSaved) {
        new BookDialog(owner, livre, false, onSaved).setVisible(true);
    }

    public static void view(Component owner, Livre livre) {
        new BookDialog(owner, livre, true, null).setVisible(true);
    }

    private BookDialog(Component owner, Livre livre, boolean view, Runnable onSaved) {
        super((Window) javax.swing.SwingUtilities.getWindowAncestor(owner), Dialog.ModalityType.APPLICATION_MODAL);
        this.existing = livre;
        this.viewMode = view;
        this.adminMode = livre != null || !view;
        this.onSaved = onSaved;
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        build();
        if (livre != null) {
            titre.setText(livre.getTitre());
            auteur.setText(livre.getAuteur());
            genre.setText(livre.getGenre());
            resume.setText(livre.getResume());
            dispo.setOn(livre.estDisponible());
        }
        Dialogs.showWithOverlay(this, root, owner);
    }

    private void build() {
        root = new RoundedPanel(Color.WHITE, 24);
        root.setLayout(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        Chrome.makeDraggable(top, this);
        JLabel titleLabel = Dialogs.title(viewMode ? "Détails du livre" : (existing != null ? "Modifier le livre" : "Ajouter un livre"));
        top.add(titleLabel, BorderLayout.WEST);
        IconButton close = new IconButton(Icons.Kind.CLOSE, 16).withColors(Theme.MUTED, new Color(255, 228, 230), Theme.DANGER);
        close.addActionListener(e -> dispose());
        top.add(close, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 14, 0);

        addField(form, gbc, "Titre", titre);
        addField(form, gbc, "Auteur", auteur);
        addField(form, gbc, "Genre", genre);

        gbc.gridy++;
        JLabel lr = fieldLabel("Résumé");
        form.add(lr, gbc);
        gbc.gridy++;
        resume.setFont(Theme.FONT);
        resume.setLineWrap(true);
        resume.setWrapStyleWord(true);
        resume.setOpaque(false);
        resume.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        RoundedPanel resumeWrap = new RoundedPanel(Theme.FIELD, 14);
        resumeWrap.setLayout(new BorderLayout());
        resumeWrap.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JScrollPane rsp = new JScrollPane(resume);
        rsp.setOpaque(false);
        rsp.getViewport().setOpaque(false);
        rsp.setBorder(BorderFactory.createEmptyBorder());
        resumeWrap.add(rsp, BorderLayout.CENTER);
        resumeWrap.setPreferredSize(new Dimension(0, 130));
        form.add(resumeWrap, gbc);

        if (!viewMode) {
            gbc.gridy++;
            gbc.insets = new Insets(6, 0, 14, 0);
            JPanel dispoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            dispoRow.setOpaque(false);
            JLabel ld = new JLabel("Disponibilité");
            ld.setFont(Theme.SMALL_BOLD);
            ld.setForeground(Theme.TEXT);
            dispoRow.add(ld);
            dispoRow.add(dispo);
            form.add(dispoRow, gbc);
        }

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 6, 0);
        errorLabel = new JLabel(" ");
        errorLabel.setFont(Theme.SMALL);
        errorLabel.setForeground(Theme.DANGER);
        form.add(errorLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 0, 0);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.setOpaque(false);
        if (viewMode) {
            RoundedButton ok = new RoundedButton("Fermer", RoundedButton.Style.PRIMARY);
            ok.addActionListener(e -> dispose());
            buttons.add(ok);
        } else {
            if (existing != null) {
                RoundedButton del = new RoundedButton("Supprimer", RoundedButton.Style.DANGER);
                del.addActionListener(e -> doDelete());
                buttons.add(del);
            }
            RoundedButton cancel = new RoundedButton("Annuler", RoundedButton.Style.SECONDARY);
            cancel.addActionListener(e -> dispose());
            RoundedButton save = new RoundedButton("Enregistrer", RoundedButton.Style.PRIMARY);
            save.addActionListener(e -> doSave());
            buttons.add(cancel);
            buttons.add(save);
        }
        form.add(buttons, gbc);

        root.add(form, BorderLayout.CENTER);
        root.setPreferredSize(new Dimension(520, 560));
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.SMALL_BOLD);
        l.setForeground(Theme.TEXT);
        return l;
    }

    private void addField(JPanel form, GridBagConstraints gbc, String label, JComponent field) {
        gbc.gridy++;
        form.add(fieldLabel(label), gbc);
        gbc.gridy++;
        field.setPreferredSize(new Dimension(0, 46));
        form.add(field, gbc);
    }

    private void doSave() {
        String t = titre.getText().trim();
        String a = auteur.getText().trim();
        if (t.isEmpty() || a.isEmpty()) {
            errorLabel.setText("Le titre et l'auteur sont obligatoires.");
            return;
        }
        String g = genre.getText().trim();
        String r = resume.getText().trim();
        boolean d = dispo.isOn();
        if (existing == null) {
            int id = DatabaseConnection.nextId("livre", "id_livre");
            DatabaseConnection.insertLivre(new Livre(id, t, a, g, r, d));
            Toast.show(this, "Livre ajouté avec succès");
        } else {
            Livre upd = new Livre(existing.getId(), t, a, g, r, d);
            DatabaseConnection.updateLivre(upd);
            Toast.show(this, "Livre modifié avec succès");
        }
        if (onSaved != null) onSaved.run();
        dispose();
    }

    private void doDelete() {
        DatabaseConnection.deleteLivre(existing.getId());
        Toast.show(this, "Livre supprimé");
        if (onSaved != null) onSaved.run();
        dispose();
    }
}
