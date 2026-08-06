import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** The member directory. Administrators can remove accounts; readers see it read-only. */
public class UtilisateursView extends JPanel {

    private final Utilisateur currentUser;
    private final Runnable refresh;
    private final boolean isAdmin;

    private final PageHeader header = new PageHeader();
    private final DefaultTableModel model = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final Card card = new Card(Theme.RADIUS_LG);
    private final EmptyState empty = new EmptyState("", "").withIcon(Icons.Kind.USERS);
    private final JScrollPane scroll = new JScrollPane(table);

    private List<Utilisateur> rows = new ArrayList<>();

    public UtilisateursView(Utilisateur currentUser, Runnable refresh) {
        this.currentUser = currentUser;
        this.refresh = refresh;
        this.isAdmin = currentUser instanceof Admin;

        setOpaque(false);
        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 18, 8, 10));

        model.setColumnIdentifiers(columnNames());
        TableStyle.apply(table);
        Theme.styleScroll(scroll);

        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(
                Card.shadowInset() + 6, Card.shadowInset() + 6,
                Card.shadowInset() + 6, Card.shadowInset() + 6));
        card.add(scroll, BorderLayout.CENTER);

        body.add(card, BorderLayout.CENTER);
        return body;
    }

    private String[] columnNames() {
        return new String[]{
                I18n.t("user.name"), I18n.t("user.email"),
                I18n.t("user.phone"), I18n.t("user.role")
        };
    }

    public void onShow() {
        header.setTitle(I18n.t("page.utilisateurs.title"), I18n.t("page.utilisateurs.sub"));
        header.clearActions();

        if (isAdmin) {
            RoundedButton remove = new RoundedButton(I18n.t("action.delete"),
                    RoundedButton.Style.SECONDARY);
            remove.withIcon(Icons.Kind.TRASH);
            remove.setPreferredSize(new Dimension(148, 48));
            remove.addActionListener(e -> deleteSelected());
            header.addAction(remove);
        }

        model.setColumnIdentifiers(columnNames());
        reload();
    }

    private void reload() {
        rows = DatabaseConnection.getUtilisateurs();
        model.setRowCount(0);
        for (Utilisateur u : rows) {
            boolean admin = u instanceof Admin;
            model.addRow(new Object[]{
                    new TableStyle.StrongText(u.getNom()),
                    u.getEmail() == null || u.getEmail().isBlank() ? "—" : u.getEmail(),
                    u.getNumero() > 0 ? formatPhone(u.getNumero()) : "—",
                    admin ? TableStyle.info(I18n.t("user.role.admin"))
                          : TableStyle.neutral(I18n.t("user.role.reader"))
            });
        }

        card.removeAll();
        if (rows.isEmpty()) {
            empty.setText(I18n.t("user.empty.title"), I18n.t("user.empty.sub"));
            card.add(empty, BorderLayout.CENTER);
        } else {
            card.add(scroll, BorderLayout.CENTER);
        }
        card.revalidate();
        card.repaint();
    }

    /** Groups a 9-digit local number as 0X XX XX XX XX. */
    private String formatPhone(int numero) {
        String digits = String.format("%09d", numero);
        return "0" + digits.charAt(0) + " " + digits.substring(1, 3) + " "
                + digits.substring(3, 5) + " " + digits.substring(5, 7) + " "
                + digits.substring(7, 9);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= rows.size()) {
            Toast.show(this, I18n.t("error.selectRow"));
            return;
        }
        Utilisateur target = rows.get(row);

        // Removing your own account would log you out of a session that is still open.
        if (target.getId() == currentUser.getId()) {
            Toast.show(this, I18n.t("error.cannotDeleteSelf"));
            return;
        }

        boolean ok = Dialogs.confirm(SwingUtilities.getWindowAncestor(this),
                I18n.t("confirm.delete.user.title"),
                I18n.t("confirm.delete.user.body", target.getNom()),
                I18n.t("action.delete"));
        if (!ok) return;

        if (DatabaseConnection.deleteUtilisateur(target.getId())) {
            Toast.show(this, I18n.t("toast.user.deleted"));
            refresh.run();
        } else {
            Toast.show(this, I18n.t("toast.error"));
        }
    }
}
