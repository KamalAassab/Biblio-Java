import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public class BiblioGUI extends JFrame {
    private final Utilisateur user;
    private final boolean isAdmin;
    private final Map<String, NavItem> navs = new LinkedHashMap<>();
    private final JPanel content = new JPanel(new CardLayout());
    private JLabel pageTitle;
    private JLabel pageSub;
    private SearchField headerSearch;

    private DashboardView dashboard;
    private CatalogueView catalogue;
    private EmpruntsView emprunts;
    private ReservationsView reservations;
    private UtilisateursView utilisateurs;

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
        setSize(1320, 840);
        build();
        setLocationRelativeTo(null);
        showPage("dashboard");
    }

    private void build() {
        RoundedPanel root = new RoundedPanel(Theme.SIDEBAR, 26);
        root.setLayout(new BorderLayout());
        root.add(Chrome.createTitlebar(this), BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(buildSidebar(), BorderLayout.WEST);
        main.add(buildRight(), BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildRight() {
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(buildHeader(), BorderLayout.NORTH);

        RoundedPanel contentCard = new RoundedPanel(Theme.BG, 20);
        contentCard.setLayout(new BorderLayout());
        contentCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));

        content.setOpaque(false);
        dashboard = new DashboardView(user, () -> showPage("catalogue"), this::refreshAll);
        catalogue = new CatalogueView(user, this::refreshAll);
        emprunts = new EmpruntsView(user, this::refreshAll);
        reservations = new ReservationsView(user, this::refreshAll);
        utilisateurs = new UtilisateursView();

        content.add(dashboard, "dashboard");
        content.add(catalogue, "catalogue");
        content.add(emprunts, "emprunts");
        content.add(reservations, "reservations");
        content.add(utilisateurs, "utilisateurs");

        contentCard.add(content, BorderLayout.CENTER);
        right.add(contentCard, BorderLayout.CENTER);
        return right;
    }

    private JPanel buildHeader() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        RoundedPanel head = new RoundedPanel(Theme.PRIMARY, 16);
        head.setGradient(Theme.PRIMARY, Theme.PRIMARY_2);
        head.setLayout(new BorderLayout());
        head.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 18));

        JPanel tt = new JPanel();
        tt.setOpaque(false);
        tt.setLayout(new BoxLayout(tt, BoxLayout.Y_AXIS));
        pageTitle = new JLabel("");
        pageTitle.setFont(Theme.H2);
        pageTitle.setForeground(Color.WHITE);
        pageSub = new JLabel("");
        pageSub.setFont(Theme.SMALL);
        pageSub.setForeground(new Color(255, 255, 255, 190));
        tt.add(pageTitle);
        tt.add(Box.createVerticalStrut(2));
        tt.add(pageSub);
        head.add(tt, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        right.setOpaque(false);

        headerSearch = new SearchField();
        headerSearch.setPreferredSize(new Dimension(320, 44));
        headerSearch.getDocument().addDocumentListener(new SimpleDocListener(() -> {
            String q = headerSearch.getText().trim();
            if (!q.isEmpty()) {
                catalogue.setFilter(q);
                showPage("catalogue");
            }
        }));

        IconButton refresh = new IconButton(Icons.Kind.REFRESH, 18)
                .withColors(new Color(255, 255, 255, 210), new Color(255, 255, 255, 40), Color.WHITE);
        refresh.setToolTipText("Rafraîchir");
        refresh.addActionListener(e -> refreshAll());

        right.add(headerSearch);
        right.add(refresh);
        right.add(new Avatar(initials(user.getNom()), 42));
        head.add(right, BorderLayout.EAST);

        wrap.add(head, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildSidebar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(236, 0));
        p.setBorder(BorderFactory.createEmptyBorder(14, 16, 18, 16));

        p.add(new SidebarHeader(), BorderLayout.NORTH);

        JPanel navBox = new JPanel();
        navBox.setOpaque(false);
        navBox.setLayout(new BoxLayout(navBox, BoxLayout.Y_AXIS));
        addNav(navBox, "dashboard", Icons.Kind.DASHBOARD, "Tableau de bord");
        addNav(navBox, "catalogue", Icons.Kind.BOOK, "Catalogue");
        addNav(navBox, "emprunts", Icons.Kind.CLOCK, "Emprunts");
        addNav(navBox, "reservations", Icons.Kind.BELL, "Réservations");
        if (isAdmin) addNav(navBox, "utilisateurs", Icons.Kind.USERS, "Utilisateurs");
        p.add(navBox, BorderLayout.CENTER);

        p.add(buildSidebarBottom(), BorderLayout.SOUTH);
        return p;
    }

    private void addNav(JPanel box, String key, Icons.Kind icon, String label) {
        NavItem item = new NavItem(icon, label, false, () -> showPage(key));
        navs.put(key, item);
        box.add(item);
        box.add(Box.createVerticalStrut(6));
    }

    private JPanel buildSidebarBottom() {
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

        RoundedPanel uc = new RoundedPanel(Theme.SIDEBAR_HOVER, 14);
        uc.setLayout(new BorderLayout());
        uc.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        uc.add(new Avatar(initials(user.getNom()), 42), BorderLayout.WEST);
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(user.getNom());
        name.setFont(Theme.FONT_BOLD);
        name.setForeground(Color.WHITE);
        JLabel role = new JLabel(isAdmin ? "Administrateur" : "Lecteur");
        role.setFont(Theme.SMALL);
        role.setForeground(new Color(255, 255, 255, 150));
        info.add(name);
        info.add(Box.createVerticalStrut(2));
        info.add(role);
        JPanel infoWrap = new JPanel(new BorderLayout());
        infoWrap.setOpaque(false);
        infoWrap.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        infoWrap.add(info, BorderLayout.CENTER);
        uc.add(infoWrap, BorderLayout.CENTER);
        south.add(uc);
        south.add(Box.createVerticalStrut(14));

        NavItem logout = new NavItem(Icons.Kind.LOGOUT, "Déconnexion", true, () -> {
            dispose();
            new LoginScreen().setVisible(true);
        });
        south.add(logout);
        return south;
    }

    private void showPage(String key) {
        navs.forEach((k, it) -> it.setSelected(k.equals(key)));
        ((CardLayout) content.getLayout()).show(content, key);
        switch (key) {
            case "dashboard":
                pageTitle.setText("Tableau de bord");
                pageSub.setText("Vue d'ensemble de la bibliothèque");
                dashboard.onShow();
                break;
            case "catalogue":
                pageTitle.setText("Catalogue");
                pageSub.setText("Tous les livres de la bibliothèque");
                catalogue.onShow();
                break;
            case "emprunts":
                pageTitle.setText("Emprunts");
                pageSub.setText("Suivi des emprunts en cours");
                emprunts.onShow();
                break;
            case "reservations":
                pageTitle.setText("Réservations");
                pageSub.setText("Réservations des lecteurs");
                reservations.onShow();
                break;
            case "utilisateurs":
                pageTitle.setText("Utilisateurs");
                pageSub.setText("Membres de la bibliothèque");
                utilisateurs.onShow();
                break;
        }
    }

    private void refreshAll() {
        dashboard.onShow();
        catalogue.onShow();
        emprunts.onShow();
        reservations.onShow();
        utilisateurs.onShow();
    }

    static String initials(String nom) {
        if (nom == null || nom.trim().isEmpty()) return "?";
        String[] parts = nom.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty()) sb.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    static class SimpleDocListener implements DocumentListener {
        private final Runnable onChange;

        SimpleDocListener(Runnable onChange) {
            this.onChange = onChange;
        }

        public void insertUpdate(DocumentEvent e) {
            onChange.run();
        }

        public void removeUpdate(DocumentEvent e) {
            onChange.run();
        }

        public void changedUpdate(DocumentEvent e) {
            onChange.run();
        }
    }

    class NavItem extends JPanel {
        private final Icons.Kind icon;
        private final String label;
        private final boolean danger;
        private final Runnable action;
        private boolean selected;
        private boolean over;
        private float t;
        private Timer anim;

        NavItem(Icons.Kind icon, String label, boolean danger, Runnable action) {
            this.icon = icon;
            this.label = label;
            this.danger = danger;
            this.action = action;
            setOpaque(false);
            setPreferredSize(new Dimension(204, 50));
            setMaximumSize(new Dimension(204, 50));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    over = true;
                    start();
                }

                public void mouseExited(MouseEvent e) {
                    over = false;
                    start();
                }

                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
            });
            anim = new Timer(16, e -> {
                float target = selected || over ? 1f : 0f;
                t += (target - t) * 0.22f;
                if (Math.abs(target - t) < 0.01f) {
                    t = target;
                    ((Timer) e.getSource()).stop();
                }
                repaint();
            });
        }

        void setSelected(boolean b) {
            selected = b;
            start();
        }

        private void start() {
            if (!anim.isRunning()) anim.start();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (t > 0) {
                if (selected && !danger) {
                    Theme.gradientRound(g, 6, 6, w - 12, h - 12, 12, Theme.PRIMARY, Theme.PRIMARY_2);
                } else if (danger && (selected || over)) {
                    g.setColor(new Color(239, 68, 68, Math.round(46 * t)));
                    g.fillRoundRect(6, 6, w - 12, h - 12, 12, 12);
                } else {
                    g.setColor(new Color(255, 255, 255, Math.round(20 * t)));
                    g.fillRoundRect(6, 6, w - 12, h - 12, 12, 12);
                }
            }
            Color iconC;
            Color textC;
            if (selected && !danger) {
                iconC = Color.WHITE;
                textC = Color.WHITE;
            } else if (danger && (over || selected)) {
                iconC = new Color(248, 113, 113);
                textC = new Color(248, 113, 113);
            } else {
                iconC = Theme.mix(new Color(181, 186, 209), Color.WHITE, t * 0.4f);
                textC = Theme.mix(new Color(196, 201, 224), Color.WHITE, t * 0.4f);
            }
            Icons.paint(g, icon, 22, (h - 22) / 2, 22, iconC);
            g.setFont(Theme.FONT_BOLD);
            g.setColor(textC);
            FontMetrics fm = g.getFontMetrics();
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(label, 56, ty);
            g.dispose();
        }
    }

    class SidebarHeader extends JPanel {
        SidebarHeader() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 96));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (Logo.AVAILABLE) {
                Logo.draw(g, 6, 20, 50, 50);
            } else {
                Theme.gradientRound(g, 4, 22, 48, 48, 15, Theme.PRIMARY, Theme.PRIMARY_2);
                Icons.paint(g, Icons.Kind.BOOK, 17, 35, 22, Color.WHITE);
            }
            g.setFont(Theme.H2);
            g.setColor(Color.WHITE);
            g.drawString("Biblio", 68, 46);
            g.setFont(Theme.SMALL);
            g.setColor(new Color(255, 255, 255, 160));
            g.drawString("Bibliothèque FST Settat", 68, 64);
            g.dispose();
        }
    }
}
