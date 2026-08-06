import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The landing page: a greeting, the headline counters, a shelf of recent additions,
 * and the shortcuts that skip a level of navigation.
 */
public class DashboardView extends JPanel {

    private final Utilisateur user;
    private final Consumer<String> navigate;
    private final Runnable refresh;

    private final PageHeader header = new PageHeader();
    private final StatCard cBooks = new StatCard("dash.stat.books", Theme.PRIMARY, Icons.Kind.BOOK);
    private final StatCard cAvailable = new StatCard("dash.stat.available", Theme.SUCCESS, Icons.Kind.CHECK);
    private final StatCard cLoans = new StatCard("dash.stat.loans", Theme.AMBER, Icons.Kind.CLOCK);
    private final StatCard cReservations = new StatCard("dash.stat.reservations", Theme.ACCENT, Icons.Kind.BOOKMARK);

    /** Height the shelf collapses to when the catalogue has nothing in it. */
    private static final int EMPTY_SHELF_HEIGHT = 236;

    private final JPanel shelf = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
    private JScrollPane shelfScroll;
    private final JPanel actions = new JPanel(new GridLayout(1, 0, 14, 0));
    private final JLabel shelfTitle = new JLabel();
    private final JLabel actionsTitle = new JLabel();

    public DashboardView(Utilisateur user, Consumer<String> navigate, Runnable refresh) {
        this.user = user;
        this.navigate = navigate;
        this.refresh = refresh;

        setOpaque(false);
        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JComponent buildBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(0, 10, 20, 10));

        JPanel stats = new JPanel(new GridLayout(1, 4, 4, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(LEFT_ALIGNMENT);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 156));
        stats.add(cBooks);
        stats.add(cAvailable);
        stats.add(cLoans);
        stats.add(cReservations);
        body.add(stats);
        body.add(Box.createVerticalStrut(12));

        shelfTitle.setFont(Theme.SECTION);
        shelfTitle.setForeground(Theme.TEXT);
        body.add(sectionRow(shelfTitle, I18n.t("dash.viewAll"), () -> navigate.accept("catalogue")));
        body.add(Box.createVerticalStrut(4));

        shelf.setOpaque(false);
        shelfScroll = new JScrollPane(shelf);
        Theme.styleScrollHorizontal(shelfScroll);
        shelfScroll.setAlignmentX(LEFT_ALIGNMENT);
        shelfScroll.setPreferredSize(new Dimension(0, BookCard.HEIGHT + 26));
        shelfScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, BookCard.HEIGHT + 26));
        body.add(shelfScroll);
        body.add(Box.createVerticalStrut(18));

        actionsTitle.setFont(Theme.SECTION);
        actionsTitle.setForeground(Theme.TEXT);
        body.add(sectionRow(actionsTitle, null, null));
        body.add(Box.createVerticalStrut(4));

        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        body.add(actions);
        body.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(body);
        Theme.styleScroll(scroll);
        return scroll;
    }

    /** A section title with an optional "view all" affordance on the right. */
    private JComponent sectionRow(JLabel title, String linkText, Runnable onLink) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        row.setBorder(new EmptyBorder(10, 10, 6, 10));
        row.add(title, BorderLayout.WEST);

        if (linkText != null && onLink != null) {
            RoundedButton link = new RoundedButton(linkText, RoundedButton.Style.SECONDARY);
            link.withIcon(Icons.Kind.CHEVRON_RIGHT);
            link.setFont(Theme.SMALL_BOLD);
            link.setPreferredSize(new Dimension(124, 40));
            link.addActionListener(e -> onLink.run());
            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            wrap.setOpaque(false);
            wrap.add(link);
            row.add(wrap, BorderLayout.EAST);
        }
        return row;
    }

    /** Reloads counters and the shelf. Called whenever the page is shown or data changes. */
    public void onShow() {
        header.setTitle(greeting(), I18n.t("page.dashboard.sub"));
        shelfTitle.setText(I18n.t("dash.recent.title"));
        actionsTitle.setText(I18n.t("dash.actions.title"));

        DatabaseConnection.Stats stats = DatabaseConnection.stats();
        cBooks.setValue(stats.books());
        cAvailable.setValue(stats.available());
        cLoans.setValue(stats.activeLoans());
        cReservations.setValue(stats.reservations());

        cAvailable.setCaption(I18n.t("dash.availability", stats.available(), stats.books()));
        cLoans.setCaption(stats.overdue() > 0
                ? I18n.t("dash.stat.overdue") + " : " + stats.overdue()
                : null);

        rebuildShelf();
        rebuildActions();
    }

    private void rebuildShelf() {
        shelf.removeAll();
        List<Livre> livres = DatabaseConnection.getLivres();

        // Newest first — ids are sequence-assigned, so descending id is insertion order.
        List<Livre> recent = new ArrayList<>(livres);
        recent.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        if (recent.size() > 12) recent = recent.subList(0, 12);

        boolean isEmpty = recent.isEmpty();
        if (isEmpty) {
            EmptyState empty = new EmptyState(
                    I18n.t("cat.empty.none"), I18n.t("cat.empty.none.sub"))
                    .withIcon(Icons.Kind.BOOK);
            empty.setPreferredSize(new Dimension(620, EMPTY_SHELF_HEIGHT));
            shelf.add(empty);
        } else {
            for (Livre l : recent) {
                shelf.add(new BookCard(l, () -> openBook(l)));
            }
        }

        // Collapse the shelf when there is nothing to show, so the sections below it
        // stay on screen instead of being pushed past the fold by a placeholder.
        int height = isEmpty ? EMPTY_SHELF_HEIGHT : BookCard.HEIGHT + 10;
        shelf.setPreferredSize(new Dimension(
                Math.max(1, recent.size()) * (BookCard.WIDTH + 16) + 24, height));
        shelfScroll.setPreferredSize(new Dimension(0, height + 16));
        shelfScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, height + 16));

        shelf.revalidate();
        shelf.repaint();
        shelfScroll.revalidate();
    }

    private void rebuildActions() {
        actions.removeAll();
        boolean isAdmin = user instanceof Admin;

        actions.add(new ActionCard(Icons.Kind.SEARCH, Theme.PRIMARY,
                I18n.t("dash.action.browse"), I18n.t("dash.action.browse.sub"),
                () -> navigate.accept("catalogue")));

        if (isAdmin) {
            actions.add(new ActionCard(Icons.Kind.PLUS, Theme.SUCCESS,
                    I18n.t("dash.action.addBook"), I18n.t("dash.action.addBook.sub"),
                    this::addBook));
        }
        actions.add(new ActionCard(Icons.Kind.CLOCK, Theme.AMBER,
                I18n.t("dash.action.newLoan"), I18n.t("dash.action.newLoan.sub"),
                this::newLoan));
        actions.add(new ActionCard(Icons.Kind.BOOKMARK, Theme.ACCENT,
                I18n.t("dash.action.newReservation"), I18n.t("dash.action.newReservation.sub"),
                this::newReservation));

        actions.revalidate();
        actions.repaint();
    }

    private void openBook(Livre livre) {
        BookDialog.view(SwingUtilities.getWindowAncestor(this), livre, user instanceof Admin, refresh);
    }

    private void addBook() {
        BookDialog.create(SwingUtilities.getWindowAncestor(this), refresh);
    }

    private void newLoan() {
        EmpruntDialog.create(SwingUtilities.getWindowAncestor(this), refresh);
    }

    private void newReservation() {
        ReservationDialog.create(SwingUtilities.getWindowAncestor(this), refresh);
    }

    private String greeting() {
        int hour = LocalTime.now().getHour();
        String key = hour < 12 ? "dash.greeting.morning"
                : hour < 18 ? "dash.greeting.afternoon"
                : "dash.greeting.evening";
        return I18n.t(key, user.getNom());
    }
}
