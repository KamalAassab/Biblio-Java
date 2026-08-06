import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a book cover.
 *
 * <p>The catalogue stores no artwork, so a cover is composed instead: a two-tone
 * gradient derived from the genre (or, failing that, deterministically from the title),
 * a darker spine down the left edge, a hairline highlight, and the title set large
 * with the author beneath.
 *
 * <p>Because the colours are derived from the title's hash, a given book always looks
 * the same across sessions and screens — the shelf reads as a row of distinct objects
 * rather than interchangeable placeholders.
 */
public final class BookCover {

    private BookCover() {}

    /** The reference layout's cover proportion, close to a real trade paperback. */
    public static final float ASPECT = 1.46f;

    public static int heightFor(int width) {
        return Math.round(width * ASPECT);
    }

    public static void paint(Graphics2D g0, int x, int y, int w, int h, Livre livre, float lift) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);

        String title = livre.getTitre() == null ? "" : livre.getTitre();
        String author = livre.getAuteur() == null ? "" : livre.getAuteur();
        Color[] grad = Theme.coverGradient(title, livre.getGenre());
        int radius = Math.max(6, Math.round(w * 0.055f));

        // Drop shadow, deepening as the cover lifts.
        Theme.shadow(g, x, y, w, h, radius,
                Math.round(9 + 7 * lift), Math.round(5 + 5 * lift), Math.round(34 + 18 * lift));

        RoundRectangle2D shape = new RoundRectangle2D.Float(x, y, w, h, radius * 2f, radius * 2f);
        g.setPaint(new GradientPaint(x, y, grad[1], x + w * 0.6f, y + h, grad[0]));
        g.fill(shape);

        java.awt.Shape clip = g.getClip();
        g.clip(shape);

        // Spine: a darker band with a bright rule, the strongest cue that this is a book.
        int spine = Math.max(5, Math.round(w * 0.085f));
        g.setColor(Theme.alpha(Color.BLACK, 46));
        g.fillRect(x, y, spine, h);
        g.setColor(Theme.alpha(Color.WHITE, 38));
        g.fillRect(x + spine, y, 1, h);

        // A soft diagonal sheen across the upper corner.
        g.setPaint(new GradientPaint(x, y, Theme.alpha(Color.WHITE, 40),
                x + w * 0.75f, y + h * 0.55f, Theme.alpha(Color.WHITE, 0)));
        g.fillRect(x, y, w, h);

        int padL = spine + Math.round(w * 0.10f);
        int padR = Math.round(w * 0.09f);
        int textW = w - padL - padR;

        // Title, wrapped and capped so a long title never spills off the cover.
        float titleSize = Math.max(10.5f, w * 0.108f);
        Font titleFont = Theme.H3.deriveFont(Font.BOLD, titleSize);
        g.setFont(titleFont);
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = wrap(title, fm, textW, 4);

        int lineHeight = fm.getHeight();
        int ty = y + Math.round(h * 0.17f) + fm.getAscent();
        g.setColor(Theme.alpha(Color.BLACK, 55));
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(lines.get(i), padL + x + 1, ty + i * lineHeight + 1);
        }
        g.setColor(Color.WHITE);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(lines.get(i), padL + x, ty + i * lineHeight);
        }

        // Rule and author, anchored to the lower third.
        int ruleY = ty + lines.size() * lineHeight + Math.round(h * 0.035f);
        if (ruleY < y + h - Math.round(h * 0.16f)) {
            g.setColor(Theme.alpha(Color.WHITE, 110));
            g.fillRect(x + padL, ruleY, Math.round(textW * 0.34f), 2);
        }

        float authorSize = Math.max(8.5f, w * 0.072f);
        g.setFont(Theme.SMALL.deriveFont(authorSize));
        FontMetrics afm = g.getFontMetrics();
        String shownAuthor = ellipsise(author, afm, textW);
        g.setColor(Theme.alpha(Color.WHITE, 215));
        g.drawString(shownAuthor, x + padL, y + h - Math.round(h * 0.075f));

        g.setClip(clip);

        // Hairline edge so the cover separates from a white card behind it.
        g.setColor(Theme.alpha(Color.BLACK, 28));
        g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1f, h - 1f,
                radius * 2f, radius * 2f));

        g.dispose();
    }

    /** Greedy word wrap, truncating the final line with an ellipsis when it overflows. */
    private static List<String> wrap(String text, FontMetrics fm, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;

        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (line.length() > 0) lines.add(line.toString());
            line.setLength(0);
            line.append(word);
            if (lines.size() == maxLines) break;
        }
        if (lines.size() < maxLines && line.length() > 0) lines.add(line.toString());

        if (lines.size() > maxLines) lines = lines.subList(0, maxLines);
        if (!lines.isEmpty()) {
            int last = lines.size() - 1;
            lines.set(last, ellipsise(lines.get(last), fm, maxWidth));
        }
        return lines;
    }

    static String ellipsise(String text, FontMetrics fm, int maxWidth) {
        if (text == null) return "";
        if (fm.stringWidth(text) <= maxWidth) return text;
        String ellipsis = "…";
        int available = maxWidth - fm.stringWidth(ellipsis);
        if (available <= 0) return ellipsis;
        int end = text.length();
        while (end > 0 && fm.stringWidth(text.substring(0, end)) > available) end--;
        return text.substring(0, end).trim() + ellipsis;
    }
}
