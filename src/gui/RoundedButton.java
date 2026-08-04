import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RoundedButton extends JButton {
    public enum Style { PRIMARY, SECONDARY, GHOST, DANGER }

    private final Style style;
    private float t;
    private boolean over;
    private boolean down;
    private Timer anim;
    private Color customHover;

    public RoundedButton(String text, Style style) {
        super(text);
        this.style = style;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.FONT_BOLD);
        setForeground(colorFor(style));
        setBorder(BorderFactory.createEmptyBorder(12, 22, 12, 22));
        anim = new Timer(16, e -> {
            float target = over ? 1f : 0f;
            t += (target - t) * 0.25f;
            if (Math.abs(target - t) < 0.01f) {
                t = target;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                over = true;
                start();
            }

            public void mouseExited(MouseEvent e) {
                over = false;
                down = false;
                start();
            }

            public void mousePressed(MouseEvent e) {
                down = true;
                repaint();
            }

            public void mouseReleased(MouseEvent e) {
                down = false;
                repaint();
            }
        });
    }

    public RoundedButton setCustomHover(Color c) {
        customHover = c;
        return this;
    }

    private Color colorFor(Style s) {
        switch (s) {
            case PRIMARY:
            case DANGER:
                return Color.WHITE;
            case SECONDARY:
                return Theme.PRIMARY;
            default:
                return Theme.TEXT;
        }
    }

    private void start() {
        if (!anim.isRunning()) anim.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int r = Math.min(Theme.RADIUS, h / 2);
        if (w <= 0 || h <= 0) {
            g.dispose();
            return;
        }
        switch (style) {
            case PRIMARY: {
                Color c1 = Theme.mix(Theme.PRIMARY, Theme.PRIMARY_2, 0.45f);
                Color c2 = Theme.PRIMARY_2;
                Theme.gradientRound(g, 0, 0, w - 1, h - 1, r, c1, c2);
                if (t > 0) {
                    g.setColor(new Color(255, 255, 255, Math.round(26 * t)));
                    g.fillRoundRect(0, 0, w - 1, h - 1, r * 2, r * 2);
                }
                if (down) {
                    g.setColor(new Color(0, 0, 0, 46));
                    g.fillRoundRect(0, 0, w - 1, h - 1, r * 2, r * 2);
                }
                break;
            }
            case SECONDARY: {
                g.setColor(Theme.mix(new Color(238, 240, 253), new Color(224, 226, 253), t));
                g.fillRoundRect(0, 0, w - 1, h - 1, r * 2, r * 2);
                if (down) {
                    g.setColor(new Color(0, 0, 0, 26));
                    g.fillRoundRect(0, 0, w - 1, h - 1, r * 2, r * 2);
                }
                break;
            }
            case GHOST: {
                Color hv = customHover != null ? customHover : new Color(17, 24, 39);
                if (t > 0) {
                    g.setColor(new Color(hv.getRed(), hv.getGreen(), hv.getBlue(), Math.round(26 * t)));
                    g.fillRoundRect(0, 0, w - 1, h - 1, r * 2, r * 2);
                }
                break;
            }
            case DANGER: {
                g.setColor(Theme.mix(Theme.DANGER, new Color(220, 38, 38), t));
                g.fillRoundRect(0, 0, w - 1, h - 1, r * 2, r * 2);
                if (down) {
                    g.setColor(new Color(0, 0, 0, 46));
                    g.fillRoundRect(0, 0, w - 1, h - 1, r * 2, r * 2);
                }
                break;
            }
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
