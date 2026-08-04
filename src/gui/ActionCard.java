import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ActionCard extends JPanel {
    private final Color c1;
    private final Color c2;
    private final Icons.Kind icon;
    private final String title;
    private final String sub;
    private final Runnable action;
    private float t;
    private boolean over;
    private Timer anim;

    public ActionCard(Color c1, Color c2, Icons.Kind icon, String title, String sub, Runnable action) {
        this.c1 = c1;
        this.c2 = c2;
        this.icon = icon;
        this.title = title;
        this.sub = sub;
        this.action = action;
        setOpaque(false);
        setPreferredSize(new Dimension(320, 132));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                over = true;
                start();
            }

            public void mouseExited(MouseEvent e) {
                over = false;
                start();
            }

            public void mouseClicked(MouseEvent e) {
                if (action != null) action.run();
            }
        });
        anim = new Timer(16, e -> {
            float target = over ? 1f : 0f;
            t += (target - t) * 0.22f;
            if (Math.abs(target - t) < 0.01f) {
                t = target;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
    }

    private void start() {
        if (!anim.isRunning()) anim.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int r = 20;
        int bx = 2, by = 4, bw = w - 6, bh = h - 8;

        g.setColor(new Color(15, 23, 42, 24));
        g.fillRoundRect(bx + 3, by + 5, bw, bh, r * 2, r * 2);
        Theme.gradientRound(g, bx, by, bw, bh, r, c1, c2);

        if (t > 0) {
            g.setColor(new Color(255, 255, 255, Math.round(30 * t)));
            g.fillRoundRect(bx, by, bw, bh, r * 2, r * 2);
        }

        int iconBox = 54;
        int ibx = bx + 18, iby = by + (bh - iconBox) / 2;
        g.setColor(new Color(255, 255, 255, 45));
        g.fillRoundRect(ibx, iby, iconBox, iconBox, 16, 16);
        Icons.paint(g, icon, ibx + (iconBox - 24) / 2, iby + (iconBox - 24) / 2, 24, Color.WHITE);

        int tx = ibx + iconBox + 20;
        g.setFont(Theme.H2);
        g.setColor(Color.WHITE);
        g.drawString(title, tx, by + bh / 2 - 4);
        g.setFont(Theme.SMALL);
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString(sub, tx, by + bh / 2 + 18);

        g.setColor(new Color(255, 255, 255, 200));
        Icons.paint(g, Icons.Kind.ARROW, bx + bw - 38, by + (bh - 20) / 2, 20, g.getColor());
        g.dispose();
    }
}
