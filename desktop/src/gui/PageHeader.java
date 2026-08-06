import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * The oversized page title block that opens every view, with an optional row of
 * actions pinned to the right.
 *
 * <p>Painted rather than composed from labels so the display face renders with the
 * exact optical spacing the layout depends on.
 */
public class PageHeader extends JPanel {

    private String title = "";
    private String subtitle = "";
    private final JPanel actions = new JPanel();

    public PageHeader() {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(4, 18, 18, 8));

        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        actions.setBorder(new EmptyBorder(22, 0, 0, 0));
        add(actions, BorderLayout.EAST);

        setPreferredSize(new Dimension(0, 104));
    }

    public PageHeader setTitle(String title, String subtitle) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        repaint();
        return this;
    }

    public PageHeader addAction(JComponent c) {
        if (actions.getComponentCount() > 0) actions.add(Box.createHorizontalStrut(10));
        actions.add(c);
        revalidate();
        return this;
    }

    public void clearActions() {
        actions.removeAll();
        actions.revalidate();
        actions.repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);

        int x = 18;
        // Shrink the display size on narrow windows so the title never collides with actions.
        int available = getWidth() - x - actions.getPreferredSize().width - 40;
        java.awt.Font face = Theme.DISPLAY;
        g.setFont(face);
        if (g.getFontMetrics().stringWidth(title) > available && available > 0) {
            face = Theme.DISPLAY_SM;
            g.setFont(face);
        }

        FontMetrics fm = g.getFontMetrics();
        g.setColor(Theme.TEXT);
        int baseline = 26 + fm.getAscent();
        g.drawString(title, x, baseline);

        if (!subtitle.isEmpty()) {
            g.setFont(Theme.BODY);
            FontMetrics sfm = g.getFontMetrics();
            g.setColor(Theme.MUTED);
            g.drawString(subtitle, x, baseline + sfm.getHeight() + 6);
        }
        g.dispose();
    }
}
