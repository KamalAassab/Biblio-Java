import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;

public final class Toast {
    private Toast() {}

    public static void show(Component anchor, String text) {
        JWindow win = new JWindow();
        RoundedPanel p = new RoundedPanel(new Color(23, 27, 44), 14);
        p.setLayout(new java.awt.BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        p.add(l, java.awt.BorderLayout.CENTER);
        win.setContentPane(p);
        win.pack();

        Window parent = anchor != null ? SwingUtilities.getWindowAncestor(anchor) : null;
        Point loc;
        Dimension ps;
        if (parent != null && parent.isShowing()) {
            loc = parent.getLocationOnScreen();
            ps = parent.getSize();
        } else {
            loc = new Point(0, 0);
            ps = new Dimension(900, 600);
        }
        win.setLocation(loc.x + (ps.width - win.getWidth()) / 2, loc.y + ps.height - win.getHeight() - 60);
        win.setVisible(true);

        Timer fade = new Timer(30, null);
        final float[] alpha = {1f};
        fade.addActionListener(e -> {
            alpha[0] -= 0.06f;
            if (alpha[0] <= 0) {
                win.dispose();
                fade.stop();
            } else {
                try {
                    win.setOpacity(alpha[0]);
                } catch (Exception ex) {
                    win.dispose();
                    fade.stop();
                }
            }
        });
        Timer wait = new Timer(2200, e -> fade.start());
        wait.setRepeats(false);
        wait.start();
    }
}
