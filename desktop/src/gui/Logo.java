import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public final class Logo {
    private Logo() {}

    private static final BufferedImage SOURCE = load();
    public static final boolean AVAILABLE = SOURCE != null;

    private static BufferedImage load() {
        // An explicit override wins, so a packaged build can point at its own copy.
        String prop = System.getProperty("fsts.logo");
        if (prop != null && !prop.isBlank()) {
            BufferedImage img = readFile(new File(prop));
            if (img != null) return img;
        }

        BufferedImage img = readFile(Resources.find("assets/fsts_logo.png"));
        if (img != null) return img;

        // Last resort: the crest bundled inside the jar.
        try (InputStream in = Logo.class.getResourceAsStream("/assets/fsts_logo.png")) {
            if (in != null) return ImageIO.read(in);
        } catch (IOException ignored) {
        }
        return null;
    }

    private static BufferedImage readFile(File f) {
        if (f == null || !f.isFile()) return null;
        try {
            return ImageIO.read(f);
        } catch (IOException e) {
            return null;
        }
    }

    public static BufferedImage image() {
        return SOURCE;
    }

    public static Image scaled(int size) {
        if (SOURCE == null) return null;
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(SOURCE, 0, 0, size, size, null);
        g.dispose();
        return out;
    }

    /**
     * Draws the crest bare — no plate, no border, no padding.
     *
     * <p>This is the right call on any light surface. The source PNG is already
     * transparent, so the crest sits directly on whatever is behind it and reads at
     * its full size instead of being inset inside a card.
     */
    public static void draw(Graphics2D g, int x, int y, int size) {
        if (SOURCE != null) {
            quality(g);
            g.drawImage(SOURCE, x, y, size, size, null);
        } else {
            Icons.paint(g, Icons.Kind.BOOK, x, y, size, Theme.PRIMARY);
        }
    }

    /**
     * Draws the crest on a white plate.
     *
     * <p>Reserved for dark surfaces. The crest is navy and gold, so on the navy
     * gradient used by the login panel and the dialog headers its navy strokes would
     * disappear entirely; the plate is what keeps it legible there.
     */
    public static void drawCard(Graphics2D g, int x, int y, int size, int radius) {
        int r = radius > 0 ? radius : 14;
        Theme.fillRound(g, x, y, size, size, r, Color.WHITE);

        // Tighter than a typical logo lockup: the crest has its own generous internal
        // margin, so a large pad reads as a small logo floating in a big box.
        int pad = Math.max(3, Math.round(size * 0.07f));
        draw(g, x + pad, y + pad, size - pad * 2);
    }

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
}
