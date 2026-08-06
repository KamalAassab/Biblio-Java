import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Records a reservation on behalf of a reader. */
public class ReservationDialog extends JDialog {

    private final Runnable onDone;

    private final JComboBox<Lecteur> readers = new JComboBox<>();
    private final RoundedTextField date = new RoundedTextField("AAAA-MM-JJ");

    public static void create(Window owner, Runnable onDone) {
        new ReservationDialog(owner, onDone).setVisible(true);
    }

    private ReservationDialog(Window owner, Runnable onDone) {
        super(owner, "", ModalityType.APPLICATION_MODAL);
        this.onDone = onDone;

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        Card card = new Card(Theme.RADIUS_XL);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(
                Card.shadowInset() + 28, Card.shadowInset() + 30,
                Card.shadowInset() + 24, Card.shadowInset() + 30));

        card.add(buildHeader(), BorderLayout.NORTH);
        card.add(buildForm(), BorderLayout.CENTER);
        card.add(buildButtons(), BorderLayout.SOUTH);

        setContentPane(card);
        pack();
        setSize(560, 400);
        setLocationRelativeTo(owner);
        getRootPane().registerKeyboardAction(e -> dispose(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        populate();
    }

    private JComponent buildHeader() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel(I18n.t("res.new"));
        title.setFont(Theme.H1);
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sub = new JLabel(I18n.t("res.new.sub"));
        sub.setFont(Theme.BODY);
        sub.setForeground(Theme.MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);

        p.add(title);
        p.add(Box.createVerticalStrut(6));
        p.add(sub);
        return p;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        ComboStyle.apply(readers);

        form.add(field("res.reader", readers));
        form.add(Box.createVerticalStrut(14));
        form.add(field("res.date", date));
        form.add(Box.createVerticalGlue());
        return form;
    }

    private JComponent field(String labelKey, JComponent input) {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setAlignmentX(LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));

        JLabel label = new JLabel(I18n.t(labelKey));
        label.setFont(Theme.SMALL_BOLD);
        label.setForeground(Theme.TEXT_SOFT);
        label.setAlignmentX(LEFT_ALIGNMENT);

        input.setAlignmentX(LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        input.setPreferredSize(new Dimension(0, 52));

        wrap.add(label);
        wrap.add(Box.createVerticalStrut(6));
        wrap.add(input);
        return wrap;
    }

    private JComponent buildButtons() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(22, 0, 0, 0));

        RoundedButton cancel = new RoundedButton(I18n.t("action.cancel"),
                RoundedButton.Style.SECONDARY);
        cancel.setPreferredSize(new Dimension(130, 46));
        cancel.addActionListener(e -> dispose());
        row.add(cancel);

        RoundedButton save = new RoundedButton(I18n.t("action.save"), RoundedButton.Style.PRIMARY);
        save.setPreferredSize(new Dimension(150, 46));
        save.addActionListener(e -> save());
        row.add(save);
        getRootPane().setDefaultButton(save);
        return row;
    }

    private void populate() {
        List<Lecteur> lecteurs = DatabaseConnection.getLecteurs();
        readers.setModel(new DefaultComboBoxModel<>(lecteurs.toArray(new Lecteur[0])));
        date.setText(LocalDate.now().toString());
    }

    private void save() {
        Lecteur reader = (Lecteur) readers.getSelectedItem();
        if (reader == null) {
            Toast.error(this, I18n.t("error.reader.required"));
            return;
        }

        LocalDate when;
        try {
            when = LocalDate.parse(date.getText().trim());
        } catch (DateTimeParseException e) {
            date.markError();
            Toast.error(this, I18n.t("error.date.invalid"));
            return;
        }

        Reservation reservation = new Reservation(reader, when, 0);
        if (DatabaseConnection.insertReservation(reservation, reader) < 0) {
            Toast.error(this, I18n.t("toast.error"));
            return;
        }

        Toast.success(getOwner(), I18n.t("toast.res.created"));
        dispose();
        if (onDone != null) onDone.run();
    }
}
