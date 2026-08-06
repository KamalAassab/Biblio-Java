import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * Vector icon set, drawn directly with Java2D.
 *
 * <p>Kept as code rather than image assets so icons stay crisp at any DPI, recolour
 * freely for hover and selected states, and add nothing to the packaged download.
 *
 * <p>All shapes are drawn on a normalised {@code s × s} box with a stroke weight
 * proportional to the size, so an icon at 16 px and the same icon at 28 px read as
 * members of one family.
 */
public final class Icons {

    private Icons() {}

    public enum Kind {
        // Primary navigation
        HOME, GRID, BOOK, CLOCK, BELL, USERS, USER, BOOKMARK,
        // Secondary navigation
        SETTINGS, HELP, LOGOUT, INFO,
        // Actions
        PLUS, SEARCH, EDIT, TRASH, CLOSE, CHECK, REFRESH, FILTER, SLIDERS, DOWNLOAD, HEART,
        // Affordances
        CALENDAR, ARROW, CHEVRON_DOWN, CHEVRON_RIGHT, CHEVRON_LEFT, GLOBE, LOCK, MAIL, PHONE, STAR,
        // Retained alias
        DASHBOARD
    }

    public static void paint(Graphics2D g, Kind k, int x, int y, int s, Color c) {
        Graphics2D gg = (Graphics2D) g.create();
        Theme.aa(gg);
        gg.setColor(c);
        float weight = Math.max(1.5f, s * 0.105f);
        gg.setStroke(new BasicStroke(weight, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        draw(gg, k, x, y, s, c);
        gg.dispose();
    }

    private static void draw(Graphics2D g, Kind k, int x, int y, int s, Color c) {
        switch (k) {
            case HOME: {
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * 0.12, y + s * 0.45);
                p.lineTo(x + s * 0.5, y + s * 0.12);
                p.lineTo(x + s * 0.88, y + s * 0.45);
                p.lineTo(x + s * 0.88, y + s * 0.86);
                p.lineTo(x + s * 0.12, y + s * 0.86);
                p.closePath();
                g.draw(p);
                g.draw(new java.awt.geom.Line2D.Float(
                        x + s * 0.40f, y + s * 0.86f, x + s * 0.40f, y + s * 0.56f));
                g.draw(new java.awt.geom.Line2D.Float(
                        x + s * 0.60f, y + s * 0.86f, x + s * 0.60f, y + s * 0.56f));
                g.draw(new java.awt.geom.Line2D.Float(
                        x + s * 0.40f, y + s * 0.56f, x + s * 0.60f, y + s * 0.56f));
                break;
            }
            case DASHBOARD:
            case GRID: {
                float gap = s * 0.14f;
                float sq = (s - gap) / 2f - s * 0.06f;
                float r = Math.max(2.5f, sq * 0.3f);
                float x0 = x + s * 0.09f, y0 = y + s * 0.09f;
                for (int i = 0; i < 4; i++) {
                    float cx = x0 + (i % 2) * (sq + gap);
                    float cy = y0 + (i / 2) * (sq + gap);
                    g.draw(new java.awt.geom.RoundRectangle2D.Float(cx, cy, sq, sq, r, r));
                }
                break;
            }
            case BOOK: {
                float bx = x + s * 0.18f, bw = s * 0.64f;
                g.draw(new java.awt.geom.RoundRectangle2D.Float(
                        bx, y + s * 0.1f, bw, s * 0.8f, s * 0.12f, s * 0.12f));
                g.draw(new java.awt.geom.Line2D.Float(
                        bx + bw * 0.28f, y + s * 0.1f, bx + bw * 0.28f, y + s * 0.9f));
                break;
            }
            case BOOKMARK: {
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * 0.24, y + s * 0.12);
                p.lineTo(x + s * 0.76, y + s * 0.12);
                p.lineTo(x + s * 0.76, y + s * 0.88);
                p.lineTo(x + s * 0.50, y + s * 0.66);
                p.lineTo(x + s * 0.24, y + s * 0.88);
                p.closePath();
                g.draw(p);
                break;
            }
            case CLOCK: {
                float r = s * 0.38f;
                float cx = x + s / 2f, cy = y + s / 2f;
                g.draw(new java.awt.geom.Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                g.draw(new java.awt.geom.Line2D.Float(cx, cy, cx, cy - r * 0.55f));
                g.draw(new java.awt.geom.Line2D.Float(cx, cy, cx + r * 0.48f, cy + r * 0.28f));
                break;
            }
            case BELL: {
                float cx = x + s / 2f;
                float w = s * 0.56f;
                g.draw(new java.awt.geom.Arc2D.Float(
                        cx - w / 2, y + s * 0.14f, w, w, 0, 180, java.awt.geom.Arc2D.OPEN));
                g.draw(new java.awt.geom.Line2D.Float(cx - w / 2, y + s * 0.42f, cx - w / 2, y + s * 0.68f));
                g.draw(new java.awt.geom.Line2D.Float(cx + w / 2, y + s * 0.42f, cx + w / 2, y + s * 0.68f));
                g.draw(new java.awt.geom.Line2D.Float(
                        cx - w * 0.72f, y + s * 0.68f, cx + w * 0.72f, y + s * 0.68f));
                g.draw(new java.awt.geom.Arc2D.Float(
                        cx - s * 0.11f, y + s * 0.68f, s * 0.22f, s * 0.22f, 180, 180,
                        java.awt.geom.Arc2D.OPEN));
                break;
            }
            case USERS:
                person(g, x + s * 0.36f, y + s * 0.5f, s * 0.3f);
                person(g, x + s * 0.66f, y + s * 0.5f, s * 0.28f);
                break;
            case USER:
                person(g, x + s * 0.5f, y + s * 0.5f, s * 0.36f);
                break;
            case SETTINGS: {
                float r = s * 0.19f;
                float cx = x + s / 2f, cy = y + s / 2f;
                g.draw(new java.awt.geom.Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4;
                    float ix = (float) (cx + Math.cos(a) * s * 0.30f);
                    float iy = (float) (cy + Math.sin(a) * s * 0.30f);
                    float ox = (float) (cx + Math.cos(a) * s * 0.42f);
                    float oy = (float) (cy + Math.sin(a) * s * 0.42f);
                    g.draw(new java.awt.geom.Line2D.Float(ix, iy, ox, oy));
                }
                break;
            }
            case HELP: {
                float r = s * 0.4f;
                float cx = x + s / 2f, cy = y + s / 2f;
                g.draw(new java.awt.geom.Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                g.draw(new java.awt.geom.Arc2D.Float(
                        cx - s * 0.14f, cy - s * 0.24f, s * 0.28f, s * 0.26f, 200, -230,
                        java.awt.geom.Arc2D.OPEN));
                g.draw(new java.awt.geom.Line2D.Float(cx, cy + s * 0.02f, cx, cy + s * 0.12f));
                dot(g, cx, cy + s * 0.26f, s * 0.055f, c);
                break;
            }
            case INFO: {
                float r = s * 0.4f;
                float cx = x + s / 2f, cy = y + s / 2f;
                g.draw(new java.awt.geom.Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                dot(g, cx, cy - s * 0.17f, s * 0.055f, c);
                g.draw(new java.awt.geom.Line2D.Float(cx, cy - s * 0.02f, cx, cy + s * 0.22f));
                break;
            }
            case LOGOUT: {
                g.draw(new java.awt.geom.Arc2D.Float(
                        x + s * 0.06f, y + s * 0.1f, s * 0.64f, s * 0.8f, 300, 120,
                        java.awt.geom.Arc2D.OPEN));
                g.draw(new java.awt.geom.Line2D.Float(x + s * 0.46f, y + s * 0.5f, x + s * 0.94f, y + s * 0.5f));
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * 0.76, y + s * 0.32);
                p.lineTo(x + s * 0.94, y + s * 0.5);
                p.lineTo(x + s * 0.76, y + s * 0.68);
                g.draw(p);
                break;
            }
            case PLUS: {
                float m = s / 2f, d = s * 0.28f;
                g.draw(new java.awt.geom.Line2D.Float(x + m - d, y + m, x + m + d, y + m));
                g.draw(new java.awt.geom.Line2D.Float(x + m, y + m - d, x + m, y + m + d));
                break;
            }
            case SEARCH: {
                float r = s * 0.27f;
                float cx = x + s * 0.44f, cy = y + s * 0.44f;
                g.draw(new java.awt.geom.Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                g.draw(new java.awt.geom.Line2D.Float(
                        cx + r * 0.72f, cy + r * 0.72f, x + s * 0.88f, y + s * 0.88f));
                break;
            }
            case EDIT: {
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * 0.16, y + s * 0.84);
                p.lineTo(x + s * 0.22, y + s * 0.62);
                p.lineTo(x + s * 0.66, y + s * 0.18);
                p.lineTo(x + s * 0.82, y + s * 0.34);
                p.lineTo(x + s * 0.38, y + s * 0.78);
                p.closePath();
                g.draw(p);
                break;
            }
            case TRASH: {
                g.draw(new java.awt.geom.Line2D.Float(x + s * 0.16f, y + s * 0.26f, x + s * 0.84f, y + s * 0.26f));
                g.draw(new java.awt.geom.Line2D.Float(x + s * 0.38f, y + s * 0.26f, x + s * 0.38f, y + s * 0.14f));
                g.draw(new java.awt.geom.Line2D.Float(x + s * 0.62f, y + s * 0.26f, x + s * 0.62f, y + s * 0.14f));
                g.draw(new java.awt.geom.Line2D.Float(x + s * 0.38f, y + s * 0.14f, x + s * 0.62f, y + s * 0.14f));
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * 0.24, y + s * 0.26);
                p.lineTo(x + s * 0.30, y + s * 0.88);
                p.lineTo(x + s * 0.70, y + s * 0.88);
                p.lineTo(x + s * 0.76, y + s * 0.26);
                g.draw(p);
                break;
            }
            case CLOSE:
                g.draw(new java.awt.geom.Line2D.Float(x + s * 0.24f, y + s * 0.24f, x + s * 0.76f, y + s * 0.76f));
                g.draw(new java.awt.geom.Line2D.Float(x + s * 0.76f, y + s * 0.24f, x + s * 0.24f, y + s * 0.76f));
                break;
            case CHECK: {
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * 0.18, y + s * 0.52);
                p.lineTo(x + s * 0.42, y + s * 0.74);
                p.lineTo(x + s * 0.84, y + s * 0.26);
                g.draw(p);
                break;
            }
            case REFRESH: {
                float r = s * 0.33f;
                float cx = x + s / 2f, cy = y + s / 2f;
                g.draw(new java.awt.geom.Arc2D.Float(
                        cx - r, cy - r, r * 2, r * 2, 55, 280, java.awt.geom.Arc2D.OPEN));
                Path2D p = new Path2D.Float();
                p.moveTo(cx + r * 0.15, cy - r * 1.05);
                p.lineTo(cx + r * 0.72, cy - r * 0.74);
                p.lineTo(cx + r * 0.18, cy - r * 0.30);
                g.draw(p);
                break;
            }
            case FILTER: {
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * 0.12, y + s * 0.22);
                p.lineTo(x + s * 0.88, y + s * 0.22);
                p.lineTo(x + s * 0.58, y + s * 0.54);
                p.lineTo(x + s * 0.58, y + s * 0.84);
                p.lineTo(x + s * 0.42, y + s * 0.74);
                p.lineTo(x + s * 0.42, y + s * 0.54);
                p.closePath();
                g.draw(p);
                break;
            }
            case SLIDERS: {
                for (int i = 0; i < 3; i++) {
                    float ly = y + s * (0.26f + i * 0.24f);
                    g.draw(new java.awt.geom.Line2D.Float(x + s * 0.14f, ly, x + s * 0.86f, ly));
                    float knob = x + s * (i == 1 ? 0.66f : 0.36f);
                    dot(g, knob, ly, s * 0.10f, c);
                }
                break;
            }
            case DOWNLOAD: {
                g.draw(new java.awt.geom.Line2D.Float(x + s * 0.5f, y + s * 0.14f, x + s * 0.5f, y + s * 0.64f));
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * 0.30, y + s * 0.46);
                p.lineTo(x + s * 0.50, y + s * 0.66);
                p.lineTo(x + s * 0.70, y + s * 0.46);
                g.draw(p);
                g.draw(new java.awt.geom.Line2D.Float(x + s * 0.18f, y + s * 0.84f, x + s * 0.82f, y + s * 0.84f));
                break;
            }
            case HEART: {
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * 0.5, y + s * 0.84);
                p.curveTo(x + s * 0.05, y + s * 0.52, x + s * 0.16, y + s * 0.12, x + s * 0.5, y + s * 0.34);
                p.curveTo(x + s * 0.84, y + s * 0.12, x + s * 0.95, y + s * 0.52, x + s * 0.5, y + s * 0.84);
                p.closePath();
                g.draw(p);
                break;
            }
            case CALENDAR: {
                g.draw(new java.awt.geom.RoundRectangle2D.Float(
                        x + s * 0.1f, y + s * 0.18f, s * 0.8f, s * 0.72f, s * 0.14f, s * 0.14f));
                g.draw(new java.awt.geom.Line2D.Float(x + s * 0.3f, y + s * 0.08f, x + s * 0.3f, y + s * 0.28f));
                g.draw(new java.awt.geom.Line2D.Float(x + s * 0.7f, y + s * 0.08f, x + s * 0.7f, y + s * 0.28f));
                g.draw(new java.awt.geom.Line2D.Float(x + s * 0.1f, y + s * 0.42f, x + s * 0.9f, y + s * 0.42f));
                break;
            }
            case ARROW: {
                g.draw(new java.awt.geom.Line2D.Float(x + s * 0.16f, y + s * 0.5f, x + s * 0.82f, y + s * 0.5f));
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * 0.58, y + s * 0.26);
                p.lineTo(x + s * 0.84, y + s * 0.5);
                p.lineTo(x + s * 0.58, y + s * 0.74);
                g.draw(p);
                break;
            }
            case CHEVRON_DOWN: {
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * 0.24, y + s * 0.40);
                p.lineTo(x + s * 0.50, y + s * 0.64);
                p.lineTo(x + s * 0.76, y + s * 0.40);
                g.draw(p);
                break;
            }
            case CHEVRON_RIGHT: {
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * 0.40, y + s * 0.24);
                p.lineTo(x + s * 0.64, y + s * 0.50);
                p.lineTo(x + s * 0.40, y + s * 0.76);
                g.draw(p);
                break;
            }
            case CHEVRON_LEFT: {
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * 0.60, y + s * 0.24);
                p.lineTo(x + s * 0.36, y + s * 0.50);
                p.lineTo(x + s * 0.60, y + s * 0.76);
                g.draw(p);
                break;
            }
            case GLOBE: {
                float r = s * 0.4f;
                float cx = x + s / 2f, cy = y + s / 2f;
                g.draw(new java.awt.geom.Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                g.draw(new java.awt.geom.Ellipse2D.Float(cx - r * 0.45f, cy - r, r * 0.9f, r * 2));
                g.draw(new java.awt.geom.Line2D.Float(cx - r, cy, cx + r, cy));
                break;
            }
            case LOCK: {
                g.draw(new java.awt.geom.RoundRectangle2D.Float(
                        x + s * 0.18f, y + s * 0.44f, s * 0.64f, s * 0.44f, s * 0.14f, s * 0.14f));
                g.draw(new java.awt.geom.Arc2D.Float(
                        x + s * 0.30f, y + s * 0.14f, s * 0.40f, s * 0.44f, 0, 180,
                        java.awt.geom.Arc2D.OPEN));
                break;
            }
            case MAIL: {
                g.draw(new java.awt.geom.RoundRectangle2D.Float(
                        x + s * 0.1f, y + s * 0.22f, s * 0.8f, s * 0.56f, s * 0.12f, s * 0.12f));
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * 0.14, y + s * 0.28);
                p.lineTo(x + s * 0.5, y + s * 0.56);
                p.lineTo(x + s * 0.86, y + s * 0.28);
                g.draw(p);
                break;
            }
            case PHONE: {
                g.draw(new java.awt.geom.RoundRectangle2D.Float(
                        x + s * 0.26f, y + s * 0.1f, s * 0.48f, s * 0.8f, s * 0.14f, s * 0.14f));
                g.draw(new java.awt.geom.Line2D.Float(x + s * 0.44f, y + s * 0.78f, x + s * 0.56f, y + s * 0.78f));
                break;
            }
            case STAR: {
                Path2D p = new Path2D.Float();
                for (int i = 0; i < 10; i++) {
                    double a = Math.PI / 2 * 3 + Math.PI * i / 5;
                    float rr = (i % 2 == 0) ? s * 0.42f : s * 0.18f;
                    float px = (float) (x + s / 2f + Math.cos(a) * rr);
                    float py = (float) (y + s / 2f + Math.sin(a) * rr);
                    if (i == 0) p.moveTo(px, py);
                    else p.lineTo(px, py);
                }
                p.closePath();
                g.draw(p);
                break;
            }
        }
    }

    /** Filled variant, for selected chips where a solid mark reads more clearly than an outline. */
    public static void paintFilled(Graphics2D g, Kind k, int x, int y, int s, Color c) {
        Graphics2D gg = (Graphics2D) g.create();
        Theme.aa(gg);
        gg.setColor(c);
        gg.setStroke(new BasicStroke(Math.max(1.8f, s * 0.13f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        draw(gg, k, x, y, s, c);
        gg.dispose();
    }

    private static void person(Graphics2D g, float cx, float cy, float r) {
        float head = r * 0.42f;
        g.draw(new java.awt.geom.Ellipse2D.Float(cx - head, cy - r * 0.85f, head * 2, head * 2));
        g.draw(new java.awt.geom.Arc2D.Float(
                cx - r * 0.82f, cy + r * 0.02f, r * 1.64f, r * 1.5f, 0, 180,
                java.awt.geom.Arc2D.OPEN));
    }

    private static void dot(Graphics2D g, float cx, float cy, float r, Color c) {
        g.setColor(c);
        g.fill(new java.awt.geom.Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
    }
}
