import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A white, heavily rounded surface that floats above the ivory canvas.
 *
 * <p>The base building block of the layout. When given a click action it becomes
 * interactive: the shadow deepens and the whole card lifts a couple of pixels on
 * hover, so "this is pressable" is communicated by elevation rather than by a border.
 */
public class Card extends JPanel {

    private final int radius;
    private Color fill;
    private boolean interactive;
    private Runnable action;

    private boolean hovered;
    private boolean pressed;
    private float lift;
    private final Timer animator;

    /** Maximum vertical travel on hover, in pixels. */
    private static final float LIFT_PX = 3f;

    public Card() {
        this(Theme.RADIUS_LG, Theme.SURFACE);
    }

    public Card(int radius) {
        this(radius, Theme.SURFACE);
    }

    public Card(int radius, Color fill) {
        this.radius = radius;
        this.fill = fill;
        setOpaque(false);

        animator = new Timer(Theme.Motion.TICK_MS, e -> {
            float target = hovered ? 1f : 0f;
            lift = Theme.Motion.approach(lift, target, Theme.Motion.NORMAL);
            if (lift == target) ((Timer) e.getSource()).stop();
            repaint();
        });
        animator.setCoalesce(true);
    }

    /** Makes the card clickable, with hover elevation and a pressed state. */
    public Card onClick(Runnable action) {
        this.action = action;
        this.interactive = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                restart();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                pressed = false;
                restart();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                boolean wasPressed = pressed;
                pressed = false;
                repaint();
                if (wasPressed && contains(e.getPoint()) && Card.this.action != null) {
                    Card.this.action.run();
                }
            }
        });
        return this;
    }

    public Card withFill(Color c) {
        this.fill = c;
        repaint();
        return this;
    }

    protected boolean isHovered() {
        return hovered;
    }

    /** Progress of the hover animation, 0 at rest and 1 fully lifted. */
    protected float hoverProgress() {
        return Theme.Motion.easeOut(lift);
    }

    private void restart() {
        if (!animator.isRunning()) animator.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);

        float p = hoverProgress();
        // Shrink the painted area to leave room for the shadow, then translate upward on hover.
        int inset = 8;
        int dy = Math.round(-LIFT_PX * p) + (pressed ? 1 : 0);
        int x = inset;
        int y = inset + dy;
        int w = getWidth() - inset * 2;
        int h = getHeight() - inset * 2;
        if (w <= 0 || h <= 0) {
            g.dispose();
            return;
        }

        if (interactive && p > 0.01f) {
            Theme.shadow(g, x, y, w, h, radius,
                    Math.round(10 + 6 * p), Math.round(4 + 4 * p), Math.round(26 + 14 * p));
        } else {
            Theme.cardShadow(g, x, y, w, h, radius);
        }
        Theme.fillRound(g, x, y, w, h, radius, fill);

        paintCardContent(g, x, y, w, h);
        g.dispose();
    }

    /**
     * Hook for subclasses to draw inside the rounded surface.
     * Coordinates are those of the card body, already offset for the hover lift.
     */
    protected void paintCardContent(Graphics2D g, int x, int y, int w, int h) {
    }

    /** The inset consumed by the shadow gutter, so children can be laid out correctly. */
    public static int shadowInset() {
        return 8;
    }
}
