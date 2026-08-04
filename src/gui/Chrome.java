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
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 5));
        right.setOpaque(false);
        right.add(winButton("─", new Color(255, 255, 255, 26), () -> frame.setState(JFrame.ICONIFIED)));
        right.add(winButton("□", new Color(255, 255, 255, 26), () -> toggleMax(frame)));
        right.add(winButton("✕", new Color(231, 76, 60), () -> {
            frame.dispose();
            System.exit(0);
        }));
        bar.add(right, BorderLayout.EAST);
        makeDraggable(bar, frame);
        return bar;
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

    private static JButton winButton(String label, Color hover, Runnable action) {
        JButton b = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g.setColor(hover);
                    g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                }
                g.dispose();
                super.paintComponent(g0);
            }
        };
        b.setFont(Theme.PLAIN_14);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setForeground(new Color(255, 255, 255, 190));
        b.setPreferredSize(new Dimension(40, 26));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }
}
