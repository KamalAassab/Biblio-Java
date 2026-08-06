import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * A shortcut tile: tinted icon chip, a title, and one line of context.
 *
 * <p>Used for the dashboard's quick actions, where each tile is a destination rather
 * than a toggle — the chevron that slides in on hover carries that distinction.
 */
public class ActionCard extends Card {

    private final Icons.Kind icon;
    private final Color accent;
    private final String title;
    private final String subtitle;

    public ActionCard(Icons.Kind icon, Color accent, String title, String subtitle, Runnable action) {
        super(Theme.RADIUS_LG);
        this.icon = icon;
        this.accent = accent;
        this.title = title;
        this.subtitle = subtitle;
        setPreferredSize(new Dimension(220, 124));
        setMinimumSize(new Dimension(170, 124));
        onClick(action);
    }

    @Override
    protected void paintCardContent(Graphics2D g, int x, int y, int w, int h) {
        int pad = 20;
        int chip = 42;

        Theme.fillRound(g, x + pad, y + pad, chip, chip, 13, Theme.alpha(accent, 30));
        Icons.paint(g, icon, x + pad + 11, y + pad + 11, 20, accent);

        float p = hoverProgress();
        if (p > 0.02f) {
            Icons.paint(g, Icons.Kind.CHEVRON_RIGHT,
                    x + w - pad - 16 + Math.round(4 * p), y + pad + 13, 16,
                    Theme.alpha(accent, Math.round(200 * p)));
        }

        g.setFont(Theme.BODY_BOLD);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Theme.TEXT);
        g.drawString(BookCover.ellipsise(title, fm, w - pad * 2), x + pad, y + pad + chip + 26);

        g.setFont(Theme.SMALL);
        FontMetrics sfm = g.getFontMetrics();
        g.setColor(Theme.MUTED);
        g.drawString(BookCover.ellipsise(subtitle, sfm, w - pad * 2),
                x + pad, y + pad + chip + 26 + sfm.getHeight() + 4);
    }
}
