import javax.swing.JButton;
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
 * A pill-shaped button.
 *
 * <p>{@code PRIMARY} is the institutional blue and carries the single most important
 * action on a screen. {@code SECONDARY} is an outlined neutral, {@code GHOST} is
 * chrome-only, {@code ACCENT} uses academic gold for emphasis without implying
 * submission, and {@code DANGER} is reserved for destructive confirmations.
 */
public class RoundedButton extends JButton {

    public enum Style { PRIMARY, SECONDARY, GHOST, DANGER, ACCENT }

    private final Style style;
    private Color customHover;
    private Icons.Kind icon;
    private int radius = Theme.PILL;
    private boolean loading;

    private boolean hovered;
    private boolean pressed;
    private float t;
    private final Timer animator;

    public RoundedButton(String text, Style style) {
        super(text);
        this.style = style;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.BODY_BOLD);
        setPreferredSize(new Dimension(getPreferredSize().width, 46));

        animator = new Timer(Theme.Motion.TICK_MS, e -> {
            float target = hovered ? 1f : 0f;
            t = Theme.Motion.approach(t, target, Theme.Motion.FAST);
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
                pressed = false;
                restart();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });
    }

    public RoundedButton setCustomHover(Color c) {
        this.customHover = c;
        return this;
    }

    public RoundedButton withIcon(Icons.Kind kind) {
        this.icon = kind;
        return this;
    }

    public RoundedButton withRadius(int r) {
        this.radius = r;
        return this;
    }

    /** Shows a spinner and blocks interaction — used while a database call is in flight. */
    public void setLoading(boolean b) {
        this.loading = b;
        setEnabled(!b);
        setCursor(Cursor.getPredefinedCursor(b ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
        if (b) spinner.start();
        else spinner.stop();
        repaint();
    }

    private final Timer spinner = new Timer(Theme.Motion.TICK_MS, e -> repaint());

    private void restart() {
        if (!animator.isRunning()) animator.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);

        int w = getWidth();
        int h = getHeight();
        float p = Theme.Motion.easeOut(t);
        int dy = pressed ? 1 : 0;
        boolean on = isEnabled();

        Color fill;
        Color textColor;
        Color border = null;

        switch (style) {
            case PRIMARY -> {
                fill = Theme.mix(Theme.PRIMARY, Theme.PRIMARY_2, p * 0.85f);
                textColor = Theme.ON_PRIMARY;
            }
            case ACCENT -> {
                fill = Theme.mix(Theme.ACCENT, Theme.ACCENT_2, p);
                textColor = new Color(0x3A, 0x2A, 0x00);
            }
            case DANGER -> {
                fill = Theme.mix(Theme.DANGER, new Color(0xF0, 0x6A, 0x5E), p);
                textColor = Color.WHITE;
            }
            case SECONDARY -> {
                fill = Theme.mix(Theme.SURFACE, Theme.HOVER, p);
                textColor = Theme.TEXT;
                border = Theme.mix(Theme.BORDER, Theme.PRIMARY_LIGHT, p * 0.6f);
            }
            default -> {
                Color hoverTint = customHover != null ? customHover : Theme.HOVER;
                fill = Theme.mix(Theme.alpha(hoverTint, 0), hoverTint, p);
                textColor = customHover != null && p > 0.5f ? Color.WHITE : Theme.TEXT_SOFT;
            }
        }

        if (!on) {
            fill = Theme.mix(fill, Theme.SURFACE_CHIP, 0.6f);
            textColor = Theme.mix(textColor, Theme.FAINT, 0.6f);
        }

        // Primary actions get a shadow so they read as the raised element on the screen.
        if ((style == Style.PRIMARY || style == Style.DANGER || style == Style.ACCENT) && on) {
            Theme.shadow(g, 2, 2 + dy, w - 4, h - 4, radius,
                    Math.round(6 + 4 * p), 3, Math.round(30 + 16 * p));
        }

        Theme.fillRound(g, 2, 2 + dy, w - 4, h - 4, radius, fill);
        if (border != null) {
            g.setColor(border);
            int d = Math.min(radius * 2, Math.min(w - 4, h - 4));
            g.drawRoundRect(2, 2 + dy, w - 5, h - 5, d, d);
        }

        if (loading) {
            paintSpinner(g, w / 2, h / 2 + dy, 9, textColor);
            g.dispose();
            return;
        }

        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        String text = getText() == null ? "" : getText();
        int textWidth = fm.stringWidth(text);
        int iconSize = icon != null ? 17 : 0;
        int gap = icon != null && !text.isEmpty() ? 9 : 0;
        int totalWidth = iconSize + gap + textWidth;
        int startX = (w - totalWidth) / 2;
        int baseline = (h - fm.getHeight()) / 2 + fm.getAscent() + dy;

        if (icon != null) {
            Icons.paint(g, icon, startX, (h - iconSize) / 2 + dy, iconSize, textColor);
        }
        g.setColor(textColor);
        g.drawString(text, startX + iconSize + gap, baseline);

        g.dispose();
    }

    private void paintSpinner(Graphics2D g, int cx, int cy, int r, Color c) {
        double angle = (System.currentTimeMillis() % 900) / 900.0 * 360.0;
        g.setStroke(new java.awt.BasicStroke(2.4f, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g.setColor(Theme.alpha(c, 70));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(c);
        g.drawArc(cx - r, cy - r, r * 2, r * 2, (int) -angle, 100);
    }
}
