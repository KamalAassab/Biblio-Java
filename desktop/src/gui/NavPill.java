import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A sidebar navigation row: a rounded icon chip followed by a label.
 *
 * <p>Selection is carried entirely by the chip — it fills with academic gold and the
 * icon flips to white, while the label darkens and gains weight. There is no pill
 * behind the whole row, which keeps a five-item sidebar from turning into a stack
 * of competing blocks.
 */
public class NavPill extends JComponent {

    private final Icons.Kind icon;
    private String label;
    private final Runnable action;
    private final boolean danger;

    private boolean selected;
    private boolean hovered;
    private float t;
    private final Timer animator;

    private static final int HEIGHT = 52;
    private static final int CHIP = 38;
    private static final int CHIP_X = 8;
    private static final int LABEL_X = CHIP_X + CHIP + 14;

    public NavPill(Icons.Kind icon, String label, Runnable action) {
        this(icon, label, action, false);
    }

    public NavPill(Icons.Kind icon, String label, Runnable action, boolean danger) {
        this.icon = icon;
        this.label = label;
        this.action = action;
        this.danger = danger;

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH - 32, HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
        setMinimumSize(new Dimension(120, HEIGHT));

        animator = new Timer(Theme.Motion.TICK_MS, e -> {
            float target = (selected || hovered) ? 1f : 0f;
            t = Theme.Motion.approach(t, target, Theme.Motion.NORMAL);
            if (t == target) ((Timer) e.getSource()).stop();
            repaint();
        });
        animator.setCoalesce(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                restart();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                restart();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (contains(e.getPoint()) && action != null) action.run();
            }
        });
    }

    public void setSelected(boolean b) {
        if (selected == b) return;
        selected = b;
        restart();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setLabel(String text) {
        this.label = text;
        repaint();
    }

    private void restart() {
        if (!animator.isRunning()) animator.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);

        float p = Theme.Motion.easeOut(t);
        int h = getHeight();
        int chipY = (h - CHIP) / 2;

        Color accent = danger ? Theme.DANGER : Theme.ACCENT;

        // Hover wash behind the whole row, kept very light so it never competes with selection.
        if (hovered && !selected) {
            Theme.fillRound(g, 0, 4, getWidth(), h - 8, Theme.RADIUS_SM,
                    Theme.alpha(danger ? Theme.DANGER : Theme.PRIMARY, Math.round(12 * p)));
        }

        Color chipFill;
        Color iconColor;
        if (selected) {
            chipFill = accent;
            iconColor = danger ? Color.WHITE : Color.WHITE;
        } else {
            chipFill = Theme.mix(Theme.SURFACE_CHIP,
                    danger ? Theme.DANGER_SOFT : Theme.PRIMARY_SOFT, p * 0.7f);
            // Destructive actions sit quiet until pointed at, so the sidebar does not
            // read as if something is already wrong.
            iconColor = Theme.mix(Theme.MUTED, danger ? Theme.DANGER : Theme.PRIMARY, p);
        }

        if (selected) {
            // A soft glow under the active chip, so the gold reads as lit rather than flat.
            g.setColor(Theme.alpha(accent, 46));
            g.fillRoundRect(CHIP_X - 2, chipY + 3, CHIP + 4, CHIP + 2, 18, 18);
        }
        Theme.fillRound(g, CHIP_X, chipY, CHIP, CHIP, 13, chipFill);

        int iconSize = 19;
        Icons.paint(g, icon, CHIP_X + (CHIP - iconSize) / 2, chipY + (CHIP - iconSize) / 2,
                iconSize, iconColor);

        g.setFont(selected ? Theme.BODY_BOLD : Theme.BODY_MEDIUM);
        Color textColor;
        if (danger) {
            textColor = Theme.mix(Theme.MUTED, Theme.DANGER, Math.max(p, selected ? 1f : 0f));
        } else {
            textColor = selected ? Theme.TEXT : Theme.mix(Theme.MUTED, Theme.TEXT, p * 0.8f);
        }
        g.setColor(textColor);
        FontMetrics fm = g.getFontMetrics();
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(label, LABEL_X, ty);

        g.dispose();
    }
}
