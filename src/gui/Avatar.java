import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class Avatar extends JComponent {
    private final String initials;
    private final int size;
    private final Color c1;
    private final Color c2;

    public Avatar(String initials, int size) {
        this(initials, size, Theme.PRIMARY, Theme.PRIMARY_2);
    }

    public Avatar(String initials, int size, Color c1, Color c2) {
        this.initials = initials;
        this.size = size;
        this.c1 = c1;
        this.c2 = c2;
        setPreferredSize(new Dimension(size, size));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int s = Math.min(getWidth(), getHeight());
        Theme.gradientRound(g, 0, 0, s - 1, s - 1, s / 2, c1, c2);
        g.setColor(Color.WHITE);
        g.setFont(Theme.FONT_BOLD.deriveFont(Font.BOLD, Math.round(size * 0.34f)));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(initials);
        int x = (s - tw) / 2;
        int y = (s - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(initials, x, y);
        g.dispose();
    }
}
