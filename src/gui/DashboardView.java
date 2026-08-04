import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;

public class DashboardView extends JPanel {
    private final Utilisateur user;
    private final boolean isAdmin;
    private final Runnable goCatalogue;
    private final Runnable refreshAll;
    private final JLabel greeting = new JLabel();
    private final StatCard cTotal = new StatCard("Livres au catalogue", Theme.PRIMARY, Icons.Kind.BOOK);
    private final StatCard cDispo = new StatCard("Livres disponibles", Theme.SUCCESS, Icons.Kind.CHECK);
    private final StatCard cEmprunts = new StatCard("Emprunts en cours", Theme.AMBER, Icons.Kind.CLOCK);
    private final StatCard cResa = new StatCard("Réservations", Theme.ACCENT, Icons.Kind.BELL);
    private final JPanel recentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
    private final JPanel quickRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));

    public DashboardView(Utilisateur user, Runnable goCatalogue, Runnable refreshAll) {
        this.user = user;
        this.isAdmin = user instanceof Admin;
        this.goCatalogue = goCatalogue;
        this.refreshAll = refreshAll;
        setOpaque(false);
        setLayout(new BorderLayout());

        JScrollPane sp = new JScrollPane(build());
        sp.getViewport().setOpaque(false);
        sp.setOpaque(false);
        sp.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp, BorderLayout.CENTER);
    }

    private JPanel build() {
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 30, 6));

        greeting.setFont(Theme.H1);
        greeting.setForeground(Theme.TEXT);
        greeting.setAlignmentX(LEFT_ALIGNMENT);
        box.add(greeting);

        JLabel date = new JLabel("Aujourd'hui, " + LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy")));
        date.setFont(Theme.SMALL);
        date.setForeground(Theme.MUTED);
        date.setAlignmentX(LEFT_ALIGNMENT);
        box.add(date);
        box.add(Box.createVerticalStrut(22));

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(LEFT_ALIGNMENT);
        stats.add(cTotal);
        stats.add(cDispo);
        stats.add(cEmprunts);
        stats.add(cResa);
        box.add(stats);

        box.add(Box.createVerticalStrut(28));
        box.add(sectionLabel("Livres récents"));
        box.add(Box.createVerticalStrut(4));
        recentRow.setOpaque(false);
        recentRow.setAlignmentX(LEFT_ALIGNMENT);
        box.add(recentRow);

        box.add(Box.createVerticalStrut(26));
        box.add(sectionLabel("Actions rapides"));
        box.add(Box.createVerticalStrut(8));
        quickRow.setOpaque(false);
        quickRow.setAlignmentX(LEFT_ALIGNMENT);
        box.add(quickRow);

        return box;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.H2);
        l.setForeground(Theme.TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    public void onShow() {
        greeting.setText(isAdmin ? "Bonjour, " + user.getNom() : "Bienvenue, " + user.getNom());

        ArrayList<Livre> livres = DatabaseConnection.getLivres();
        int dispo = 0;
        for (Livre l : livres) if (l.estDisponible()) dispo++;
        cTotal.setValue(livres.size());
        cDispo.setValue(dispo);
        cEmprunts.setValue(DatabaseConnection.getEmprunts().size());
        cResa.setValue(DatabaseConnection.getReservations().size());

        recentRow.removeAll();
        ArrayList<Livre> recent = new ArrayList<>(livres);
        recent.sort(Comparator.comparingInt(Livre::getId).reversed());
        int n = 0;
        for (Livre l : recent) {
            if (n++ >= 4) break;
            Livre copy = l;
            recentRow.add(new BookCard(l, isAdmin, () -> {
                if (isAdmin) BookDialog.edit(this, copy, refreshAll);
                else BookDialog.view(this, copy);
            }));
        }
        if (recent.isEmpty()) {
            EmptyState es = new EmptyState("Aucun livre", "Ajoutez votre premier livre au catalogue.");
            recentRow.add(es);
        }
        recentRow.revalidate();
        recentRow.repaint();

        quickRow.removeAll();
        if (isAdmin) {
            quickRow.add(new ActionCard(Theme.PRIMARY, Theme.PRIMARY_2, Icons.Kind.PLUS, "Ajouter un livre",
                    "Enrichir le catalogue", () -> BookDialog.add(this, refreshAll)));
            quickRow.add(new ActionCard(new Color(217, 119, 6), new Color(245, 158, 11), Icons.Kind.CLOCK, "Nouvel emprunt",
                    "Enregistrer un emprunt", () -> EmpruntDialog.create(this, refreshAll)));
        } else {
            quickRow.add(new ActionCard(Theme.PRIMARY, Theme.PRIMARY_2, Icons.Kind.BOOK, "Explorer le catalogue",
                    "Parcourir les livres", () -> goCatalogue.run()));
            quickRow.add(new ActionCard(new Color(16, 185, 129), new Color(52, 211, 153), Icons.Kind.CALENDAR, "Réserver un livre",
                    "Effectuer une réservation", () -> ReservationDialog.create(this, refreshAll)));
        }
        quickRow.revalidate();
        quickRow.repaint();
    }
}
