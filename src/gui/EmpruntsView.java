import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;

public class EmpruntsView extends JPanel {
    private final boolean isAdmin;
    private final Runnable onChanged;
    private final JPanel area = new JPanel(new CardLayout());
    private final JScrollPane tableScroll = new JScrollPane();
    private final RoundedPanel card;

    public EmpruntsView(Utilisateur user, Runnable onChanged) {
        this.isAdmin = user instanceof Admin;
        this.onChanged = onChanged;
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        if (isAdmin) {
            RoundedButton add = new RoundedButton("Nouvel emprunt", RoundedButton.Style.PRIMARY);
            add.addActionListener(e -> EmpruntDialog.create(this, onChanged));
            toolbar.add(add);
        }
        add(toolbar, BorderLayout.NORTH);

        area.setOpaque(false);
        card = new RoundedPanel(Color.WHITE, 18);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        card.add(tableScroll, BorderLayout.CENTER);
        area.add(card, "table");
        EmptyState es = new EmptyState("Aucun emprunt", "Les emprunts apparaîtront ici.");
        area.add(es, "empty");
        add(area, BorderLayout.CENTER);
    }

    public void onShow() {
        ArrayList<Emprunt> emprunts = DatabaseConnection.getEmprunts();
        String[] cols = {"ID", "Livre", "Utilisateur", "Date d'emprunt", "Retour prévu", "Statut"};
        Object[][] rows = new Object[emprunts.size()][6];
        LocalDate today = LocalDate.now();
        for (int i = 0; i < emprunts.size(); i++) {
            Emprunt e = emprunts.get(i);
            boolean late = e.getDateRetour().isBefore(today);
            rows[i] = new Object[]{
                e.getId(),
                e.getLivre().getTitre(),
                e.getUtilisateur().getNom(),
                e.getDateEmprunts().toString(),
                e.getDateRetour().toString(),
                late ? new TableStyle.ChipText("En retard", Theme.DANGER, new Color(0xFE, 0xE2, 0xE2))
                     : new TableStyle.ChipText("En cours", new Color(0x0D, 0x94, 0x88), new Color(0xCC, 0xFB, 0xF1))
            };
        }
        JTable table = TableStyle.create(cols, rows, new Color[]{Theme.MUTED, Theme.TEXT, Theme.TEXT, Theme.MUTED, Theme.MUTED, null});
        table.getColumnModel().getColumn(5).setCellRenderer(new TableStyle.ChipCellRenderer());
        tableScroll.setViewportView(table);
        Theme.styleScroll(tableScroll);
        CardLayout cl = (CardLayout) area.getLayout();
        cl.show(area, emprunts.isEmpty() ? "empty" : "table");
    }
}
