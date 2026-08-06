import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

/** Modal dialogs shared across the application. */
public final class Dialogs {

    private Dialogs() {}

    /**
     * A blocking confirmation.
     *
     * @return true when the user chose the confirming action
     */
    public static boolean confirm(Window owner, String title, String body, String confirmLabel) {
        return confirm(owner, title, body, confirmLabel, true);
    }

    public static boolean confirm(Window owner, String title, String body,
                                  String confirmLabel, boolean destructive) {
        final boolean[] result = {false};

        JDialog dialog = new JDialog(owner, "", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        Card card = new Card(Theme.RADIUS_XL);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(
                Card.shadowInset() + 30, Card.shadowInset() + 30,
                Card.shadowInset() + 24, Card.shadowInset() + 30));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel iconRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.aa(g);
                Color tint = destructive ? Theme.DANGER : Theme.PRIMARY;
                Theme.fillRound(g, 0, 0, 52, 52, 16, Theme.alpha(tint, 28));
                Icons.paint(g, destructive ? Icons.Kind.TRASH : Icons.Kind.INFO,
                        16, 16, 20, tint);
                g.dispose();
            }
        };
        iconRow.setOpaque(false);
        iconRow.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        iconRow.setPreferredSize(new Dimension(52, 52));
        iconRow.setMaximumSize(new Dimension(52, 52));
        content.add(iconRow);
        content.add(Box.createVerticalStrut(18));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.H2);
        titleLabel.setForeground(Theme.TEXT);
        titleLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(10));

        JLabel bodyLabel = new JLabel("<html><body style='width:340px'>" + escape(body) + "</body></html>");
        bodyLabel.setFont(Theme.BODY);
        bodyLabel.setForeground(Theme.MUTED);
        bodyLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        content.add(bodyLabel);
        content.add(Box.createVerticalStrut(26));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        RoundedButton cancel = new RoundedButton(I18n.t("action.cancel"),
                RoundedButton.Style.SECONDARY);
        cancel.setPreferredSize(new Dimension(126, 46));
        cancel.addActionListener(e -> dialog.dispose());

        RoundedButton ok = new RoundedButton(confirmLabel,
                destructive ? RoundedButton.Style.DANGER : RoundedButton.Style.PRIMARY);
        ok.setPreferredSize(new Dimension(150, 46));
        ok.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });

        buttons.add(cancel);
        buttons.add(ok);
        content.add(buttons);

        card.add(content, BorderLayout.CENTER);
        dialog.setContentPane(card);
        dialog.pack();
        dialog.setSize(Math.max(470, dialog.getWidth()), dialog.getHeight());
        dialog.setLocationRelativeTo(owner);

        // Escape cancels, Enter confirms.
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.getRootPane().setDefaultButton(ok);

        dialog.setVisible(true);
        return result[0];
    }

    /** Project and authorship information. */
    public static void about(Window owner) {
        JDialog dialog = new JDialog(owner, "", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        Card card = new Card(Theme.RADIUS_XL);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(
                Card.shadowInset(), Card.shadowInset(),
                Card.shadowInset() + 26, Card.shadowInset()));

        JPanel banner = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.aa(g);
                int w = getWidth();
                int h = getHeight();
                Theme.gradientRound(g, 0, 0, w, h + 40, Theme.RADIUS_XL,
                        Theme.PRIMARY_DARK, Theme.PRIMARY);
                g.setColor(Theme.alpha(Theme.ACCENT, 58));
                g.fillOval(w - 120, -66, 170, 170);

                Logo.drawCard(g, 30, 30, 60, 16);

                g.setFont(Theme.H1);
                g.setColor(Color.WHITE);
                g.drawString(I18n.t("app.name"), 106, 60);
                g.setFont(Theme.SMALL);
                g.setColor(Theme.alpha(Color.WHITE, 200));
                g.drawString(I18n.t("app.tagline"), 106, 82);
                g.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setPreferredSize(new Dimension(0, 122));
        card.add(banner, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 30, 0, 30));

        content.add(paragraph(I18n.t("credit.about.body")));
        content.add(Box.createVerticalStrut(16));
        content.add(row(Icons.Kind.HOME, I18n.t("app.university")));
        content.add(Box.createVerticalStrut(8));
        content.add(row(Icons.Kind.USER, I18n.t("credit.builtBy")));
        content.add(Box.createVerticalStrut(8));
        content.add(link(Icons.Kind.GLOBE, "https://kamal-aassab.vercel.app/"));
        content.add(Box.createVerticalStrut(24));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        RoundedButton close = new RoundedButton(I18n.t("action.close"), RoundedButton.Style.PRIMARY);
        close.setPreferredSize(new Dimension(140, 46));
        close.addActionListener(e -> dialog.dispose());
        buttons.add(close);
        content.add(buttons);

        card.add(content, BorderLayout.CENTER);
        dialog.setContentPane(card);
        dialog.pack();
        dialog.setSize(Math.max(540, dialog.getWidth()), dialog.getHeight());
        dialog.setLocationRelativeTo(owner);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setVisible(true);
    }

    private static JComponent paragraph(String text) {
        JLabel l = new JLabel("<html><body style='width:420px'>" + escape(text) + "</body></html>");
        l.setFont(Theme.BODY);
        l.setForeground(Theme.TEXT_SOFT);
        l.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        return l;
    }

    private static JComponent row(Icons.Kind icon, String text) {
        JPanel p = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.aa(g);
                Icons.paint(g, icon, 0, (getHeight() - 17) / 2, 17, Theme.PRIMARY);
                g.dispose();
            }
        };
        p.setOpaque(false);
        p.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        p.setBorder(new EmptyBorder(0, 28, 0, 0));

        JLabel l = new JLabel(text);
        l.setFont(Theme.SMALL);
        l.setForeground(Theme.TEXT_SOFT);
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private static JComponent link(Icons.Kind icon, String url) {
        JComponent row = row(icon, url);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        for (java.awt.Component c : ((JPanel) row).getComponents()) {
            if (c instanceof JLabel label) label.setForeground(Theme.PRIMARY);
        }
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                try {
                    if (Desktop.isDesktopSupported()
                            && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI(url));
                    }
                } catch (Exception ignored) {
                    // Nothing useful to tell the user if the platform has no browser hook.
                }
            }
        });
        return row;
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
    }
}
