import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.ArrayList;

public class ReservationsView extends JPanel {
    private final Runnable onChanged;
    private final JPanel area = new JPanel(new CardLayout());
    private final JScrollPane tableScroll = new JScrollPane();
    private final RoundedPanel card;

    public ReservationsView(Utilisateur user, Runnable onChanged) {
        this.onChanged = onChanged;
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        RoundedButton add = new RoundedButton("Nouvelle réservation", RoundedButton.Style.PRIMARY);
        add.addActionListener(e -> ReservationDialog.create(this, onChanged));
        toolbar.add(add);
        add(toolbar, BorderLayout.NORTH);

        area.setOpaque(false);
        card = new RoundedPanel(Color.WHITE, 18);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        card.add(tableScroll, BorderLayout.CENTER);
        area.add(card, "table");
        EmptyState es = new EmptyState("Aucune réservation", "Les réservations apparaîtront ici.");
        area.add(es, "empty");
        add(area, BorderLayout.CENTER);
    }

    public void onShow() {
        ArrayList<Reservation> reservations = DatabaseConnection.getReservations();
        String[] cols = {"ID", "Lecteur", "Date de réservation"};
        Object[][] rows = new Object[reservations.size()][3];
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            rows[i] = new Object[]{
                r.getId_reservation(),
                r.getUtilisateur().getNom(),
                r.getDateReservation().toString()
            };
        }
        JTable table = TableStyle.create(cols, rows, new Color[]{Theme.MUTED, Theme.TEXT, Theme.MUTED});
        tableScroll.setViewportView(table);
        Theme.styleScroll(tableScroll);
        CardLayout cl = (CardLayout) area.getLayout();
        cl.show(area, reservations.isEmpty() ? "empty" : "table");
    }
}
