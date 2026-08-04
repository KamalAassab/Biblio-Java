import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.util.ArrayList;

public class UtilisateursView extends JPanel {
    private final Runnable onChanged;
    private final JPanel area = new JPanel(new CardLayout());
    private final JScrollPane tableScroll = new JScrollPane();
    private final RoundedPanel card;

    public UtilisateursView() {
        this(null);
    }

    public UtilisateursView(Runnable onChanged) {
        this.onChanged = onChanged;
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        area.setOpaque(false);
        card = new RoundedPanel(Color.WHITE, 18);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        card.add(tableScroll, BorderLayout.CENTER);
        area.add(card, "table");
        EmptyState es = new EmptyState("Aucun utilisateur", "Les utilisateurs apparaîtront ici.");
        area.add(es, "empty");
        add(area, BorderLayout.CENTER);
    }

    public void onShow() {
        ArrayList<Utilisateur> users = DatabaseConnection.getUtilisateurs();
        String[] cols = {"ID", "Nom", "Email", "Téléphone", "Rôle"};
        Object[][] rows = new Object[users.size()][5];
        for (int i = 0; i < users.size(); i++) {
            Utilisateur u = users.get(i);
            boolean isAdmin = u instanceof Admin;
            rows[i] = new Object[]{
                u.getId(),
                u.getNom(),
                u.getEmail(),
                u.getNumero(),
                isAdmin
                    ? new TableStyle.ChipText("Admin", new Color(0x00, 0x40, 0x80), new Color(0xDB, 0xEA, 0xFE))
                    : new TableStyle.ChipText("Lecteur", new Color(0x0D, 0x94, 0x88), new Color(0xCC, 0xFB, 0xF1))
            };
        }
        JTable table = TableStyle.create(cols, rows, new Color[]{Theme.MUTED, Theme.TEXT, Theme.MUTED, Theme.MUTED, null});
        table.getColumnModel().getColumn(4).setCellRenderer(new TableStyle.ChipCellRenderer());
        tableScroll.setViewportView(table);
        Theme.styleScroll(tableScroll);
        CardLayout cl = (CardLayout) area.getLayout();
        cl.show(area, users.isEmpty() ? "empty" : "table");
    }
}
