import javax.imageio.ImageIO;
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
        if (SOURCE == null) return;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(SOURCE, x, y, w, h, null);
    }
}
