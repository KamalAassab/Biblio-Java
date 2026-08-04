import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class Dialogs {
    private Dialogs() {}

    public static JDialog shell(Component owner) {
        JDialog d = new JDialog((Window) SwingUtilities.getWindowAncestor(owner), Dialog.ModalityType.APPLICATION_MODAL);
        d.setUndecorated(true);
        d.setBackground(new Color(0, 0, 0, 0));
        return d;
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.H2);
        l.setForeground(Theme.TEXT);
        return l;
    }

    public static JPanel verticalBox() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    public static void strut(JPanel p, int h) {
        p.add(Box.createVerticalStrut(h));
    }

    public static void showWithOverlay(JDialog dialog, JComponent card, Component owner) {
        Window parentWin = (owner != null) ? SwingUtilities.getWindowAncestor(owner) : null;

        JPanel overlay = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        overlay.setOpaque(false);

        if (parentWin != null) {
            dialog.setBounds(parentWin.getBounds());
        } else {
            dialog.setSize(1200, 800);
        }

        Dimension cardSize = card.getPreferredSize();
        int cx = (dialog.getWidth() - cardSize.width) / 2;
        int cy = (dialog.getHeight() - cardSize.height) / 2;
        card.setBounds(cx, cy, cardSize.width, cardSize.height);

        overlay.add(card);
        overlay.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() == overlay) dialog.dispose();
            }
        });
        dialog.setContentPane(overlay);
        dialog.setLocationRelativeTo(null);
    }
}
