import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The sign-in window: an editorial panel on the left carrying the institutional
 * identity, and the credential form on the right.
 *
 * <p>Authentication runs on a background worker. It performs a PBKDF2 verification
 * (deliberately expensive) plus a network round trip to Neon, and doing that on the
 * event dispatch thread would freeze the window for the duration.
 */
public class LoginScreen extends JFrame {

    private final RoundedTextField userField =
            new RoundedTextField(I18n.t("login.username.hint")).withIcon(Icons.Kind.USER);
    private final RoundedPasswordField passField =
            new RoundedPasswordField(I18n.t("login.password.hint"));
    private final RoundedButton submit =
            new RoundedButton(I18n.t("login.submit"), RoundedButton.Style.PRIMARY);
    private final JLabel error = new JLabel(" ");

    public LoginScreen() {
        Chrome.prepare(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle(I18n.t("app.name"));
        setSize(1040, 660);
        setResizable(false);

        build();
        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(userField::requestFocusInWindow);

        // Warm the connection pool and apply pending migrations without blocking the UI.
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    if (!DatabaseConnection.isConfigured()) return null;
                    DatabaseConnection.ensureSchema();
                    DatabaseConnection.seedIfEmpty();
                } catch (Exception e) {
                    System.err.println("[startup] " + Security.redact(String.valueOf(e.getMessage())));
                }
                return null;
            }
        }.execute();
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.aa(g);
                Theme.fillRound(g, 0, 0, getWidth(), getHeight(), 22, Theme.CANVAS);
                g.dispose();
            }
        };
        root.setOpaque(false);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(new HeroPanel(), BorderLayout.WEST);
        body.add(buildFormSide(), BorderLayout.CENTER);

        root.add(buildTitlebar(), BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JComponent buildTitlebar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 34));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        right.setOpaque(false);
        right.add(new LanguageToggle());
        right.add(Chrome.windowControls(this, false));
        bar.add(right, BorderLayout.EAST);
        Chrome.makeDraggable(bar, this);
        return bar;
    }

    /** A compact FR / EN switch. */
    private class LanguageToggle extends JComponent {
        private boolean hovered;

        LanguageToggle() {
            setOpaque(false);
            setPreferredSize(new Dimension(78, 26));
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
                    I18n.toggleLanguage();
                    relabel();
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            int w = getWidth();
            int h = getHeight();

            Theme.fillRound(g, 0, 2, w, h - 4, Theme.PILL,
                    hovered ? Theme.SURFACE : Theme.alpha(Theme.SURFACE, 170));
            Icons.paint(g, Icons.Kind.GLOBE, 9, (h - 14) / 2, 14, Theme.MUTED);

            g.setFont(Theme.EYEBROW);
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Theme.TEXT_SOFT);
            g.drawString(I18n.language().code, 30, (h - fm.getHeight()) / 2 + fm.getAscent());

            Icons.paint(g, Icons.Kind.CHEVRON_DOWN, w - 22, (h - 12) / 2, 12, Theme.FAINT);
            g.dispose();
        }
    }

    /** The left-hand editorial panel: crest, headline, and demo credentials. */
    private static class HeroPanel extends JComponent {
        private float phase;

        HeroPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(470, 0));
            // A very slow drift on the background orbs; enough to feel alive, not enough to distract.
            new Timer(50, e -> {
                phase += 0.006f;
                repaint();
            }).start();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            int w = getWidth();
            int h = getHeight();

            g.setPaint(new GradientPaint(0, 0, Theme.PRIMARY_DARK, w, h, Theme.PRIMARY));
            g.fillRoundRect(14, 6, w - 14, h - 20, 40, 40);

            java.awt.Shape clip = g.getClip();
            g.clipRect(14, 6, w - 14, h - 20);
            g.setColor(Theme.alpha(Theme.ACCENT, 46));
            g.fillOval((int) (w - 150 + Math.sin(phase) * 18), (int) (-60 + Math.cos(phase) * 14), 230, 230);
            g.setColor(Theme.alpha(Theme.PRIMARY_2, 120));
            g.fillOval((int) (-70 + Math.cos(phase * 0.8f) * 16), (int) (h - 220 + Math.sin(phase * 1.1f) * 12), 240, 240);
            g.setClip(clip);

            Logo.drawCard(g, 52, 56, 62, 17);

            g.setFont(Theme.H2);
            g.setColor(Color.WHITE);
            g.drawString(I18n.t("app.name"), 130, 84);
            g.setFont(Theme.SMALL);
            g.setColor(Theme.alpha(Color.WHITE, 190));
            g.drawString(I18n.t("app.university.short"), 130, 105);

            // Headline. The panel is a fixed width, so step the display size down until
            // every line fits rather than letting a long translation run off the edge.
            int textWidth = w - 118;
            java.util.List<String> lines = new java.util.ArrayList<>();
            FontMetrics fm = null;
            for (float size = 34f; size >= 22f; size -= 2f) {
                g.setFont(Theme.DISPLAY_SM.deriveFont(size));
                fm = g.getFontMetrics();
                lines.clear();
                for (String paragraph : I18n.t("login.hero.title").split("\n")) {
                    lines.addAll(wrap(paragraph, fm, textWidth));
                }
                boolean fits = true;
                for (String line : lines) {
                    if (fm.stringWidth(line) > textWidth) fits = false;
                }
                if (fits) break;
            }

            int y = h / 2 - 40 - Math.max(0, (lines.size() - 2)) * (fm.getHeight() / 2);
            g.setColor(Color.WHITE);
            for (String line : lines) {
                g.drawString(line, 52, y);
                y += fm.getHeight() - 4;
            }

            g.setFont(Theme.BODY);
            FontMetrics sfm = g.getFontMetrics();
            g.setColor(Theme.alpha(Color.WHITE, 200));
            int ty = y + 14;
            for (String line : wrap(I18n.t("login.hero.sub"), sfm, textWidth)) {
                g.drawString(line, 52, ty);
                ty += sfm.getHeight();
            }

            // Demo credentials, so a reviewer can get in without asking.
            int cardY = h - 168;
            Theme.fillRound(g, 52, cardY, w - 118, 96, Theme.RADIUS, Theme.alpha(Color.WHITE, 28));
            g.setFont(Theme.EYEBROW);
            g.setColor(Theme.alpha(Color.WHITE, 170));
            g.drawString(I18n.t("login.demo.title").toUpperCase(), 72, cardY + 26);

            g.setFont(Theme.SMALL);
            g.setColor(Color.WHITE);
            g.drawString(I18n.t("login.demo.admin") + " — admin / admin123", 72, cardY + 52);
            g.drawString(I18n.t("login.demo.reader") + " — lecteur / lecteur123", 72, cardY + 74);

            g.setFont(Theme.TINY);
            g.setColor(Theme.alpha(Color.WHITE, 150));
            g.drawString(I18n.t("credit.builtBy"), 52, h - 44);

            g.dispose();
        }

        private java.util.List<String> wrap(String text, FontMetrics fm, int maxWidth) {
            java.util.List<String> out = new java.util.ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : text.split("\\s+")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(candidate) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                }
            }
            if (line.length() > 0) out.add(line.toString());
            return out;
        }
    }

    private JComponent buildFormSide() {
        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        side.setBorder(new EmptyBorder(0, 44, 0, 52));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(84, 0, 0, 0));

        JLabel welcome = new JLabel(I18n.t("login.welcome"));
        welcome.setFont(Theme.H1);
        welcome.setForeground(Theme.TEXT);
        welcome.setAlignmentX(LEFT_ALIGNMENT);
        welcome.putClientProperty("key", "login.welcome");

        JLabel subtitle = new JLabel(I18n.t("login.subtitle"));
        subtitle.setFont(Theme.BODY);
        subtitle.setForeground(Theme.MUTED);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        subtitle.putClientProperty("key", "login.subtitle");

        form.add(welcome);
        form.add(Box.createVerticalStrut(8));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(34));

        form.add(labelled("login.username", userField));
        form.add(Box.createVerticalStrut(16));
        form.add(labelled("login.password", passField));
        form.add(Box.createVerticalStrut(10));

        error.setFont(Theme.SMALL);
        error.setForeground(Theme.DANGER);
        error.setAlignmentX(LEFT_ALIGNMENT);
        form.add(error);
        form.add(Box.createVerticalStrut(14));

        submit.setAlignmentX(LEFT_ALIGNMENT);
        submit.setPreferredSize(new Dimension(Integer.MAX_VALUE, 54));
        submit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        submit.addActionListener(e -> attemptLogin());
        form.add(submit);

        getRootPane().setDefaultButton(submit);
        passField.addActionListener(e -> attemptLogin());
        userField.addActionListener(e -> passField.requestFocusInWindow());

        form.add(Box.createVerticalGlue());
        side.add(form, BorderLayout.CENTER);
        return side;
    }

    private JComponent labelled(String labelKey, JComponent field) {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setAlignmentX(LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));

        JLabel label = new JLabel(I18n.t(labelKey));
        label.setFont(Theme.SMALL_BOLD);
        label.setForeground(Theme.TEXT_SOFT);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.putClientProperty("key", labelKey);

        field.setAlignmentX(LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        field.setPreferredSize(new Dimension(0, 54));

        wrap.add(label);
        wrap.add(Box.createVerticalStrut(6));
        wrap.add(field);
        return wrap;
    }

    /** Re-reads every label after a language switch. */
    private void relabel() {
        setTitle(I18n.t("app.name"));
        submit.setText(I18n.t("login.submit"));
        error.setText(" ");
        applyKeys(getContentPane());
        repaint();
    }

    private void applyKeys(java.awt.Container container) {
        for (java.awt.Component c : container.getComponents()) {
            if (c instanceof JLabel label) {
                Object key = label.getClientProperty("key");
                if (key != null) label.setText(I18n.t(String.valueOf(key)));
            }
            if (c instanceof java.awt.Container child) applyKeys(child);
        }
    }

    private void attemptLogin() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError(I18n.t("login.error.empty"));
            if (username.isEmpty()) userField.markError();
            if (password.isEmpty()) passField.markError();
            return;
        }

        submit.setLoading(true);
        error.setText(" ");

        new SwingWorker<Object, Void>() {
            @Override
            protected Object doInBackground() {
                try {
                    return DatabaseConnection.authentifier(username, password);
                } catch (IllegalStateException e) {
                    // Carries a localised, user-safe message (lockout or connectivity).
                    return e;
                }
            }

            @Override
            protected void done() {
                submit.setLoading(false);
                Object result;
                try {
                    result = get();
                } catch (Exception e) {
                    showError(I18n.t("login.error.connection"));
                    return;
                }

                if (result instanceof IllegalStateException e) {
                    showError(e.getMessage());
                    return;
                }
                if (result == null) {
                    showError(I18n.t("login.error.invalid"));
                    passField.markError();
                    passField.setText("");
                    return;
                }

                Utilisateur user = (Utilisateur) result;
                dispose();
                SwingUtilities.invokeLater(() -> new BiblioGUI(user).setVisible(true));
            }
        }.execute();
    }

    private void showError(String message) {
        error.setText(message);
    }
}
