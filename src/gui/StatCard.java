import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class StatCard extends JPanel {
    private final String label;
    private int value;
    private final Color accent;
    private final Icons.Kind icon;

    public StatCard(String label, Color accent, Icons.Kind icon) {
        this.label = label;
        this.accent = accent;
        this.icon = icon;
        setOpaque(false);
        setPreferredSize(new Dimension(210, 108));
    }

    public void setValue(int v) {
        value = v;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int r = 14;
        int bx = 2, by = 4, bw = w - 6, bh = h - 8;

        g.setColor(new Color(15, 23, 42, 18));
        g.fillRoundRect(bx + 2, by + 3, bw, bh, r * 2, r * 2);
        Theme.fillRound(g, bx, by, bw, bh, r, Color.WHITE);

        int chip = 36;
        int cx = bx + 14, cy = by + 12;
        Theme.gradientRound(g, cx, cy, chip, chip, 10, accent, Theme.mix(accent, Color.WHITE, 0.30f));
        Icons.paint(g, icon, cx + (chip - 18) / 2, cy + (chip - 18) / 2, 18, Color.WHITE);

        g.setFont(Theme.BOLD_24);
        g.setColor(Theme.TEXT);
        FontMetrics fm = g.getFontMetrics();
        int vx = bx + 14;
        int vy = cy + chip + 14;
        g.drawString(String.valueOf(value), vx, vy);
        g.setFont(Theme.SMALL_BOLD);
        g.setColor(Theme.MUTED);
        g.drawString(label, vx, vy + 18);
        g.dispose();
    }
}
