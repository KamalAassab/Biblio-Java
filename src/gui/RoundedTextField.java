import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class RoundedTextField extends JTextField {
    private final String placeholder;
    private float t;
    private boolean focused;
    private Timer anim;
    private final int corner;

    public RoundedTextField(String placeholder) {
        this(placeholder, Theme.RADIUS);
    }

    public RoundedTextField(String placeholder, int corner) {
        this.placeholder = placeholder;
        this.corner = corner;
        setOpaque(false);
        setFont(Theme.FONT);
        setForeground(Theme.TEXT);
        setCaretColor(Theme.PRIMARY);
        setSelectionColor(new Color(210, 213, 252));
        setBorder(new EmptyBorder(12, 16, 12, 16));
        anim = new Timer(16, e -> {
            float target = focused ? 1f : 0f;
            t += (target - t) * 0.22f;
            if (Math.abs(target - t) < 0.01f) {
                t = target;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                focused = true;
                start();
            }

            public void focusLost(FocusEvent e) {
                focused = false;
                start();
            }
        });
    }

    private void start() {
        if (!anim.isRunning()) anim.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        if (w > 0 && h > 0) {
            g.setColor(Theme.mix(Theme.FIELD, new Color(240, 241, 255), t));
            g.fillRoundRect(0, 0, w - 1, h - 1, corner * 2, corner * 2);
            g.setStroke(new BasicStroke(1.4f));
            g.setColor(Theme.mix(new Color(225, 228, 240), Theme.PRIMARY, t));
            g.drawRoundRect(0, 0, w - 1, h - 1, corner * 2, corner * 2);
        }
        g.dispose();
        super.paintComponent(g0);
        if (getText().isEmpty() && placeholder != null) {
            Graphics2D g2 = (Graphics2D) g0.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(Theme.FONT);
            g2.setColor(Theme.MUTED);
            FontMetrics fm = g2.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(placeholder, getInsets().left, y);
            g2.dispose();
        }
    }
}
