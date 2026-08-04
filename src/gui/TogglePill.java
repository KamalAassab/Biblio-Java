import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TogglePill extends JComponent {
    private boolean on;
    private float t;
    private Timer anim;
    private final Runnable onChange;

    public TogglePill(boolean initial, Runnable onChange) {
        this.on = initial;
        this.onChange = onChange;
        t = on ? 1f : 0f;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                setOn(!on);
                if (onChange != null) onChange.run();
            }
        });
        anim = new Timer(16, e -> {
            float target = on ? 1f : 0f;
            t += (target - t) * 0.22f;
            if (Math.abs(target - t) < 0.01f) {
                t = target;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
    }

    public void setOn(boolean b) {
        on = b;
        if (!anim.isRunning()) anim.start();
        repaint();
    }

    public boolean isOn() {
        return on;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(64, 34);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        Color track = Theme.mix(new Color(214, 218, 230), Theme.PRIMARY, t);
        Theme.fillRound(g, 0, 0, w - 1, h - 1, h / 2, track);
        int knob = h - 8;
        int maxX = w - knob - 4;
        int kx = 4 + Math.round((maxX - 4) * t);
        int ky = 4;
        g.setColor(Color.WHITE);
        g.fillOval(kx, ky, knob, knob);
        g.dispose();
    }
}
