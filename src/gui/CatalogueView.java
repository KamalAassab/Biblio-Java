import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CatalogueView extends JPanel {
    private final boolean isAdmin;
    private final Runnable onChanged;
    private final SearchField search = new SearchField();
    private final JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    private final JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 20, 20));
    private final JPanel gridArea = new JPanel(new CardLayout());
    private final List<Chip> chips = new ArrayList<>();
    private ArrayList<Livre> livres = new ArrayList<>();
    private String selectedGenre = null;

    public CatalogueView(Utilisateur user, Runnable onChanged) {
        this.isAdmin = user instanceof Admin;
        this.onChanged = onChanged;
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel toolbar = new JPanel();
        toolbar.setOpaque(false);
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JPanel row1 = new JPanel(new BorderLayout());
        row1.setOpaque(false);
        if (isAdmin) {
            RoundedButton add = new RoundedButton("Ajouter un livre", RoundedButton.Style.PRIMARY);
            add.addActionListener(e -> BookDialog.add(this, onChanged));
            row1.add(add, BorderLayout.EAST);
        }
        toolbar.add(row1);
        toolbar.add(Box.createVerticalStrut(10));
        toolbar.add(chipRow);

        add(toolbar, BorderLayout.NORTH);

        grid.setOpaque(false);
        JScrollPane sp = new JScrollPane(grid);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        Theme.styleScroll(sp);

        EmptyState empty = new EmptyState("Aucun livre trouvé", "Essayez de modifier votre recherche ou vos filtres.");
        empty.setOpaque(false);
        gridArea.setOpaque(false);
        gridArea.add(sp, "grid");
        gridArea.add(empty, "empty");
        add(gridArea, BorderLayout.CENTER);
    }

    void setFilter(String q) {
        search.setText(q);
        apply();
    }

    public void onShow() {
        livres = DatabaseConnection.getLivres();
        rebuildChips();
        apply();
    }

    private void rebuildChips() {
        chipRow.removeAll();
        chips.clear();
        Set<String> genres = new LinkedHashSet<>();
        for (Livre l : livres) {
            if (l.getGenre() != null && !l.getGenre().isEmpty()) genres.add(l.getGenre());
        }
        Chip tous = new Chip("Tous");
        tous.setOn(true);
        Chip finalTous = tous;
        tous.addActionListener(e -> selectChip(finalTous));
        chips.add(tous);
        chipRow.add(tous);
        for (String genre : genres) {
            Chip c = new Chip(genre);
            c.addActionListener(e -> selectChip(c));
            chips.add(c);
            chipRow.add(c);
        }
        chipRow.revalidate();
        chipRow.repaint();
    }

    private void selectChip(Chip clicked) {
        if (clicked.isOn()) {
            for (Chip c : chips) c.setOn(c == clicked);
            selectedGenre = clicked.getText().equals("Tous") ? null : clicked.getText();
        } else {
            for (Chip c : chips) c.setOn(c.getText().equals("Tous"));
            selectedGenre = null;
        }
        apply();
    }

    private void apply() {
        String q = search.getText().trim().toLowerCase();
        grid.removeAll();
        for (Livre l : livres) {
            if (selectedGenre != null && !selectedGenre.equals(l.getGenre())) continue;
            if (!q.isEmpty()) {
                boolean match = (l.getTitre() != null && l.getTitre().toLowerCase().contains(q))
                        || (l.getAuteur() != null && l.getAuteur().toLowerCase().contains(q))
                        || (l.getGenre() != null && l.getGenre().toLowerCase().contains(q));
                if (!match) continue;
            }
            Livre copy = l;
            grid.add(new BookCard(l, isAdmin, () -> {
                if (isAdmin) BookDialog.edit(this, copy, onChanged);
                else BookDialog.view(this, copy);
            }));
        }
        CardLayout cl = (CardLayout) gridArea.getLayout();
        cl.show(gridArea, grid.getComponentCount() == 0 ? "empty" : "grid");
        grid.revalidate();
        grid.repaint();
    }
}
