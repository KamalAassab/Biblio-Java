import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
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

    /**
     * Paints the cover.
     *
     * <p>{@code onArtworkLoaded} is invoked on the event thread when a cover that was
     * not yet cached finishes downloading; the caller should repaint. Pass null when
     * repainting is not possible (a one-shot render), and the generated cover is used.
     */
    public static void paint(Graphics2D g0, int x, int y, int w, int h, Livre livre, float lift,
                             Runnable onArtworkLoaded) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);

        String title = livre.getTitre() == null ? "" : livre.getTitre();
        String author = livre.getAuteur() == null ? "" : livre.getAuteur();
        Color[] grad = Theme.coverGradient(title, livre.getGenre());
        // Proportional so a thumbnail and the large dialog cover look like the same
        // object; ~18px at the catalogue's card width, which is Theme.RADIUS_SM and
        // matches `rounded-[--radius-sm]` on the web cover.
        int radius = Math.max(8, Math.round(w * 0.072f));

        // Drop shadow, deepening as the cover lifts.
        Theme.shadow(g, x, y, w, h, radius,
                Math.round(9 + 7 * lift), Math.round(5 + 5 * lift), Math.round(34 + 18 * lift));

        RoundRectangle2D shape = new RoundRectangle2D.Float(x, y, w, h, radius * 2f, radius * 2f);
        // The gradient is painted first either way: it is what shows through in the
        // moment between the card appearing and the artwork arriving.
        g.setPaint(new GradientPaint(x, y, grad[1], x + w * 0.6f, y + h, grad[0]));
        g.fill(shape);

        java.awt.Shape clip = g.getClip();
        g.clip(shape);

        BufferedImage artwork = CoverCache.get(livre.getImageUrl(), onArtworkLoaded);
        if (artwork != null) {
            paintArtwork(g, artwork, x, y, w, h);
            g.setClip(clip);
            // Hairline edge so the cover separates from a white card behind it.
            g.setColor(Theme.alpha(Color.BLACK, 28));
            g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1f, h - 1f,
                    radius * 2f, radius * 2f));
            g.dispose();
            return;
        }

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

    /** Overload for callers that cannot repaint; always draws the generated cover. */
    public static void paint(Graphics2D g, int x, int y, int w, int h, Livre livre, float lift) {
        paint(g, x, y, w, h, livre, lift, null);
    }

    /**
     * Draws {@code image} filling the cover rectangle, centre-cropped.
     *
     * <p>Editions differ wildly in proportion — some covers are near-square, some are
     * tall — so the image is scaled to cover the box and the overflow is trimmed
     * equally from both sides. This is the same rule as CSS {@code object-fit: cover},
     * which keeps the desktop and web shelves looking identical.
     */
    private static void paintArtwork(Graphics2D g, BufferedImage image, int x, int y, int w, int h) {
        int iw = image.getWidth();
        int ih = image.getHeight();
        if (iw <= 0 || ih <= 0) return;

        // Scale so the image covers the box, then centre the source rectangle.
        double scale = Math.max((double) w / iw, (double) h / ih);
        int drawW = (int) Math.ceil(iw * scale);
        int drawH = (int) Math.ceil(ih * scale);
        int dx = x + (w - drawW) / 2;
        int dy = y + (h - drawH) / 2;

        Object previous = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, dx, dy, drawW, drawH, null);
        if (previous != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, previous);

        // A whisper of shading down the left edge so the artwork still reads as a book
        // rather than a flat tile pasted onto the card.
        g.setPaint(new GradientPaint(x, y, Theme.alpha(Color.BLACK, 90),
                x + Math.max(4, w * 0.06f), y, Theme.alpha(Color.BLACK, 0)));
        g.fillRect(x, y, Math.max(4, Math.round(w * 0.06f)), h);
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
