import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.File;

public final class Theme {
    private Theme() {}

    // FST Settat — palette extraite du logo officiel (fsts_logo.png).
    // Bleu institutionnel profond #004080 + or académique #F0A000.
    public static final Color PRIMARY = new Color(0x00, 0x40, 0x80);
    public static final Color PRIMARY_2 = new Color(0x0B, 0x5A, 0xA8);
    public static final Color PRIMARY_DARK = new Color(0x00, 0x2B, 0x57);
    public static final Color PRIMARY_LIGHT = new Color(0x70, 0x90, 0xB0);
    public static final Color SECONDARY = new Color(0x3A, 0x6F, 0xA5);
    public static final Color ACCENT = new Color(0xE9, 0xA4, 0x00);
    public static final Color ACCENT_LIGHT = new Color(0xF5, 0xC1, 0x3D);

    public static final Color SIDEBAR = new Color(0x0A, 0x1D, 0x36);
    public static final Color SIDEBAR_HOVER = new Color(0x12, 0x2C, 0x4E);
    public static final Color BG = new Color(0xF3, 0xF6, 0xFB);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(0x14, 0x26, 0x3F);
    public static final Color MUTED = new Color(0x66, 0x75, 0x8E);
    public static final Color BORDER = new Color(0xE1, 0xE7, 0xF0);
    public static final Color DIVIDER = new Color(0xEC, 0xF0, 0xF6);
    public static final Color FIELD = new Color(0xF1, 0xF4, 0xFA);
    public static final Color HOVER = new Color(0xEA, 0xF1, 0xF9);
    public static final Color SELECTED = new Color(0xD8, 0xE6, 0xF6);
    public static final Color FOCUS = new Color(0x8F, 0xB5, 0xDE);
    public static final Color SUCCESS = new Color(0x16, 0xA3, 0x4A);
    public static final Color DANGER = new Color(0xDC, 0x26, 0x26);
    public static final Color AMBER = new Color(0xD9, 0x77, 0x06);

    public static final int RADIUS = 16;
    public static final int CARD_RADIUS = 22;

    public static final String FONT_FAMILY = "Inter";

    private static Font loadFont(String fileName, int style) {
        try {
            String base = System.getProperty("user.dir");
            File f = new File(base, "Inter" + File.separator + "static" + File.separator + fileName);
            if (!f.exists()) {
                f = new File("Inter/static/" + fileName);
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, f).deriveFont(style);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (Exception e) {
            System.out.println("Inter font not found: " + fileName + " – falling back to SansSerif");
            return new Font("SansSerif", style, 14);
        }
    }

    private static final Font INTER_REGULAR = loadFont("Inter_18pt-Regular.ttf", Font.PLAIN);
    private static final Font INTER_SEMIBOLD = loadFont("Inter_18pt-SemiBold.ttf", Font.BOLD);
    private static final Font INTER_BOLD = loadFont("Inter_18pt-Bold.ttf", Font.BOLD);
    private static final Font INTER_MEDIUM = loadFont("Inter_18pt-Medium.ttf", Font.PLAIN);

    public static final Font FONT       = INTER_REGULAR.deriveFont(14f);
    public static final Font FONT_BOLD  = INTER_SEMIBOLD.deriveFont(14f);
    public static final Font H1         = INTER_BOLD.deriveFont(26f);
    public static final Font H2         = INTER_SEMIBOLD.deriveFont(19f);
    public static final Font NUMBER     = INTER_BOLD.deriveFont(30f);
    public static final Font SMALL      = INTER_REGULAR.deriveFont(12f);
    public static final Font SMALL_BOLD = INTER_SEMIBOLD.deriveFont(12f);
    public static final Font TITLE_BIG  = INTER_BOLD.deriveFont(42f);
    public static final Font BODY_15    = INTER_REGULAR.deriveFont(15f);
    public static final Font BOLD_15    = INTER_SEMIBOLD.deriveFont(15f);
    public static final Font BOLD_24    = INTER_SEMIBOLD.deriveFont(24f);
    public static final Font BOLD_13    = INTER_SEMIBOLD.deriveFont(13f);
    public static final Font PLAIN_14   = INTER_REGULAR.deriveFont(14f);

    public static Color mix(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    public static void fillRound(Graphics2D g, int x, int y, int w, int h, int r, Color c) {
        g.setColor(c);
        g.fillRoundRect(x, y, w, h, r * 2, r * 2);
    }

    public static void gradientRound(Graphics2D g, int x, int y, int w, int h, int r, Color c1, Color c2) {
        g.setPaint(new GradientPaint(x, y, c1, x + w, y + h, c2, true));
        g.fillRoundRect(x, y, w, h, r * 2, r * 2);
    }

    public static Color[] genreGradient(String genre) {
        String g = genre == null ? "" : genre.toLowerCase();
        if (g.contains("trag") || g.contains("drame")) return new Color[]{new Color(0x9F, 0x1D, 0x1D), new Color(0xDC, 0x26, 0x26)};
        if (g.contains("polic") || g.contains("sf") || g.contains("thrill") || g.contains("science")) return new Color[]{new Color(0x1D, 0x4E, 0xD8), new Color(0x3B, 0x82, 0xF6)};
        if (g.contains("roman") || g.contains("hist")) return new Color[]{new Color(0x00, 0x40, 0x80), new Color(0x0B, 0x5A, 0xA8)};
        if (g.contains("autobi") || g.contains("bio")) return new Color[]{new Color(0x0D, 0x94, 0x88), new Color(0x14, 0xB8, 0xA6)};
        if (g.contains("po") || g.contains("essai") || g.contains("philo")) return new Color[]{new Color(0xB4, 0x53, 0x09), new Color(0xF5, 0x9E, 0x0B)};
        return new Color[]{new Color(0x00, 0x40, 0x80), new Color(0x0B, 0x5A, 0xA8)};
    }

    public static void styleScroll(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        sp.getVerticalScrollBar().setUI(new ScrollUI());
    }

    static class ScrollUI extends BasicScrollBarUI {
        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            if (this.decrButton == null) this.decrButton = createDecreaseButton(NORTH);
            if (this.incrButton == null) this.incrButton = createIncreaseButton(SOUTH);
        }

        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(150, 158, 190);
        }

        @Override
        protected JButton createDecreaseButton(int o) {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected JButton createIncreaseButton(int o) {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }

        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        }

        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D gg = (Graphics2D) g.create();
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gg.setColor(new Color(140, 148, 178, 140));
            gg.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            gg.dispose();
        }
    }
}
