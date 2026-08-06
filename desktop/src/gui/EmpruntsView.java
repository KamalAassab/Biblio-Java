import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Loan tracking: who has what, when it is due, and what is overdue.
 *
 * <p>Double-clicking an open loan records its return, which is the action taken far
 * more often than any other on this screen.
 */
public class EmpruntsView extends JPanel {

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
    private final EmptyState empty = new EmptyState("", "").withIcon(Icons.Kind.CLOCK);
    private final JScrollPane scroll = new JScrollPane(table);

    private List<Emprunt> rows = new ArrayList<>();

    public EmpruntsView(Utilisateur user, Runnable refresh) {
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

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) recordReturn();
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
        return new String[]{
                I18n.t("loan.reader"), I18n.t("loan.book"),
                I18n.t("loan.borrowedOn"), I18n.t("loan.dueOn"), I18n.t("book.status")
        };
    }

    public void onShow() {
        header.setTitle(I18n.t("page.emprunts.title"), I18n.t("page.emprunts.sub"));
        header.clearActions();

        RoundedButton add = new RoundedButton(I18n.t("loan.new"), RoundedButton.Style.PRIMARY);
        add.withIcon(Icons.Kind.PLUS);
        add.setPreferredSize(new Dimension(190, 48));
        add.addActionListener(e ->
                EmpruntDialog.create(SwingUtilities.getWindowAncestor(this), refresh));
        header.addAction(add);

        model.setColumnIdentifiers(columnNames());
        reload();
    }

    private void reload() {
        rows = DatabaseConnection.getEmprunts();
        model.setRowCount(0);

        LocalDate today = LocalDate.now();
        for (Emprunt e : rows) {
            model.addRow(new Object[]{
                    new TableStyle.StrongText(e.getUtilisateur().getNom()),
                    e.getLivre().getTitre(),
                    I18n.date(e.getDateEmprunts()),
                    I18n.date(e.getDateRetour()),
                    statusOf(e, today)
            });
        }

        card.removeAll();
        if (rows.isEmpty()) {
            empty.setText(I18n.t("loan.empty.title"), I18n.t("loan.empty.sub"));
            card.add(empty, BorderLayout.CENTER);
        } else {
            card.add(scroll, BorderLayout.CENTER);
        }
        card.revalidate();
        card.repaint();
    }

    private TableStyle.ChipText statusOf(Emprunt e, LocalDate today) {
        if (e.getDateRetourLivre() != null) {
            return TableStyle.neutral(I18n.t("loan.status.returned"));
        }
        LocalDate due = e.getDateRetour();
        if (due == null) return TableStyle.info(I18n.t("loan.status.active"));

        long days = ChronoUnit.DAYS.between(today, due);
        if (days < 0) return TableStyle.danger(I18n.t("loan.overdueBy", -days));
        if (days == 0) return TableStyle.warning(I18n.t("loan.dueToday"));
        if (days <= 3) return TableStyle.warning(I18n.t("loan.dueIn", days));
        return TableStyle.success(I18n.t("loan.dueIn", days));
    }

    private void recordReturn() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= rows.size()) return;

        Emprunt e = rows.get(row);
        if (e.getDateRetourLivre() != null) return; // already returned
        if (!isAdmin) {
            Toast.show(this, I18n.t("error.notAllowed"));
            return;
        }

        boolean ok = Dialogs.confirm(SwingUtilities.getWindowAncestor(this),
                I18n.t("confirm.return.title"),
                I18n.t("confirm.return.body", e.getLivre().getTitre()),
                I18n.t("action.confirm"));
        if (!ok) return;

        if (DatabaseConnection.returnEmprunt(e.getId())) {
            Toast.show(this, I18n.t("toast.loan.returned"));
            refresh.run();
        } else {
            Toast.show(this, I18n.t("toast.error"));
        }
    }
}
