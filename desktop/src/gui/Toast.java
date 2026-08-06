import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;

/**
 * A transient confirmation that slides up from the bottom of the window.
 *
 * <p>Placed in the layered pane rather than a popup window so it inherits the frame's
 * rounded, undecorated styling and cannot outlive the window that spawned it.
 */
public final class Toast {

    private Toast() {}

    private static final int VISIBLE_MS = 2_600;

    public static void show(Component anchor, String message) {
        show(anchor, message, Kind.INFO);
    }

    public static void success(Component anchor, String message) {
        show(anchor, message, Kind.SUCCESS);
    }

    public static void error(Component anchor, String message) {
        show(anchor, message, Kind.ERROR);
    }

    public enum Kind { INFO, SUCCESS, ERROR }

    private static void show(Component anchor, String message, Kind kind) {
        if (anchor == null || message == null || message.isBlank()) return;

        Window window = SwingUtilities.getWindowAncestor(anchor);
        if (!(window instanceof javax.swing.RootPaneContainer container)) return;

        JLayeredPane layers = container.getLayeredPane();
        Bubble bubble = new Bubble(message, kind);

        Dimension size = bubble.getPreferredSize();
        int x = (layers.getWidth() - size.width) / 2;
        int startY = layers.getHeight() - 20;
        int endY = layers.getHeight() - size.height - 34;

        bubble.setBounds(x, startY, size.width, size.height);
        layers.add(bubble, JLayeredPane.POPUP_LAYER);
        layers.repaint();

        // Slide in, hold, then fade out.
        Timer in = new Timer(Theme.Motion.TICK_MS, null);
        final float[] progress = {0f};
        in.addActionListener(e -> {
            progress[0] = Theme.Motion.approach(progress[0], 1f, Theme.Motion.FAST);
            float eased = Theme.Motion.easeOut(progress[0]);
            bubble.setLocation(x, Math.round(startY + (endY - startY) * eased));
            bubble.setOpacity(eased);
            if (progress[0] == 1f) in.stop();
        });
        in.start();

        Timer hold = new Timer(VISIBLE_MS, e -> {
            Timer out = new Timer(Theme.Motion.TICK_MS, null);
            final float[] fade = {1f};
            out.addActionListener(ev -> {
                fade[0] = Theme.Motion.approach(fade[0], 0f, Theme.Motion.FAST);
                bubble.setOpacity(fade[0]);
                if (fade[0] == 0f) {
                    out.stop();
                    layers.remove(bubble);
                    layers.repaint();
                }
            });
            out.start();
        });
        hold.setRepeats(false);
        hold.start();
    }

    private static class Bubble extends JComponent {
        private final String message;
        private final Kind kind;
        private float opacity = 0f;

        Bubble(String message, Kind kind) {
            this.message = message;
            this.kind = kind;
            setOpaque(false);
            FontMetrics fm = getFontMetrics(Theme.BODY_MEDIUM);
            int width = Math.min(560, fm.stringWidth(message) + 76);
            setPreferredSize(new Dimension(width, 54));
        }

        void setOpacity(float o) {
            this.opacity = Math.max(0f, Math.min(1f, o));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            int w = getWidth();
            int h = getHeight();
            int a = Math.round(255 * opacity);

            Theme.shadow(g, 4, 4, w - 8, h - 8, Theme.PILL, 10, 5, Math.round(50 * opacity));
            Theme.fillRound(g, 4, 4, w - 8, h - 8, Theme.PILL,
                    new Color(0x1A, 0x22, 0x2E, a));

            Color mark = switch (kind) {
                case SUCCESS -> Theme.SUCCESS;
                case ERROR -> Theme.DANGER;
                default -> Theme.ACCENT;
            };
            Icons.Kind icon = switch (kind) {
                case SUCCESS -> Icons.Kind.CHECK;
                case ERROR -> Icons.Kind.INFO;
                default -> Icons.Kind.INFO;
            };
            g.setColor(Theme.alpha(mark, Math.round(46 * opacity)));
            g.fillOval(18, h / 2 - 14, 28, 28);
            Icons.paint(g, icon, 25, h / 2 - 7, 14, Theme.alpha(mark, a));

            g.setFont(Theme.BODY_MEDIUM);
            FontMetrics fm = g.getFontMetrics();
            g.setColor(new Color(255, 255, 255, a));
            g.drawString(BookCover.ellipsise(message, fm, w - 84), 56,
                    (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }
}
