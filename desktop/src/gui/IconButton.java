import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class IconButton extends JButton {
    private final Icons.Kind icon;
    private final int iconSize;
    private float t;
    private boolean over;
    private Timer anim;
    private Color iconColor = Theme.MUTED;
    private Color hoverColor;

    public IconButton(Icons.Kind icon, int iconSize) {
        this.icon = icon;
        this.iconSize = iconSize;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(iconSize + 18, iconSize + 18));
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
                start();
            }
        });
    }

    public IconButton withColors(Color normal, Color hoverFill, Color hoverIcon) {
        this.iconColor = normal;
        this.hoverColor = hoverFill;
        setIconColorOnHover(hoverIcon);
        return this;
    }

    private void setIconColorOnHover(Color c) {
        // used by paint
        this.hoverIconColor = c;
    }

    private Color hoverIconColor;

    private void start() {
        if (!anim.isRunning()) anim.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        if (t > 0) {
            Color fill = hoverColor != null ? hoverColor : new Color(17, 24, 39);
            g.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), Math.round(30 * t)));
            g.fillOval(0, 0, w - 1, h - 1);
        }
        int x = (w - iconSize) / 2, y = (h - iconSize) / 2;
        Color c = over ? (hoverIconColor != null ? hoverIconColor : iconColor) : iconColor;
        Icons.paint(g, icon, x, y, iconSize, c);
        g.dispose();
    }
}
