import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * A catalogue tile: the composed cover floating above the title, author and
 * availability badge.
 *
 * <p>Everything is painted in one pass rather than assembled from child components.
 * The catalogue can hold hundreds of these inside a scroll pane, and a few hundred
 * nested Swing containers each with their own borders and layout managers makes
 * scrolling visibly stutter.
 */
public class BookCard extends Card {

    private final Livre livre;

    public static final int WIDTH = 214;
    public static final int HEIGHT = 392;

    private static final int PAD = 18;
    /** Titles always reserve two lines, so authors and badges align across the grid. */
    private static final int TITLE_LINES = 2;

    public BookCard(Livre livre, Runnable onOpen) {
        super(Theme.RADIUS_LG);
        this.livre = livre;

        Dimension d = new Dimension(WIDTH, HEIGHT);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
        onClick(onOpen);
        setToolTipText(livre.getTitre());
    }

    public Livre getLivre() {
        return livre;
    }

    @Override
    protected void paintCardContent(Graphics2D g, int x, int y, int w, int h) {
        float lift = hoverProgress();

        int coverW = w - PAD * 2;
        int coverH = Math.round(coverW * 1.30f);
        int coverX = x + PAD;
        int coverY = y + PAD - Math.round(2 * lift);

        BookCover.paint(g, coverX, coverY, coverW, coverH, livre, lift);

        int textTop = coverY + coverH + 16;
        int maxWidth = w - PAD * 2;

        // Title, wrapped to at most two lines. The block always occupies the full two
        // lines' worth of height even when the title is short, so every card in the grid
        // puts its author and badge on the same baseline.
        g.setFont(Theme.BODY_BOLD);
        FontMetrics fm = g.getFontMetrics();
        String title = livre.getTitre() == null ? "" : livre.getTitre();

        String first = title;
        String second = null;
        if (fm.stringWidth(title) > maxWidth) {
            int split = findSplit(title, fm, maxWidth);
            first = title.substring(0, split).trim();
            second = BookCover.ellipsise(title.substring(split).trim(), fm, maxWidth);
        }

        g.setColor(Theme.TEXT);
        g.drawString(first, x + PAD, textTop + fm.getAscent());
        if (second != null) {
            g.drawString(second, x + PAD, textTop + fm.getAscent() + fm.getHeight());
        }

        int authorY = textTop + fm.getAscent() + (TITLE_LINES - 1) * fm.getHeight() + 20;
        g.setFont(Theme.SMALL);
        FontMetrics afm = g.getFontMetrics();
        g.setColor(Theme.MUTED);
        String author = livre.getAuteur() == null ? "" : livre.getAuteur();
        g.drawString(BookCover.ellipsise(author, afm, maxWidth), x + PAD, authorY);

        // Availability badge, pinned to the bottom edge so it lines up across the grid.
        boolean available = livre.estDisponible();
        String badge = I18n.t(available ? "book.available" : "book.borrowed");
        g.setFont(Theme.TINY);
        FontMetrics bfm = g.getFontMetrics();
        int bw = bfm.stringWidth(badge) + 26;
        int bh = 26;
        int bx = x + PAD;
        int by = y + h - PAD - bh;

        Color fill = available ? Theme.SUCCESS_SOFT : Theme.AMBER_SOFT;
        Color ink = available ? new Color(0x11, 0x6B, 0x3D) : new Color(0x92, 0x51, 0x05);
        Theme.fillRound(g, bx, by, bw, bh, Theme.PILL, fill);
        g.setColor(ink);
        g.fillOval(bx + 11, by + bh / 2 - 3, 6, 6);
        g.drawString(badge, bx + 23, by + (bh - bfm.getHeight()) / 2 + bfm.getAscent());

        // Genre, right-aligned on the same baseline.
        String genre = livre.getGenre();
        if (genre != null && !genre.isBlank()) {
            g.setFont(Theme.TINY);
            FontMetrics gfm = g.getFontMetrics();
            String shown = BookCover.ellipsise(genre, gfm, maxWidth - bw - 12);
            g.setColor(Theme.FAINT);
            g.drawString(shown, x + w - PAD - gfm.stringWidth(shown),
                    by + (bh - gfm.getHeight()) / 2 + gfm.getAscent());
        }
    }

    /** Finds the character index to break a title at, without splitting a word. */
    private int findSplit(String text, FontMetrics fm, int maxWidth) {
        int best = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != ' ') continue;
            if (fm.stringWidth(text.substring(0, i)) <= maxWidth) best = i;
            else break;
        }
        if (best > 0) return best;
        // A single unbroken word longer than the line: fall back to a hard cut.
        int end = text.length();
        while (end > 1 && fm.stringWidth(text.substring(0, end)) > maxWidth) end--;
        return end;
    }
}
