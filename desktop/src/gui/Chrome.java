import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public final class Chrome {
    private Chrome() {}

    private static final Map<JFrame, Rectangle> NORMAL = new HashMap<>();

    public static void prepare(JFrame frame) {
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 0));
        if (Logo.AVAILABLE) frame.setIconImage(Logo.scaled(64));
    }

    public static void makeDraggable(JComponent comp, Window win) {
        final Point[] offset = {null};
        comp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                offset[0] = new Point(e.getXOnScreen() - win.getX(), e.getYOnScreen() - win.getY());
            }
        });
        comp.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (offset[0] != null) {
                    win.setLocation(e.getXOnScreen() - offset[0].x, e.getYOnScreen() - offset[0].y);
                }
            }
        });
    }

    public static JPanel createTitlebar(JFrame frame) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 36));
        bar.add(windowControls(frame, true), BorderLayout.EAST);
        makeDraggable(bar, frame);
        return bar;
    }

    /**
     * The minimise / maximise / close cluster.
     *
     * @param withMaximise false for fixed-size windows such as the sign-in screen
     */
    public static JPanel windowControls(JFrame frame, boolean withMaximise) {
        // Dark marks on a light hover wash — the canvas behind this bar is ivory, not navy.
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 5));
        right.setOpaque(false);
        right.add(winButton(Glyph.MINIMISE, Theme.alpha(Theme.TEXT, 22), false,
                () -> frame.setState(JFrame.ICONIFIED)));
        if (withMaximise) {
            right.add(winButton(Glyph.MAXIMISE, Theme.alpha(Theme.TEXT, 22), false,
                    () -> toggleMax(frame)));
        }
        right.add(winButton(Glyph.CLOSE, Theme.DANGER, true, () -> {
            frame.dispose();
            System.exit(0);
        }));
        return right;
    }

    private static void toggleMax(JFrame frame) {
        Rectangle max = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Rectangle current = frame.getBounds();
        Rectangle normal = NORMAL.get(frame);
        if (normal != null && current.equals(max)) {
            frame.setBounds(normal);
            NORMAL.remove(frame);
        } else {
            if (normal == null) NORMAL.put(frame, current);
            frame.setBounds(max);
        }
    }

    /** Window-control marks, drawn as vectors. */
    private enum Glyph { MINIMISE, MAXIMISE, CLOSE }

    /**
     * A window control.
     *
     * <p>The marks are drawn rather than typed: the usual characters (─ □ ✕) are absent
     * from the bundled Inter faces and render as tofu boxes.
     *
     * @param invertOnHover when true the mark flips to white, for the close button whose
     *                      hover fill is a saturated red
     */
    private static JButton winButton(Glyph glyph, Color hover, boolean invertOnHover,
                                     Runnable action) {
        JButton b = new JButton() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                boolean over = getModel().isRollover();
                if (over) {
                    g.setColor(hover);
                    g.fillRoundRect(2, 1, getWidth() - 5, getHeight() - 3, 10, 10);
                }

                g.setColor(over && invertOnHover ? Color.WHITE : Theme.MUTED);
                g.setStroke(new java.awt.BasicStroke(1.4f, java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                switch (glyph) {
                    case MINIMISE -> g.drawLine(cx - 5, cy, cx + 5, cy);
                    case MAXIMISE -> g.drawRoundRect(cx - 5, cy - 5, 10, 10, 3, 3);
                    case CLOSE -> {
                        g.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
                        g.drawLine(cx + 4, cy - 4, cx - 4, cy + 4);
                    }
                }
                g.dispose();
            }
        };
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setPreferredSize(new Dimension(38, 26));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }
}
