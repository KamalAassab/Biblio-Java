import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Placeholder shown when a list has nothing in it.
 *
 * <p>Distinguishes "nothing matched your filters" from "nothing exists yet" by taking
 * both strings from the caller — the two situations need different next steps, and a
 * single generic message helps with neither.
 */
public class EmptyState extends JComponent {

    private String title;
    private String message;
    private Icons.Kind icon = Icons.Kind.SEARCH;

    public EmptyState(String title, String message) {
        this.title = title;
        this.message = message;
        setOpaque(false);
        setPreferredSize(new Dimension(320, 260));
    }

    public EmptyState withIcon(Icons.Kind kind) {
        this.icon = kind;
        repaint();
        return this;
    }

    public void setText(String title, String message) {
        this.title = title;
        this.message = message;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        int disc = 84;
        Theme.fillRound(g, cx - disc / 2, cy - disc - 6, disc, disc, disc / 2, Theme.SURFACE_CHIP);
        Icons.paint(g, icon, cx - 17, cy - disc + 19, 34, Theme.mix(Theme.MUTED, Theme.FAINT, 0.4f));

        g.setFont(Theme.H3);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Theme.TEXT);
        g.drawString(title, cx - fm.stringWidth(title) / 2, cy + 22);

        g.setFont(Theme.BODY);
        FontMetrics mfm = g.getFontMetrics();
        g.setColor(Theme.MUTED);
        // Wrap the message across at most two lines.
        int maxWidth = Math.min(getWidth() - 60, 420);
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : message.split("\\s+")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (mfm.stringWidth(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
            if (lines.size() == 2) break;
        }
        if (lines.size() < 2 && line.length() > 0) lines.add(line.toString());

        int ty = cy + 22 + mfm.getHeight() + 6;
        for (String l : lines) {
            g.drawString(l, cx - mfm.stringWidth(l) / 2, ty);
            ty += mfm.getHeight();
        }
        g.dispose();
    }
}
