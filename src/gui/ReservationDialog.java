import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.util.ArrayList;

public class ReservationDialog extends javax.swing.JDialog {
    private final JComboBox<Lecteur> lecteurCb = new JComboBox<>();
    private final RoundedTextField dateField = new RoundedTextField("AAAA-MM-JJ");
    private final Runnable onChanged;
    private RoundedPanel root;
    private JLabel errorLabel;

    public static void create(Component owner, Runnable onChanged) {
        new ReservationDialog(owner, onChanged).setVisible(true);
    }

    private ReservationDialog(Component owner, Runnable onChanged) {
        super((Window) javax.swing.SwingUtilities.getWindowAncestor(owner), Dialog.ModalityType.APPLICATION_MODAL);
        this.onChanged = onChanged;
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        build();
        Dialogs.showWithOverlay(this, root, owner);
    }

    private void build() {
        root = new RoundedPanel(Color.WHITE, 24);
        root.setLayout(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        Chrome.makeDraggable(top, this);
        top.add(Dialogs.title("Nouvelle réservation"), BorderLayout.WEST);
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

        ArrayList<Lecteur> lecteurs = DatabaseConnection.getLecteurs();
        for (Lecteur l : lecteurs) lecteurCb.addItem(l);
        ComboStyle.apply(lecteurCb);
        dateField.setText(LocalDate.now().toString());

        gbc.gridy++;
        form.add(fieldLabel("Lecteur"), gbc);
        gbc.gridy++;
        lecteurCb.setPreferredSize(new Dimension(0, 46));
        form.add(lecteurCb, gbc);

        gbc.gridy++;
        form.add(fieldLabel("Date de réservation"), gbc);
        gbc.gridy++;
        dateField.setPreferredSize(new Dimension(0, 46));
        form.add(dateField, gbc);

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
        RoundedButton cancel = new RoundedButton("Annuler", RoundedButton.Style.SECONDARY);
        cancel.addActionListener(e -> dispose());
        RoundedButton save = new RoundedButton("Enregistrer", RoundedButton.Style.PRIMARY);
        save.addActionListener(e -> doSave());
        buttons.add(cancel);
        buttons.add(save);
        form.add(buttons, gbc);

        root.add(form, BorderLayout.CENTER);
        root.setPreferredSize(new Dimension(460, 340));
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.SMALL_BOLD);
        l.setForeground(Theme.TEXT);
        return l;
    }

    private void doSave() {
        if (lecteurCb.getSelectedItem() == null) {
            errorLabel.setText("Sélectionnez un lecteur.");
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateField.getText().trim());
        } catch (Exception ex) {
            errorLabel.setText("Date invalide. Format attendu : AAAA-MM-JJ");
            return;
        }
        Lecteur lecteur = (Lecteur) lecteurCb.getSelectedItem();
        int id = DatabaseConnection.nextId("reservation", "id_reservation");
        DatabaseConnection.insertReservation(new Reservation(lecteur, date, id), lecteur);
        Toast.show(this, "Réservation enregistrée");
        if (onChanged != null) onChanged.run();
        dispose();
    }
}
