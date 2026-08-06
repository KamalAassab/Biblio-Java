import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** An on/off switch with a knob that slides between states. */
public class TogglePill extends JComponent {

    private boolean selected;
    private final Runnable onChange;

    private float t;
    private final Timer animator;

    private static final int W = 56;
    private static final int H = 32;

    public TogglePill(boolean selected, Runnable onChange) {
        this.selected = selected;
        this.onChange = onChange;
        this.t = selected ? 1f : 0f;

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension d = new Dimension(W, H);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);

        animator = new Timer(Theme.Motion.TICK_MS, e -> {
            float target = this.selected ? 1f : 0f;
            t = Theme.Motion.approach(t, target, Theme.Motion.FAST);
            if (t == target) ((Timer) e.getSource()).stop();
            repaint();
        });
        animator.setCoalesce(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!contains(e.getPoint())) return;
                setSelected(!TogglePill.this.selected);
                if (onChange != null) onChange.run();
            }
        });
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean b) {
        if (selected == b) return;
        selected = b;
        if (!animator.isRunning()) animator.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);

        float p = Theme.Motion.easeOut(t);
        Color track = Theme.mix(Theme.BORDER, Theme.SUCCESS, p);
        Theme.fillRound(g, 0, 0, W, H, Theme.PILL, track);

        int knob = H - 8;
        int travel = W - knob - 8;
        int kx = 4 + Math.round(travel * p);

        g.setColor(Theme.alpha(Color.BLACK, 30));
        g.fillOval(kx, 5, knob, knob);
        g.setColor(Color.WHITE);
        g.fillOval(kx, 4, knob, knob);

        g.dispose();
    }
}
