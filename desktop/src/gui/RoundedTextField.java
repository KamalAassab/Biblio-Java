import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * A rounded text input with a placeholder and an animated focus ring.
 *
 * <p>Also renders an inline error state, so validation feedback appears against the
 * offending field rather than only in a toast the user may have already dismissed.
 */
public class RoundedTextField extends JTextField {

    private final String placeholder;
    private Icons.Kind icon;
    private boolean error;

    private float focus;
    private final Timer animator;

    public RoundedTextField(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;

        setOpaque(false);
        setBorder(new EmptyBorder(0, 18, 0, 16));
        setFont(Theme.BODY);
        setForeground(Theme.TEXT);
        setCaretColor(Theme.PRIMARY);
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
    }

    public RoundedTextField withIcon(Icons.Kind kind) {
        this.icon = kind;
        setBorder(new EmptyBorder(0, 46, 0, 16));
        return this;
    }

    /** Marks the field as invalid until it is next focused. */
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

        if (icon != null) {
            Icons.paint(g, icon, 16, (h - 18) / 2, 18,
                    error ? Theme.DANGER : Theme.mix(Theme.MUTED, Theme.PRIMARY, p));
        }

        if (getText().isEmpty() && !placeholder.isEmpty()) {
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Theme.FAINT);
            g.drawString(placeholder, getInsets().left,
                    (h - fm.getHeight()) / 2 + fm.getAscent());
        }

        g.dispose();
        super.paintComponent(g0);
    }
}
