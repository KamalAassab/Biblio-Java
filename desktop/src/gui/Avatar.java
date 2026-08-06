import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * A circular initials badge.
 *
 * <p>Colour is derived from the initials themselves, so the same person is always the
 * same colour without needing a stored preference — useful in the members table where
 * a column of identical badges would carry no information.
 */
public class Avatar extends JComponent {

    private String initials;
    private final int size;
    private Color from;
    private Color to;
    private boolean ring;

    public Avatar(String initials, int size) {
        this(initials, size, null, null);
    }

    public Avatar(String initials, int size, Color from, Color to) {
        this.initials = initials == null || initials.isBlank() ? "?" : initials;
        this.size = size;
        if (from != null && to != null) {
            this.from = from;
            this.to = to;
        } else {
            Color[] derived = derive(this.initials);
            this.from = derived[0];
            this.to = derived[1];
        }
        setOpaque(false);
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
    }

    public void setInitials(String text) {
        this.initials = text == null || text.isBlank() ? "?" : text;
        Color[] derived = derive(this.initials);
        this.from = derived[0];
        this.to = derived[1];
        repaint();
    }

    /** Draws a white ring around the badge, for placement over a coloured surface. */
    public Avatar withRing() {
        this.ring = true;
        return this;
    }

    private static Color[] derive(String seed) {
        int hash = Math.abs(seed.hashCode());
        float hue = (hash % 360) / 360f;
        return new Color[]{
                Color.getHSBColor(hue, 0.55f, 0.62f),
                Color.getHSBColor((hue + 0.05f) % 1f, 0.62f, 0.80f)
        };
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);

        int d = Math.min(getWidth(), getHeight());
        int x = (getWidth() - d) / 2;
        int y = (getHeight() - d) / 2;

        if (ring) {
            g.setColor(Color.WHITE);
            g.fillOval(x, y, d, d);
            int inset = 2;
            x += inset;
            y += inset;
            d -= inset * 2;
        }

        g.setPaint(new java.awt.GradientPaint(x, y, to, x + d, y + d, from));
        g.fillOval(x, y, d, d);

        g.setFont(Theme.BODY_BOLD.deriveFont(Math.max(10f, d * 0.38f)));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(initials);
        g.setColor(Color.WHITE);
        g.drawString(initials, x + (d - tw) / 2, y + (d - fm.getHeight()) / 2 + fm.getAscent());

        g.dispose();
    }

    /** Extracts up to two uppercase initials from a display name. */
    public static String initialsOf(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty()) sb.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }
}
