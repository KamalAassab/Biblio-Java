import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The catalogue: search, category filters, and a responsive grid of book tiles.
 */
public class CatalogueView extends JPanel {

    private final Utilisateur user;
    private final Runnable refresh;
    private final boolean isAdmin;

    private final PageHeader header = new PageHeader();
    private final SearchBar searchBar;
    private final JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    private final JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 16, 16));
    private final EmptyState empty = new EmptyState("", "").withIcon(Icons.Kind.SEARCH);
    private final JPanel gridHost = new JPanel(new BorderLayout());

    private String query = "";
    private String category;
    private String availability = "all";

    private final Map<String, Chip> filterChips = new LinkedHashMap<>();

    public CatalogueView(Utilisateur user, Runnable refresh) {
        this.user = user;
        this.refresh = refresh;
        this.isAdmin = user instanceof Admin;

        setOpaque(false);
        setLayout(new BorderLayout());

        searchBar = new SearchBar(I18n.t("cat.search"), List.of(), I18n.t("cat.filter.all"))
                .onSearch((q, cat) -> {
                    query = q;
                    category = cat;
                    rebuildGrid();
                });

        add(header, BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JComponent buildBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(0, 18, 0, 10));

        JPanel searchRow = new JPanel(new BorderLayout());
        searchRow.setOpaque(false);
        searchRow.setAlignmentX(LEFT_ALIGNMENT);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        searchRow.add(searchBar, BorderLayout.CENTER);
        body.add(searchRow);
        body.add(Box.createVerticalStrut(8));

        chipRow.setOpaque(false);
        chipRow.setAlignmentX(LEFT_ALIGNMENT);
        chipRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        body.add(chipRow);
        body.add(Box.createVerticalStrut(6));

        grid.setOpaque(false);
        gridHost.setOpaque(false);
        gridHost.add(grid, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(gridHost);
        Theme.styleScroll(scroll);
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        body.add(scroll);

        return body;
    }

    /** Applies an external search term, e.g. from a link elsewhere in the app. */
    public void setFilter(String q) {
        this.query = q == null ? "" : q;
        searchBar.setQuery(this.query);
        rebuildGrid();
    }

    public void onShow() {
        header.setTitle(I18n.t("page.catalogue.title"), I18n.t("page.catalogue.sub"));
        header.clearActions();
        if (isAdmin) {
            RoundedButton add = new RoundedButton(I18n.t("cat.add"), RoundedButton.Style.PRIMARY);
            add.withIcon(Icons.Kind.PLUS);
            add.setPreferredSize(new Dimension(190, 48));
            add.addActionListener(e ->
                    BookDialog.create(SwingUtilities.getWindowAncestor(this), refresh));
            header.addAction(add);
        }
        RoundedButton reload = new RoundedButton("", RoundedButton.Style.SECONDARY);
        reload.withIcon(Icons.Kind.REFRESH);
        reload.setPreferredSize(new Dimension(52, 48));
        reload.setToolTipText(I18n.t("action.refresh"));
        reload.addActionListener(e -> {
            DatabaseConnection.invalidateCache();
            refresh.run();
        });
        header.addAction(reload);

        searchBar.setCategories(DatabaseConnection.genres());
        rebuildChips();
        rebuildGrid();
    }

    private void rebuildChips() {
        chipRow.removeAll();
        filterChips.clear();

        addChip("all", I18n.t("cat.filter.all"));
        addChip("available", I18n.t("cat.filter.available"));
        addChip("borrowed", I18n.t("cat.filter.borrowed"));

        filterChips.forEach((key, chip) -> chip.setSelected(key.equals(availability)));
        chipRow.revalidate();
        chipRow.repaint();
    }

    private void addChip(String key, String label) {
        Chip chip = new Chip(label);
        chip.onClick(() -> {
            availability = key;
            filterChips.forEach((k, c) -> c.setSelected(k.equals(key)));
            rebuildGrid();
        });
        filterChips.put(key, chip);
        chipRow.add(chip);
    }

    private void rebuildGrid() {
        List<Livre> matches = filtered();

        grid.removeAll();
        gridHost.removeAll();

        if (matches.isEmpty()) {
            boolean filtering = !query.isBlank() || category != null || !"all".equals(availability);
            empty.setText(
                    I18n.t(filtering ? "cat.empty.title" : "cat.empty.none"),
                    I18n.t(filtering ? "cat.empty.sub" : "cat.empty.none.sub"));
            empty.withIcon(filtering ? Icons.Kind.SEARCH : Icons.Kind.BOOK);
            gridHost.add(empty, BorderLayout.CENTER);
        } else {
            for (Livre l : matches) {
                grid.add(new BookCard(l, () -> openBook(l)));
            }
            gridHost.add(grid, BorderLayout.CENTER);
        }

        // Reflect the result count on the chip so the filter reads as active.
        Chip all = filterChips.get("all");
        if (all != null) all.setText(I18n.t("cat.filter.all"));

        gridHost.revalidate();
        gridHost.repaint();
    }

    private List<Livre> filtered() {
        String q = Validate.searchTerm(query).toLowerCase(Locale.ROOT);
        List<Livre> out = new ArrayList<>();
        for (Livre l : DatabaseConnection.getLivres()) {
            if (!"all".equals(availability)) {
                boolean wantAvailable = "available".equals(availability);
                if (l.estDisponible() != wantAvailable) continue;
            }
            if (category != null && !category.equalsIgnoreCase(nullSafe(l.getGenre()))) continue;
            if (!q.isEmpty()) {
                String haystack = (nullSafe(l.getTitre()) + " " + nullSafe(l.getAuteur())
                        + " " + nullSafe(l.getGenre())).toLowerCase(Locale.ROOT);
                if (!haystack.contains(q)) continue;
            }
            out.add(l);
        }
        return out;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private void openBook(Livre livre) {
        BookDialog.view(SwingUtilities.getWindowAncestor(this), livre, isAdmin, refresh);
    }
}
