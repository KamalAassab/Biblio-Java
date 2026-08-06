import javax.swing.JPasswordField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A rounded password input matching {@link RoundedTextField}, with a reveal toggle.
 *
 * <p>The toggle exists because forced masking pushes people toward shorter, weaker
 * passwords; letting them verify what they typed is the safer trade-off on a desktop
 * client used at a library desk.
 */
public class RoundedPasswordField extends JPasswordField {

    private final String placeholder;
    private boolean revealed;
    private boolean error;

    private float focus;
    private final Timer animator;

    private static final int TOGGLE_WIDTH = 46;

    public RoundedPasswordField(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;

        setOpaque(false);
        setBorder(new EmptyBorder(0, 46, 0, TOGGLE_WIDTH));
        setFont(Theme.BODY);
        setForeground(Theme.TEXT);
        setCaretColor(Theme.PRIMARY);
        setEchoChar('•');
        setPreferredSize(new Dimension(240, 52));

        animator = new Timer(Theme.Motion.TICK_MS, e -> {
            float target = hasFocus() ? 1f : 0f;
            focus = Theme.Motion.approach(focus, target, Theme.Motion.FAST);
            if (focus == target) ((Timer) e.getSource()).stop();
            repaint();
        });
        animator.setCoalesce(true);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                error = false;
                restart();
            }

            @Override
            public void focusLost(FocusEvent e) {
                restart();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getX() >= getWidth() - TOGGLE_WIDTH) {
                    revealed = !revealed;
                    setEchoChar(revealed ? (char) 0 : '•');
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                setCursor(Cursor.getPredefinedCursor(
                        e.getX() >= getWidth() - TOGGLE_WIDTH
                                ? Cursor.HAND_CURSOR : Cursor.TEXT_CURSOR));
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setCursor(Cursor.getPredefinedCursor(
                        e.getX() >= getWidth() - TOGGLE_WIDTH
                                ? Cursor.HAND_CURSOR : Cursor.TEXT_CURSOR));
            }
        });
    }

    public void markError() {
        this.error = true;
        repaint();
    }

    private void restart() {
        if (!animator.isRunning()) animator.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);

        int w = getWidth();
        int h = getHeight();
        float p = Theme.Motion.easeOut(focus);

        Color fill = error ? Theme.DANGER_SOFT : Theme.mix(Theme.FIELD, Theme.SURFACE, p);
        Theme.fillRound(g, 0, 0, w, h, Theme.RADIUS_SM, fill);

        Color ring = error ? Theme.DANGER : Theme.mix(Theme.BORDER, Theme.PRIMARY, p);
        g.setColor(ring);
        g.setStroke(new java.awt.BasicStroke(error || p > 0.5f ? 1.6f : 1f));
        g.drawRoundRect(0, 0, w - 1, h - 1, Theme.RADIUS_SM * 2, Theme.RADIUS_SM * 2);

        Icons.paint(g, Icons.Kind.LOCK, 16, (h - 18) / 2, 18,
                error ? Theme.DANGER : Theme.mix(Theme.MUTED, Theme.PRIMARY, p));

        if (getPassword().length == 0 && !placeholder.isEmpty()) {
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Theme.FAINT);
            g.drawString(placeholder, getInsets().left, (h - fm.getHeight()) / 2 + fm.getAscent());
        }

        // Reveal toggle: an eye that gains a slash when the value is visible.
        int cx = w - TOGGLE_WIDTH / 2;
        int cy = h / 2;
        g.setColor(revealed ? Theme.PRIMARY : Theme.MUTED);
        g.setStroke(new java.awt.BasicStroke(1.6f, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g.drawArc(cx - 10, cy - 7, 20, 14, 0, 180);
        g.drawArc(cx - 10, cy - 7, 20, 14, 180, 180);
        g.fillOval(cx - 3, cy - 3, 6, 6);
        if (revealed) {
            g.drawLine(cx - 9, cy + 7, cx + 9, cy - 7);
        }

        g.dispose();
        super.paintComponent(g0);
    }
}
