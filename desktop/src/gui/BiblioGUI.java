import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The application shell: a white sidebar floating on a warm ivory canvas, with the
 * active page rendered in the column to its right.
 *
 * <p>Views own their own headers rather than the shell imposing one, which lets each
 * page choose whether it needs a search bar, a filter row, or nothing at all.
 */
public class BiblioGUI extends JFrame {

    private Utilisateur user;
    private final boolean isAdmin;

    private final Map<String, NavPill> navs = new LinkedHashMap<>();
    private final JPanel content = new JPanel(new CardLayout());
    private String currentPage = "dashboard";

    private DashboardView dashboard;
    private CatalogueView catalogue;
    private EmpruntsView emprunts;
    private ReservationsView reservations;
    private UtilisateursView utilisateurs;
    private ProfileView profile;
    private UserChip userChip;
    private BellButton bell;

    public static void launch() {
        SwingUtilities.invokeLater(() -> new LoginScreen().setVisible(true));
    }

    public static void main(String[] args) {
        launch();
    }

    public BiblioGUI(Utilisateur user) {
        this.user = user;
        this.isAdmin = user instanceof Admin;

        Chrome.prepare(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle(I18n.t("app.name") + " — " + I18n.t("app.tagline"));
        setSize(1380, 880);
        setMinimumSize(new Dimension(1120, 720));

        build();
        setLocationRelativeTo(null);
        showPage("dashboard");

        // Relabel the whole shell in place when the language is switched.
        I18n.onChange(this::applyLanguage);
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.aa(g);
                Theme.fillRound(g, 0, 0, getWidth(), getHeight(), 20, Theme.CANVAS);
                g.dispose();
            }
        };
        root.setOpaque(false);
        root.add(Chrome.createTitlebar(this), BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(0, 14, 14, 14));
        main.add(buildSidebar(), BorderLayout.WEST);
        main.add(buildContentColumn(), BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);

        setContentPane(root);
    }

    // ── Sidebar ──────────────────────────────────────────────────────────────

    private JComponent buildSidebar() {
        Card shell = new Card(Theme.RADIUS_XL);
        shell.setLayout(new BorderLayout());
        shell.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH + Card.shadowInset() * 2, 0));
        shell.setBorder(new EmptyBorder(
                Card.shadowInset() + 22, Card.shadowInset() + 18,
                Card.shadowInset() + 18, Card.shadowInset() + 18));

        shell.add(buildBrand(), BorderLayout.NORTH);

        JPanel navBox = new JPanel();
        navBox.setOpaque(false);
        navBox.setLayout(new BoxLayout(navBox, BoxLayout.Y_AXIS));

        navBox.add(eyebrow(I18n.t("nav.menu.primary")));
        navBox.add(Box.createVerticalStrut(6));
        addNav(navBox, "dashboard", Icons.Kind.HOME, "nav.dashboard");
        addNav(navBox, "catalogue", Icons.Kind.GRID, "nav.catalogue");
        addNav(navBox, "emprunts", Icons.Kind.CLOCK, "nav.emprunts");
        addNav(navBox, "reservations", Icons.Kind.BOOKMARK, "nav.reservations");
        if (isAdmin) addNav(navBox, "utilisateurs", Icons.Kind.USERS, "nav.utilisateurs");

        navBox.add(Box.createVerticalStrut(18));
        navBox.add(divider());
        navBox.add(Box.createVerticalStrut(14));
        navBox.add(eyebrow(I18n.t("nav.menu.account")));
        navBox.add(Box.createVerticalStrut(6));
        addNav(navBox, "profile", Icons.Kind.USER, "nav.profile");
        navBox.add(languagePill());
        navBox.add(aboutPill());

        shell.add(navBox, BorderLayout.CENTER);
        shell.add(buildSidebarFooter(), BorderLayout.SOUTH);
        return shell;
    }

    private JComponent buildBrand() {
        JPanel p = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.aa(g);
                // Bare crest on the light sidebar: no plate, no border, and larger
                // than the old carded version so the detail actually reads.
                Logo.draw(g, 2, (getHeight() - 56) / 2, 56);
                g.dispose();
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(0, 78));
        p.setBorder(new EmptyBorder(0, 66, 0, 0));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBorder(new EmptyBorder(15, 0, 0, 0));

        javax.swing.JLabel name = new javax.swing.JLabel(I18n.t("app.name"));
        name.setFont(Theme.H2);
        name.setForeground(Theme.TEXT);
        name.setAlignmentX(LEFT_ALIGNMENT);

        javax.swing.JLabel sub = new javax.swing.JLabel(I18n.t("app.university.short"));
        sub.setFont(Theme.TINY);
        sub.setForeground(Theme.MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);

        text.add(name);
        text.add(Box.createVerticalStrut(2));
        text.add(sub);
        p.add(text, BorderLayout.CENTER);
        return p;
    }

    private JComponent eyebrow(String text) {
        javax.swing.JLabel l = new javax.swing.JLabel(text.toUpperCase());
        l.setFont(Theme.EYEBROW);
        l.setForeground(Theme.FAINT);
        l.setBorder(new EmptyBorder(10, 10, 4, 0));
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        return l;
    }

    private JComponent divider() {
        JPanel d = new JPanel();
        d.setOpaque(false);
        d.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        d.setPreferredSize(new Dimension(0, 1));
        d.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 8, 0, 8, Theme.DIVIDER));
        return d;
    }

    private void addNav(JPanel box, String key, Icons.Kind icon, String labelKey) {
        NavPill item = new NavPill(icon, I18n.t(labelKey), () -> showPage(key));
        item.putClientProperty("labelKey", labelKey);
        item.setAlignmentX(LEFT_ALIGNMENT);
        navs.put(key, item);
        box.add(item);
    }

    private JComponent languagePill() {
        NavPill pill = new NavPill(Icons.Kind.GLOBE, languageLabel(), () -> {
            I18n.toggleLanguage();
            Toast.show(this, I18n.t("toast.language", I18n.language().label));
        });
        pill.putClientProperty("dynamic", "language");
        pill.setAlignmentX(LEFT_ALIGNMENT);
        navs.put("language", pill);
        return pill;
    }

    private String languageLabel() {
        return I18n.t("action.language") + " · " + I18n.language().code;
    }

    private JComponent aboutPill() {
        NavPill pill = new NavPill(Icons.Kind.INFO, I18n.t("credit.about"), this::showAbout);
        pill.putClientProperty("labelKey", "credit.about");
        pill.setAlignmentX(LEFT_ALIGNMENT);
        navs.put("about", pill);
        return pill;
    }

    private JComponent buildSidebarFooter() {
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

        south.add(new CreditCard());
        south.add(Box.createVerticalStrut(10));

        NavPill logout = new NavPill(Icons.Kind.LOGOUT, I18n.t("nav.logout"), this::logout, true);
        logout.putClientProperty("labelKey", "nav.logout");
        logout.setAlignmentX(LEFT_ALIGNMENT);
        navs.put("logout", logout);
        south.add(logout);
        return south;
    }

    /**
     * The authorship card at the foot of the sidebar. Clicking it opens the portfolio —
     * this is a student project and the credit is part of the deliverable.
     */
    private class CreditCard extends JComponent {
        private boolean hovered;

        CreditCard() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 92));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
            setAlignmentX(LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("https://kamal-aassab.vercel.app/");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (contains(e.getPoint())) openPortfolio();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            int w = getWidth();
            int h = getHeight();

            Theme.gradientRound(g, 4, 4, w - 8, h - 8, Theme.RADIUS,
                    Theme.PRIMARY_DARK, hovered ? Theme.PRIMARY_2 : Theme.PRIMARY);

            // A gold arc in the corner, echoing the crest.
            g.setColor(Theme.alpha(Theme.ACCENT, hovered ? 70 : 46));
            g.fillOval(w - 52, -22, 66, 66);

            g.setFont(Theme.EYEBROW);
            g.setColor(Theme.alpha(Color.WHITE, 160));
            g.drawString(I18n.t("credit.eyebrow").toUpperCase(), 18, 28);

            g.setFont(Theme.SMALL_BOLD);
            g.setColor(Color.WHITE);
            g.drawString("Kamal Aassab", 18, 50);

            g.setFont(Theme.TINY);
            g.setColor(Theme.alpha(Color.WHITE, 190));
            g.drawString(I18n.t("credit.portfolio"), 18, 70);

            if (hovered) {
                Icons.paint(g, Icons.Kind.CHEVRON_RIGHT, w - 34, h / 2 - 8, 16,
                        Theme.alpha(Color.WHITE, 220));
            }
            g.dispose();
        }
    }

    private void openPortfolio() {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("https://kamal-aassab.vercel.app/"));
            }
        } catch (Exception e) {
            Toast.show(this, I18n.t("toast.error"));
        }
    }

    // ── Content column ───────────────────────────────────────────────────────

    private JComponent buildContentColumn() {
        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.setBorder(new EmptyBorder(0, 6, 0, 0));

        column.add(buildTopStrip(), BorderLayout.NORTH);

        content.setOpaque(false);
        dashboard = new DashboardView(user, this::showPage, this::refreshAll);
        catalogue = new CatalogueView(user, this::refreshAll);
        emprunts = new EmpruntsView(user, this::refreshAll);
        reservations = new ReservationsView(user, this::refreshAll);
        utilisateurs = new UtilisateursView(user, this::refreshAll);
        profile = new ProfileView(user, this::onProfileUpdated);

        content.add(dashboard, "dashboard");
        content.add(catalogue, "catalogue");
        content.add(emprunts, "emprunts");
        content.add(reservations, "reservations");
        content.add(utilisateurs, "utilisateurs");
        content.add(profile, "profile");

        column.add(content, BorderLayout.CENTER);
        return column;
    }

    private JComponent buildTopStrip() {
        JPanel strip = new JPanel(new BorderLayout());
        strip.setOpaque(false);
        strip.setBorder(new EmptyBorder(6, 18, 4, 8));
        strip.setPreferredSize(new Dimension(0, 62));

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));

        userChip = new UserChip();
        bell = new BellButton();
        right.add(userChip);
        right.add(Box.createHorizontalStrut(6));
        right.add(bell);

        strip.add(right, BorderLayout.EAST);
        Chrome.makeDraggable(strip, this);
        return strip;
    }

    /** Avatar, name, role and a chevron that opens the account menu. */
    private class UserChip extends JComponent {
        private boolean hovered;

        UserChip() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            resize();
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (contains(e.getPoint())) showAccountMenu(UserChip.this);
                }
            });
        }

        void resize() {
            FontMetrics fm = getFontMetrics(Theme.BODY_BOLD);
            int w = Math.max(180, fm.stringWidth(user.getNom()) + 118);
            Dimension d = new Dimension(w, 52);
            setPreferredSize(d);
            setMaximumSize(d);
            setMinimumSize(d);
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            int w = getWidth();
            int h = getHeight();

            if (hovered) {
                Theme.fillRound(g, 0, 2, w, h - 4, Theme.PILL, Theme.alpha(Color.WHITE, 200));
            }

            int av = 38;
            int ay = (h - av) / 2;
            String initials = Avatar.initialsOf(user.getNom());
            int hash = Math.abs(initials.hashCode());
            float hue = (hash % 360) / 360f;
            g.setPaint(new java.awt.GradientPaint(6, ay,
                    Color.getHSBColor((hue + 0.05f) % 1f, 0.62f, 0.80f),
                    6 + av, ay + av, Color.getHSBColor(hue, 0.55f, 0.62f)));
            g.fillOval(6, ay, av, av);
            g.setFont(Theme.SMALL_BOLD);
            FontMetrics ifm = g.getFontMetrics();
            g.setColor(Color.WHITE);
            g.drawString(initials, 6 + (av - ifm.stringWidth(initials)) / 2,
                    ay + (av - ifm.getHeight()) / 2 + ifm.getAscent());

            g.setFont(Theme.BODY_BOLD);
            g.setColor(Theme.TEXT);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(user.getNom(), 54, h / 2 - 1);

            g.setFont(Theme.TINY);
            g.setColor(Theme.MUTED);
            g.drawString(I18n.t(isAdmin ? "user.role.admin" : "user.role.reader"), 54, h / 2 + 14);

            Icons.paint(g, Icons.Kind.CHEVRON_DOWN, w - 26, (h - 15) / 2, 15, Theme.MUTED);
            g.dispose();
        }
    }

    /** Notification bell showing the count of overdue loans. */
    private class BellButton extends JComponent {
        private boolean hovered;
        private int badge;

        BellButton() {
            setOpaque(false);
            setPreferredSize(new Dimension(48, 48));
            setMaximumSize(new Dimension(48, 48));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!contains(e.getPoint())) return;
                    if (badge > 0) {
                        showPage("emprunts");
                    } else {
                        Toast.show(BiblioGUI.this, I18n.t("toast.noAlerts"));
                    }
                }
            });
        }

        void setBadge(int n) {
            this.badge = n;
            setToolTipText(n > 0 ? I18n.t("dash.stat.overdue") + " : " + n : null);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            int w = getWidth();
            int h = getHeight();

            Theme.fillRound(g, 4, 4, w - 8, h - 8, Theme.PILL,
                    hovered ? Color.WHITE : Theme.alpha(Color.WHITE, 150));
            Icons.paint(g, Icons.Kind.BELL, (w - 20) / 2, (h - 20) / 2, 20, Theme.TEXT_SOFT);

            if (badge > 0) {
                int d = 16;
                int bx = w - 18;
                int by = 8;
                g.setColor(Color.WHITE);
                g.fillOval(bx - 2, by - 2, d + 4, d + 4);
                g.setColor(Theme.DANGER);
                g.fillOval(bx, by, d, d);
                g.setFont(Theme.EYEBROW.deriveFont(9f));
                FontMetrics fm = g.getFontMetrics();
                String t = badge > 9 ? "9+" : String.valueOf(badge);
                g.setColor(Color.WHITE);
                g.drawString(t, bx + (d - fm.stringWidth(t)) / 2,
                        by + (d - fm.getHeight()) / 2 + fm.getAscent());
            }
            g.dispose();
        }
    }

    private void showAccountMenu(JComponent anchor) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(new EmptyBorder(6, 6, 6, 6));
        menu.setBackground(Theme.SURFACE);

        menu.add(menuItem(I18n.t("nav.profile"), () -> showPage("profile")));
        menu.add(menuItem(I18n.t("action.language") + " · " + I18n.language().code, () -> {
            I18n.toggleLanguage();
            Toast.show(this, I18n.t("toast.language", I18n.language().label));
        }));
        menu.add(menuItem(I18n.t("credit.about"), this::showAbout));
        menu.addSeparator();
        menu.add(menuItem(I18n.t("nav.logout"), this::logout));

        menu.show(anchor, 0, anchor.getHeight() - 2);
    }

    private JMenuItem menuItem(String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.setFont(Theme.BODY);
        item.setForeground(Theme.TEXT);
        item.setBackground(Theme.SURFACE);
        item.setBorder(new EmptyBorder(9, 14, 9, 28));
        item.addActionListener(e -> action.run());
        return item;
    }

    private void showAbout() {
        Dialogs.about(this);
    }

    private void logout() {
        boolean confirmed = Dialogs.confirm(this,
                I18n.t("confirm.logout.title"), I18n.t("confirm.logout.body"),
                I18n.t("nav.logout"));
        if (!confirmed) return;
        dispose();
        SwingUtilities.invokeLater(() -> new LoginScreen().setVisible(true));
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    private void showPage(String key) {
        currentPage = key;
        navs.forEach((k, item) -> item.setSelected(k.equals(key)));
        ((CardLayout) content.getLayout()).show(content, key);

        switch (key) {
            case "dashboard" -> dashboard.onShow();
            case "catalogue" -> catalogue.onShow();
            case "emprunts" -> emprunts.onShow();
            case "reservations" -> reservations.onShow();
            case "utilisateurs" -> utilisateurs.onShow();
            case "profile" -> profile.onShow();
            default -> { }
        }
        refreshBadge();
    }

    private void refreshAll() {
        dashboard.onShow();
        catalogue.onShow();
        emprunts.onShow();
        reservations.onShow();
        utilisateurs.onShow();
        refreshBadge();
    }

    private void refreshBadge() {
        if (bell == null) return;
        // Cheap: served from the short-lived read cache in DatabaseConnection.
        bell.setBadge(DatabaseConnection.stats().overdue());
    }

    private void onProfileUpdated(Utilisateur updated) {
        this.user = updated;
        if (userChip != null) userChip.resize();
        refreshAll();
    }

    // ── Language ─────────────────────────────────────────────────────────────

    private void applyLanguage() {
        setTitle(I18n.t("app.name") + " — " + I18n.t("app.tagline"));
        navs.forEach((key, pill) -> {
            Object dynamic = pill.getClientProperty("dynamic");
            if ("language".equals(dynamic)) {
                pill.setLabel(languageLabel());
                return;
            }
            Object labelKey = pill.getClientProperty("labelKey");
            if (labelKey != null) pill.setLabel(I18n.t(String.valueOf(labelKey)));
        });
        repaint();
        // Views rebuild their own copy on the next show; force the visible one now.
        showPage(currentPage);
    }

    /** Retained for compatibility with older call sites. */
    static String initials(String nom) {
        return Avatar.initialsOf(nom);
    }
}
