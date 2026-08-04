import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LoginScreen extends JFrame {
    private final LoginBg bg = new LoginBg();
    private JPanel card;
    private RoundedTextField userField;
    private RoundedPasswordField passField;
    private JLabel errorLabel;
    private RoundedButton loginBtn;
    private int cardX, cardY;
    private final int cardW = 392, cardH = 500;
    private Timer shake;

    public LoginScreen() {
        Chrome.prepare(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 660);
        setContentPane(bg);
        build();
        setLocationRelativeTo(null);
        bg.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                layoutCards();
            }
        });
    }

    private void build() {
        try {
            DatabaseConnection.ensureSchema();
            DatabaseConnection.seedIfEmpty();
        } catch (Exception ex) {
            System.out.println("Attention: " + ex.getMessage());
        }
        buildCard();
        buildWindowButtons();
        layoutCards();
    }

    private void buildWindowButtons() {
        RoundedButton min = new RoundedButton("─", RoundedButton.Style.GHOST).setCustomHover(new Color(255, 255, 255));
        min.setForeground(new Color(255, 255, 255, 190));
        min.setPreferredSize(new Dimension(40, 28));
        min.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        min.addActionListener(e -> setState(JFrame.ICONIFIED));
        min.setBounds(bg.getWidth() - 96, 14, 40, 28);

        RoundedButton close = new RoundedButton("✕", RoundedButton.Style.GHOST).setCustomHover(new Color(231, 76, 60));
        close.setForeground(new Color(255, 255, 255, 190));
        close.setPreferredSize(new Dimension(40, 28));
        close.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        close.addActionListener(e -> {
            dispose();
            System.exit(0);
        });
        close.setBounds(bg.getWidth() - 50, 14, 40, 28);

        bg.add(min);
        bg.add(close);
    }

    private void buildCard() {
        card = new RoundedPanel(Color.WHITE, 22);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Connexion");
        title.setFont(Theme.H1);
        title.setForeground(Theme.TEXT);
        card.add(title, gbc);

        gbc.gridy++;
        JLabel sub = new JLabel("Bienvenue, identifiez-vous pour accéder à votre espace");
        sub.setFont(Theme.SMALL);
        sub.setForeground(Theme.MUTED);
        card.add(sub, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(26, 0, 0, 0);
        JLabel ln = new JLabel("Nom d'utilisateur");
        ln.setFont(Theme.SMALL_BOLD);
        ln.setForeground(Theme.TEXT);
        card.add(ln, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 0, 0);
        userField = new RoundedTextField("ex. admin");
        userField.setPreferredSize(new Dimension(0, 46));
        userField.addActionListener(e -> passField.requestFocus());
        card.add(userField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(18, 0, 0, 0);
        JLabel lp = new JLabel("Mot de passe");
        lp.setFont(Theme.SMALL_BOLD);
        lp.setForeground(Theme.TEXT);
        card.add(lp, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 0, 0);
        passField = new RoundedPasswordField("••••••••");
        passField.setPreferredSize(new Dimension(0, 46));
        passField.addActionListener(e -> doLogin());
        card.add(passField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(12, 0, 0, 0);
        errorLabel = new JLabel(" ");
        errorLabel.setFont(Theme.SMALL);
        errorLabel.setForeground(Theme.DANGER);
        card.add(errorLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(14, 0, 0, 0);
        loginBtn = new RoundedButton("Se connecter", RoundedButton.Style.PRIMARY);
        loginBtn.setPreferredSize(new Dimension(0, 48));
        loginBtn.addActionListener(e -> doLogin());
        card.add(loginBtn, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(16, 0, 0, 0);
        JLabel hint = new JLabel("<html><div style='text-align:center'>Démo — <b>admin</b> / admin123 · <b>lecteur</b> / lecteur123</div></html>");
        hint.setFont(Theme.SMALL);
        hint.setForeground(Theme.MUTED);
        hint.setHorizontalAlignment(JLabel.CENTER);
        card.add(hint, gbc);

        bg.add(card);
    }

    private void layoutCards() {
        int w = bg.getWidth(), h = bg.getHeight();
        cardX = w - cardW - 58;
        cardY = (h - cardH) / 2;
        card.setBounds(cardX, cardY, cardW, cardH);
        bg.repaint();
    }

    private void doLogin() {
        String nom = userField.getText().trim();
        String mdp = new String(passField.getPassword());
        if (nom.isEmpty() || mdp.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }
        try {
            Utilisateur u = DatabaseConnection.authentifier(nom, mdp);
            if (u == null) {
                showError("Nom d'utilisateur ou mot de passe incorrect.");
                shake();
                return;
            }
            BiblioGUI gui = new BiblioGUI(u);
            gui.setVisible(true);
            dispose();
        } catch (Exception ex) {
            showError("Base de données indisponible. Vérifiez DATABASE_URL, le fichier .env ou %LOCALAPPDATA%\\Biblio-Java\\database.url.");
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
    }

    private void shake() {
        if (shake != null && shake.isRunning()) shake.stop();
        final int[] step = {0};
        final int base = cardX;
        shake = new Timer(14, e -> {
            step[0]++;
            int dx = (int) (Math.sin(step[0] * 0.9) * 12 * Math.max(0, 1 - step[0] / 18.0));
            card.setLocation(base + dx, cardY);
            if (step[0] >= 18) {
                card.setLocation(base, cardY);
                ((Timer) e.getSource()).stop();
            }
        });
        shake.start();
    }

    class LoginBg extends RoundedPanel {
        LoginBg() {
            super(new Color(0x0A, 0x1D, 0x36), 26);
            setGradient(new Color(0x0A, 0x1D, 0x36), new Color(0x00, 0x40, 0x80));
            setLayout(null);
            Chrome.makeDraggable(this, LoginScreen.this);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            g.setColor(new Color(255, 255, 255, 14));
            g.fillOval(-120, h - 260, 340, 340);
            g.fillOval(w - 300, -160, 420, 420);

            int x = 74;
            int y = (int) (h * 0.18);
            if (Logo.AVAILABLE) {
                Logo.draw(g, x, y, 66, 66);
            } else {
                Theme.gradientRound(g, x, y, 62, 62, 18, Theme.PRIMARY, Theme.PRIMARY_2);
                Icons.paint(g, Icons.Kind.BOOK, x + 18, y + 18, 26, Color.WHITE);
            }

            g.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 42));
            g.setColor(Color.WHITE);
            g.drawString("Biblio", x, y + 128);

            g.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 15));
            g.setColor(new Color(255, 255, 255, 200));
            g.drawString("Faculté des Sciences et Techniques de Settat", x, y + 154);

            drawFeature(g, x, y + 210, "Catalogue intelligent et élégant");
            drawFeature(g, x, y + 258, "Suivi des emprunts et réservations");
            drawFeature(g, x, y + 306, "Accès sécurisé par rôle (Admin / Lecteur)");

            g.setFont(Theme.SMALL);
            g.setColor(new Color(255, 255, 255, 120));
            g.drawString("FST Settat · Biblio © 2026", x, h - 34);
            g.dispose();
        }

        private void drawFeature(Graphics2D g, int x, int y, String text) {
            g.setColor(new Color(255, 255, 255, 160));
            Icons.paint(g, Icons.Kind.CHECK, x, y - 10, 18, new Color(96, 232, 158));
            g.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 15));
            g.setColor(new Color(255, 255, 255, 215));
            g.drawString(text, x + 30, y);
        }
    }
}
