import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class RoundedPanel extends JPanel {
    protected int radius;
    protected Color bg;
    protected Color border;
    protected int borderWidth = 1;
    protected Color[] gradient;

    public RoundedPanel(Color bg) {
        this(bg, Theme.RADIUS);
    }

    public RoundedPanel(Color bg, int radius) {
        this.bg = bg;
        this.radius = radius;
        setOpaque(false);
    }

    public RoundedPanel setBg(Color c) {
        bg = c;
        repaint();
        return this;
    }

    public RoundedPanel setGradient(Color a, Color b) {
        gradient = new Color[]{a, b};
        repaint();
        return this;
    }

    public RoundedPanel setBorderColor(Color c) {
        border = c;
        repaint();
        return this;
    }

    public RoundedPanel setBorderColor(Color c, int w) {
        border = c;
        borderWidth = w;
        repaint();
        return this;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) {
            g.dispose();
            return;
        }
        if (gradient != null) {
            g.setPaint(new GradientPaint(0, 0, gradient[0], w, h, gradient[1], true));
        } else {
            g.setColor(bg);
        }
        g.fillRoundRect(0, 0, w - 1, h - 1, radius * 2, radius * 2);
        if (border != null) {
            g.setStroke(new BasicStroke(borderWidth));
            g.setColor(border);
            g.drawRoundRect(0, 0, w - 1, h - 1, radius * 2, radius * 2);
        }
        g.dispose();
    }
}
