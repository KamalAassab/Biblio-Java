import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * A dashboard metric: large figure, label, and a tinted icon chip.
 *
 * <p>The figure counts up from zero when it changes rather than snapping, which draws
 * the eye to what actually moved after a refresh.
 */
public class StatCard extends Card {

    private final String labelKey;
    private final Color accent;
    private final Icons.Kind icon;

    private int value;
    private float shown;
    private final Timer counter;
    private String caption;

    public StatCard(String labelKey, Color accent, Icons.Kind icon) {
        super(Theme.RADIUS_LG);
        this.labelKey = labelKey;
        this.accent = accent;
        this.icon = icon;

        setPreferredSize(new Dimension(240, 148));
        setMinimumSize(new Dimension(180, 148));

        counter = new Timer(Theme.Motion.TICK_MS, e -> {
            shown = Theme.Motion.approach(shown, value, 0.18f);
            if (Math.abs(shown - value) < 0.5f) {
                shown = value;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        counter.setCoalesce(true);
    }

    public void setValue(int v) {
        if (this.value == v) return;
        this.value = v;
        if (!counter.isRunning()) counter.start();
    }

    /** Optional secondary line, e.g. "4 of 8 available". */
    public void setCaption(String text) {
        this.caption = text;
        repaint();
    }

    @Override
    protected void paintCardContent(Graphics2D g, int x, int y, int w, int h) {
        int pad = 22;

        // Icon chip, tinted from the metric's accent.
        int chip = 44;
        Theme.fillRound(g, x + w - pad - chip, y + pad, chip, chip, 14, Theme.alpha(accent, 30));
        Icons.paint(g, icon, x + w - pad - chip + 12, y + pad + 12, 20, accent);

        g.setFont(Theme.NUMBER);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Theme.TEXT);
        g.drawString(String.valueOf(Math.round(shown)), x + pad, y + pad + fm.getAscent() + 4);

        g.setFont(Theme.BODY_MEDIUM);
        FontMetrics lfm = g.getFontMetrics();
        g.setColor(Theme.MUTED);
        String label = I18n.t(labelKey);
        g.drawString(BookCover.ellipsise(label, lfm, w - pad * 2),
                x + pad, y + pad + fm.getAscent() + lfm.getHeight() + 12);

        if (caption != null && !caption.isEmpty()) {
            g.setFont(Theme.TINY);
            FontMetrics cfm = g.getFontMetrics();
            g.setColor(accent);
            g.drawString(BookCover.ellipsise(caption, cfm, w - pad * 2),
                    x + pad, y + h - pad + 2);
        }
    }
}
