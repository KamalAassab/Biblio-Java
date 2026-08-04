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
        String prop = System.getProperty("fsts.logo");
        if (prop != null && !prop.isBlank()) {
            BufferedImage img = readFile(new File(prop));
            if (img != null) return img;
        }
        String[] candidates = {
            "assets/fsts_logo.png",
            "../assets/fsts_logo.png",
            "../../assets/fsts_logo.png",
            "Biblio-Java-master/assets/fsts_logo.png",
        };
        for (String c : candidates) {
            BufferedImage img = readFile(new File(c));
            if (img != null) return img;
        }
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

    public static void draw(Graphics2D g, int x, int y, int w, int h) {
        drawCard(g, x, y, Math.min(w, h), 14);
    }

    public static void drawCard(Graphics2D g, int x, int y, int size, int radius) {
        int r = radius > 0 ? radius : 14;
        g.setColor(new Color(0, 0, 0, 24));
        g.fillRoundRect(x + 1, y + 2, size, size, r * 2, r * 2);
        Theme.fillRound(g, x, y, size, size, r, Color.WHITE);

        if (SOURCE != null) {
            int pad = Math.max(4, Math.round(size * 0.12f));
            int iw = size - pad * 2;
            int ih = size - pad * 2;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(SOURCE, x + pad, y + pad, iw, ih, null);
        } else {
            int iconSize = Math.round(size * 0.5f);
            int ix = x + (size - iconSize) / 2;
            int iy = y + (size - iconSize) / 2;
            Icons.paint(g, Icons.Kind.BOOK, ix, iy, iconSize, Theme.PRIMARY);
        }
    }
}
