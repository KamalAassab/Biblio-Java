import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.io.File;

/**
 * Design tokens for the desktop client.
 *
 * <p>Visual language: a near-white canvas carrying white, heavily rounded surfaces;
 * pill navigation with soft icon chips; oversized display type; and soft, wide
 * shadows that let cards float rather than sit in boxes.
 *
 * <p>The palette is sampled directly from the official FST Settat crest
 * ({@code assets/fsts_logo.png}), whose two inks are navy {@code #084888} and gold
 * {@code #F8A808}. Everything else is a tint or shade of those two hues, so the
 * interface introduces no colour the crest does not already contain.
 *
 * <p>Order of precedence: navy is primary and carries actions, active navigation and
 * headings; gold is secondary and marks the selected or noteworthy surface; white is
 * tertiary and carries the cards, with the canvas a barely-there navy tint so white
 * surfaces separate from it without needing a border.
 *
 * <p>Kept in step with {@code web/src/app/globals.css}.
 */
public final class Theme {

    private Theme() {}

    // ── Brand: navy (primary) ────────────────────────────────────────────────
    /** The exact navy ink of the FST Settat crest. */
    public static final Color PRIMARY = new Color(0x08, 0x48, 0x88);
    public static final Color PRIMARY_2 = new Color(0x14, 0x58, 0x9E);
    public static final Color PRIMARY_DARK = new Color(0x05, 0x2C, 0x54);
    public static final Color PRIMARY_SOFT = new Color(0xDC, 0xE8, 0xF7);
    public static final Color PRIMARY_LIGHT = new Color(0x8D, 0xB2, 0xE0);
    public static final Color SECONDARY = new Color(0x4A, 0x80, 0xBE);

    // ── Brand: gold (secondary) ──────────────────────────────────────────────
    /** The exact gold ink of the crest — the accent that marks the active surface. */
    public static final Color ACCENT = new Color(0xF8, 0xA8, 0x08);
    public static final Color ACCENT_2 = new Color(0xFA, 0xBB, 0x2C);
    public static final Color ACCENT_LIGHT = new Color(0xFB, 0xCF, 0x5E);
    public static final Color ACCENT_SOFT = new Color(0xFE, 0xEF, 0xC9);

    // ── Canvas & surfaces (white, tertiary) ──────────────────────────────────
    /** Application background: white cooled by a trace of the brand navy. */
    public static final Color CANVAS = new Color(0xF1, 0xF5, 0xFA);
    /** Slightly deeper tint for the band behind the header. */
    public static final Color CANVAS_DEEP = new Color(0xE4, 0xEB, 0xF4);
    public static final Color SURFACE = Color.WHITE;
    public static final Color SURFACE_SUNK = new Color(0xF7, 0xFA, 0xFD);
    public static final Color SURFACE_CHIP = new Color(0xEE, 0xF3, 0xF9);

    /** Retained for compatibility with older call sites. */
    public static final Color BG = CANVAS;
    public static final Color CARD = SURFACE;
    public static final Color SIDEBAR = SURFACE;
    public static final Color SIDEBAR_HOVER = SURFACE_CHIP;

    // ── Text ─────────────────────────────────────────────────────────────────
    public static final Color TEXT = new Color(0x10, 0x18, 0x22);
    public static final Color TEXT_SOFT = new Color(0x38, 0x42, 0x4F);
    public static final Color MUTED = new Color(0x6B, 0x76, 0x84);
    public static final Color FAINT = new Color(0x9A, 0xA4, 0xB2);
    public static final Color ON_PRIMARY = Color.WHITE;

    // ── Lines & states ───────────────────────────────────────────────────────
    public static final Color BORDER = new Color(0xDD, 0xE5, 0xEF);
    public static final Color BORDER_SOFT = new Color(0xEB, 0xF1, 0xF7);
    public static final Color DIVIDER = new Color(0xEE, 0xF3, 0xF9);
    public static final Color FIELD = new Color(0xF4, 0xF8, 0xFC);
    public static final Color HOVER = new Color(0xEE, 0xF3, 0xF9);
    public static final Color SELECTED = ACCENT_SOFT;
    public static final Color FOCUS = new Color(0x8D, 0xB2, 0xE0);

    public static final Color SUCCESS = new Color(0x1B, 0x9E, 0x5A);
    public static final Color SUCCESS_SOFT = new Color(0xE4, 0xF6, 0xEC);
    public static final Color DANGER = new Color(0xD9, 0x3F, 0x36);
    public static final Color DANGER_SOFT = new Color(0xFD, 0xEA, 0xE8);
    public static final Color AMBER = new Color(0xB8, 0x77, 0x0A);
    public static final Color AMBER_SOFT = new Color(0xFD, 0xF2, 0xD9);

    // ── Geometry ─────────────────────────────────────────────────────────────
    /**
     * The radius scale, in pixels.
     *
     * <p>These are the exact pixel values of {@code --radius-*} in the web client's
     * globals.css. The two must move together: a book card, a dialog and a badge are
     * meant to read as the same object in both editions, and a mismatched corner is
     * the first thing that gives that away.
     *
     * <p>Every step is deliberately generous — the design leans on soft, continuous
     * corners rather than on borders to separate surfaces.
     */
    public static final int RADIUS_XS = 14;
    public static final int RADIUS_SM = 18;
    public static final int RADIUS = 24;
    public static final int RADIUS_LG = 30;
    /** The large radius on the main content surface, as in the reference layout. */
    public static final int RADIUS_XL = 40;
    public static final int PILL = 999;
    public static final int CARD_RADIUS = RADIUS_LG;

    public static final int SIDEBAR_WIDTH = 248;
    public static final int GUTTER = 28;

    // ── Typography ───────────────────────────────────────────────────────────

    public static final String FONT_FAMILY = "Inter";

    /**
     * Loads a static Inter face.
     *
     * <p>Deliberately not the variable font: the JDK cannot select a weight axis, so a
     * variable file always renders at its default weight and Swing fakes bold by
     * smearing the glyphs. Loading real static faces is what makes the heavy display
     * sizes look intentional rather than blurry.
     */
    private static Font loadFont(String fileName, int fallbackStyle) {
        File file = Resources.find("assets/fonts/" + fileName);
        if (file != null) {
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, file);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                return font;
            } catch (Exception ignored) {
                // Corrupt or unreadable face — fall through to the system default.
            }
        }
        return new Font("SansSerif", fallbackStyle, 14);
    }

    private static final Font REGULAR   = loadFont("Inter_18pt-Regular.ttf", Font.PLAIN);
    private static final Font MEDIUM    = loadFont("Inter_18pt-Medium.ttf", Font.PLAIN);
    private static final Font SEMIBOLD  = loadFont("Inter_18pt-SemiBold.ttf", Font.BOLD);
    private static final Font BOLD      = loadFont("Inter_18pt-Bold.ttf", Font.BOLD);
    private static final Font EXTRABOLD = loadFont("Inter_24pt-ExtraBold.ttf", Font.BOLD);
    private static final Font BLACK     = loadFont("Inter_28pt-Black.ttf", Font.BOLD);

    /** Oversized page title, the anchor of the layout. */
    public static final Font DISPLAY     = BLACK.deriveFont(44f);
    public static final Font DISPLAY_SM  = BLACK.deriveFont(34f);
    public static final Font H1          = EXTRABOLD.deriveFont(28f);
    public static final Font H2          = BOLD.deriveFont(21f);
    public static final Font H3          = SEMIBOLD.deriveFont(17f);
    public static final Font SECTION     = BOLD.deriveFont(19f);
    public static final Font BODY        = REGULAR.deriveFont(14.5f);
    public static final Font BODY_MEDIUM = MEDIUM.deriveFont(14.5f);
    public static final Font BODY_BOLD   = SEMIBOLD.deriveFont(14.5f);
    public static final Font SMALL       = REGULAR.deriveFont(12.5f);
    public static final Font SMALL_BOLD  = SEMIBOLD.deriveFont(12.5f);
    public static final Font TINY        = MEDIUM.deriveFont(11f);
    /** Uppercase, tracked eyebrow label — the "MENU" treatment in the reference. */
    public static final Font EYEBROW     = SEMIBOLD.deriveFont(11f);
    public static final Font NUMBER      = EXTRABOLD.deriveFont(32f);
    public static final Font NUMBER_LG   = BLACK.deriveFont(40f);

    // Compatibility aliases for existing call sites.
    public static final Font FONT       = BODY;
    public static final Font FONT_BOLD  = BODY_BOLD;
    public static final Font TITLE_BIG  = DISPLAY;
    public static final Font BODY_15    = BODY;
    public static final Font BOLD_15    = BODY_BOLD;
    public static final Font BOLD_24    = H2;
    public static final Font BOLD_13    = SMALL_BOLD;
    public static final Font PLAIN_14   = BODY;

    // ── Painting helpers ─────────────────────────────────────────────────────

    public static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /**
     * Interpolates between two colours, including their alpha.
     *
     * <p>Alpha matters: fading a control in from {@code alpha(c, 0)} is a common pattern
     * here, and dropping the alpha channel would make a transparent start render as a
     * fully opaque block.
     */
    public static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    public static void fillRound(Graphics2D g, int x, int y, int w, int h, int r, Color c) {
        g.setColor(c);
        int d = clampRadius(r, w, h);
        g.fill(new RoundRectangle2D.Float(x, y, w, h, d, d));
    }

    public static void gradientRound(Graphics2D g, int x, int y, int w, int h, int r,
                                     Color c1, Color c2) {
        g.setPaint(new GradientPaint(x, y, c1, x + w, y + h, c2, true));
        int d = clampRadius(r, w, h);
        g.fill(new RoundRectangle2D.Float(x, y, w, h, d, d));
    }

    private static int clampRadius(int r, int w, int h) {
        // A PILL radius means "fully rounded", which is half the shorter side.
        int max = Math.min(w, h);
        return Math.min(r * 2, max);
    }

    /**
     * Paints a soft drop shadow beneath a rounded rectangle.
     *
     * <p>Built from a few concentric translucent outlines rather than a Gaussian blur —
     * the blur would allocate and convolve an image on every repaint, which is far too
     * expensive inside {@code paintComponent}.
     */
    public static void shadow(Graphics2D g, int x, int y, int w, int h, int r,
                              int spread, int yOffset, int opacity) {
        for (int i = spread; i > 0; i--) {
            int a = (int) (opacity * (1.0 - (double) i / (spread + 1)) / spread * 2.2);
            if (a <= 0) continue;
            // Navy-tinted rather than neutral: a grey shadow on the pale blue canvas
            // reads as dirt. Mirrors --shadow-card in the web client's globals.css.
            g.setColor(new Color(8, 40, 90, Math.min(a, 255)));
            int d = clampRadius(r + i / 2, w + i * 2, h + i * 2);
            g.fill(new RoundRectangle2D.Float(x - i, y - i + yOffset, w + i * 2f, h + i * 2f, d, d));
        }
    }

    /** The standard resting elevation for a card. */
    public static void cardShadow(Graphics2D g, int x, int y, int w, int h, int r) {
        shadow(g, x, y, w, h, r, 10, 4, 26);
    }

    /** A raised elevation used on hover, to make lift read as a state change. */
    public static void cardShadowHover(Graphics2D g, int x, int y, int w, int h, int r) {
        shadow(g, x, y, w, h, r, 16, 8, 40);
    }

    /**
     * Deterministic cover artwork for a book.
     *
     * <p>The catalogue has no cover images, so a stable two-tone gradient is derived from
     * the title's hash. The same book always renders the same colours, which lets the
     * shelf read as a set of distinct objects rather than repeated placeholders.
     */
    public static Color[] coverGradient(String title, String genre) {
        Color[] byGenre = genreGradientOrNull(genre);
        if (byGenre != null) return byGenre;
        int hash = title == null ? 0 : Math.abs(title.hashCode());
        float hue = (hash % 360) / 360f;
        Color a = Color.getHSBColor(hue, 0.42f, 0.52f);
        Color b = Color.getHSBColor((hue + 0.06f) % 1f, 0.50f, 0.72f);
        return new Color[]{a, b};
    }

    /** Genre-derived gradient, falling back to the brand blue for unrecognised genres. */
    public static Color[] genreGradient(String genre) {
        Color[] c = genreGradientOrNull(genre);
        return c != null ? c : new Color[]{PRIMARY, PRIMARY_2};
    }

    private static Color[] genreGradientOrNull(String genre) {
        String g = genre == null ? "" : genre.toLowerCase();
        if (g.isEmpty()) return null;
        if (g.contains("trag") || g.contains("drame"))
            return new Color[]{new Color(0x8E, 0x1F, 0x2E), new Color(0xC4, 0x3B, 0x45)};
        if (g.contains("polic") || g.contains("thrill") || g.contains("sf") || g.contains("science"))
            return new Color[]{new Color(0x1B, 0x3A, 0x6B), new Color(0x2F, 0x6B, 0xB8)};
        if (g.contains("hist"))
            return new Color[]{new Color(0x5B, 0x3A, 0x1E), new Color(0x9A, 0x6B, 0x38)};
        if (g.contains("roman"))
            return new Color[]{Theme.PRIMARY, Theme.PRIMARY_2};
        if (g.contains("autobi") || g.contains("bio") || g.contains("mémo") || g.contains("memo"))
            return new Color[]{new Color(0x0E, 0x6E, 0x66), new Color(0x18, 0xA7, 0x96)};
        if (g.contains("conte") || g.contains("fant") || g.contains("merveil"))
            return new Color[]{new Color(0x5A, 0x35, 0x8C), new Color(0x8C, 0x5A, 0xC8)};
        if (g.contains("philo") || g.contains("essai") || g.contains("po"))
            return new Color[]{new Color(0xA4, 0x5B, 0x0B), new Color(0xE9, 0xA4, 0x00)};
        return null;
    }

    // ── Scrollbars ───────────────────────────────────────────────────────────

    public static void styleScroll(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(9, 0));
        sp.getVerticalScrollBar().setOpaque(false);
        sp.getVerticalScrollBar().setUI(new ScrollUI());
    }

    /** Horizontal variant, for the shelf rows that scroll sideways. */
    public static void styleScrollHorizontal(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        sp.getHorizontalScrollBar().setUnitIncrement(24);
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 9));
        sp.getHorizontalScrollBar().setOpaque(false);
        sp.getHorizontalScrollBar().setUI(new ScrollUI());
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
            this.thumbColor = new Color(0xC2, 0xBA, 0xAA);
        }

        @Override
        protected JButton createDecreaseButton(int o) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int o) {
            return zeroButton();
        }

        private JButton zeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D gg = (Graphics2D) g.create();
            aa(gg);
            gg.setColor(new Color(0xB4, 0xAB, 0x99, 170));
            gg.fillRoundRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 9, 9);
            gg.dispose();
        }
    }

    // ── Motion ───────────────────────────────────────────────────────────────

    /**
     * Animation timing.
     *
     * <p>Every transition in the app runs through {@link #easeOut}. Motion decelerates
     * into its resting state and never overshoots, which keeps a dense admin interface
     * feeling quick rather than bouncy.
     */
    public static final class Motion {
        private Motion() {}

        /** ~60 fps. */
        public static final int TICK_MS = 16;
        public static final float FAST = 0.32f;
        public static final float NORMAL = 0.22f;
        public static final float SLOW = 0.14f;

        /** Cubic ease-out. */
        public static float easeOut(float t) {
            t = Math.max(0f, Math.min(1f, t));
            float inv = 1f - t;
            return 1f - inv * inv * inv;
        }

        /** Advances an interpolated value towards its target; frame-rate independent enough for Swing timers. */
        public static float approach(float current, float target, float rate) {
            float next = current + (target - current) * rate;
            return Math.abs(target - next) < 0.004f ? target : next;
        }
    }
}
