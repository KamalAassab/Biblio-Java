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

public class Chip extends JButton {
    private boolean on;
    private float t;
    private boolean over;
    private Timer anim;

    public Chip(String text) {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.SMALL_BOLD);
        setForeground(Theme.TEXT);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(9, 18, 9, 18));
        anim = new Timer(16, e -> {
            float target = on || over ? 1f : 0f;
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

    public void setOn(boolean b) {
        on = b;
        setForeground(on ? Color.WHITE : Theme.TEXT);
        if (!anim.isRunning()) anim.start();
        repaint();
    }

    public boolean isOn() {
        return on;
    }

    private void start() {
        if (!anim.isRunning()) anim.start();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, 38);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        if (on) {
            Theme.gradientRound(g, 0, 0, w - 1, h - 1, h / 2, Theme.PRIMARY, Theme.PRIMARY_2);
        } else {
            g.setColor(Theme.mix(new Color(236, 238, 248), new Color(225, 227, 243), t));
            g.fillRoundRect(0, 0, w - 1, h - 1, h / 2, h / 2);
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
