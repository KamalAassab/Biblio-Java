import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/** Reservation requests, with cancellation available to administrators. */
public class ReservationsView extends JPanel {

    private final Utilisateur user;
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
    private final EmptyState empty = new EmptyState("", "").withIcon(Icons.Kind.BOOKMARK);
    private final JScrollPane scroll = new JScrollPane(table);

    private List<Reservation> rows = new ArrayList<>();

    public ReservationsView(Utilisateur user, Runnable refresh) {
        this.user = user;
        this.refresh = refresh;
        this.isAdmin = user instanceof Admin;

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

        table.setFocusable(true);
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) cancelSelected();
            }
        });

        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(
                Card.shadowInset() + 6, Card.shadowInset() + 6,
                Card.shadowInset() + 6, Card.shadowInset() + 6));
        card.add(scroll, BorderLayout.CENTER);

        body.add(card, BorderLayout.CENTER);
        return body;
    }

    private String[] columnNames() {
        return new String[]{"#", I18n.t("res.reader"), I18n.t("res.date")};
    }

    public void onShow() {
        header.setTitle(I18n.t("page.reservations.title"), I18n.t("page.reservations.sub"));
        header.clearActions();

        RoundedButton add = new RoundedButton(I18n.t("res.new"), RoundedButton.Style.PRIMARY);
        add.withIcon(Icons.Kind.PLUS);
        add.setPreferredSize(new Dimension(210, 48));
        add.addActionListener(e ->
                ReservationDialog.create(SwingUtilities.getWindowAncestor(this), refresh));
        header.addAction(add);

        if (isAdmin) {
            RoundedButton cancel = new RoundedButton(I18n.t("action.delete"),
                    RoundedButton.Style.SECONDARY);
            cancel.withIcon(Icons.Kind.TRASH);
            cancel.setPreferredSize(new Dimension(148, 48));
            cancel.addActionListener(e -> cancelSelected());
            header.addAction(cancel);
        }

        model.setColumnIdentifiers(columnNames());
        reload();
    }

    private void reload() {
        rows = DatabaseConnection.getReservations();
        model.setRowCount(0);
        for (Reservation r : rows) {
            model.addRow(new Object[]{
                    String.valueOf(r.getId_reservation()),
                    new TableStyle.StrongText(r.getUtilisateur().getNom()),
                    I18n.date(r.getDateReservation())
            });
        }

        card.removeAll();
        if (rows.isEmpty()) {
            empty.setText(I18n.t("res.empty.title"), I18n.t("res.empty.sub"));
            card.add(empty, BorderLayout.CENTER);
        } else {
            card.add(scroll, BorderLayout.CENTER);
        }
        card.revalidate();
        card.repaint();
    }

    private void cancelSelected() {
        if (!isAdmin) {
            Toast.show(this, I18n.t("error.notAllowed"));
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0 || row >= rows.size()) {
            Toast.show(this, I18n.t("error.selectRow"));
            return;
        }
        Reservation r = rows.get(row);
        boolean ok = Dialogs.confirm(SwingUtilities.getWindowAncestor(this),
                I18n.t("confirm.delete.res.title"), I18n.t("confirm.delete.res.body"),
                I18n.t("action.delete"));
        if (!ok) return;

        if (DatabaseConnection.deleteReservation(r.getId_reservation())) {
            Toast.show(this, I18n.t("toast.res.deleted"));
            refresh.run();
        } else {
            Toast.show(this, I18n.t("toast.error"));
        }
    }
}
