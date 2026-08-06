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
 * A selectable filter pill, used for the category row above the catalogue.
 *
 * <p>Also serves as a read-only status badge when constructed with explicit colours
 * and no click handler.
 */
public class Chip extends JComponent {

    private String text;
    private boolean selected;
    private Runnable action;
    private Color staticFill;
    private Color staticText;
    private Integer count;

    private boolean hovered;
    private float t;
    private final Timer animator;

    public Chip(String text) {
        this(text, null, null);
    }

    /** Static badge form: fixed colours, not interactive. */
    public Chip(String text, Color fill, Color textColor) {
        this.text = text == null ? "" : text;
        this.staticFill = fill;
        this.staticText = textColor;
        setOpaque(false);
        setFont(Theme.SMALL_BOLD);

        animator = new Timer(Theme.Motion.TICK_MS, e -> {
            float target = (hovered || selected) ? 1f : 0f;
            t = Theme.Motion.approach(t, target, Theme.Motion.FAST);
            if (t == target) ((Timer) e.getSource()).stop();
            repaint();
        });
        animator.setCoalesce(true);

        if (fill == null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
        updateSize();
    }

    public Chip onClick(Runnable r) {
        this.action = r;
        return this;
    }

    public Chip withCount(int n) {
        this.count = n;
        updateSize();
        return this;
    }

    public void setText(String s) {
        this.text = s == null ? "" : s;
        updateSize();
        repaint();
    }

    public void setSelected(boolean b) {
        if (selected == b) return;
        selected = b;
        restart();
    }

    public boolean isSelected() {
        return selected;
    }

    private void restart() {
        if (!animator.isRunning()) animator.start();
    }

    private void updateSize() {
        FontMetrics fm = getFontMetrics(Theme.SMALL_BOLD);
        int w = fm.stringWidth(text) + 34;
        if (count != null) w += fm.stringWidth(String.valueOf(count)) + 16;
        Dimension d = new Dimension(w, 38);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);

        int w = getWidth();
        int h = getHeight();
        float p = Theme.Motion.easeOut(t);

        Color fill;
        Color textColor;
        if (staticFill != null) {
            fill = staticFill;
            textColor = staticText != null ? staticText : Theme.TEXT;
        } else if (selected) {
            fill = Theme.PRIMARY;
            textColor = Color.WHITE;
        } else {
            fill = Theme.mix(Theme.SURFACE_CHIP, Theme.PRIMARY_SOFT, p);
            textColor = Theme.mix(Theme.MUTED, Theme.PRIMARY, p);
        }

        Theme.fillRound(g, 0, 0, w, h, Theme.PILL, fill);

        g.setFont(Theme.SMALL_BOLD);
        FontMetrics fm = g.getFontMetrics();
        int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();
        g.setColor(textColor);
        g.drawString(text, 17, baseline);

        if (count != null) {
            int cx = 17 + fm.stringWidth(text) + 8;
            String n = String.valueOf(count);
            int cw = fm.stringWidth(n) + 14;
            g.setColor(selected ? Theme.alpha(Color.WHITE, 55) : Theme.alpha(Theme.MUTED, 38));
            g.fillRoundRect(cx, (h - 20) / 2, cw, 20, 20, 20);
            g.setColor(textColor);
            g.drawString(n, cx + 7, baseline);
        }

        g.dispose();
    }

    // ── Preset badges ────────────────────────────────────────────────────────

    public static Chip success(String text) {
        return new Chip(text, Theme.SUCCESS_SOFT, new Color(0x11, 0x6B, 0x3D));
    }

    public static Chip danger(String text) {
        return new Chip(text, Theme.DANGER_SOFT, new Color(0xA3, 0x2C, 0x24));
    }

    public static Chip warning(String text) {
        return new Chip(text, Theme.AMBER_SOFT, new Color(0x92, 0x51, 0x05));
    }

    public static Chip neutral(String text) {
        return new Chip(text, Theme.SURFACE_CHIP, Theme.MUTED);
    }
}
