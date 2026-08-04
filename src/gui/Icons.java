import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public final class Icons {
    private Icons() {}

    public enum Kind {
        DASHBOARD, BOOK, CLOCK, BELL, USERS, PLUS, LOGOUT, SEARCH,
        EDIT, TRASH, CLOSE, CALENDAR, CHECK, ARROW, REFRESH
    }

    public static void paint(Graphics2D g, Kind k, int x, int y, int s, Color c) {
        g.setColor(c);
        g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (k) {
            case DASHBOARD: {
                int gx = s / 6;
                int sq = (s - gx) / 2;
                int r = Math.max(3, sq / 3);
                g.drawRoundRect(x, y, sq, sq, r, r);
                g.drawRoundRect(x + sq + gx, y, sq, sq, r, r);
                g.drawRoundRect(x, y + sq + gx, sq, sq, r, r);
                g.drawRoundRect(x + sq + gx, y + sq + gx, sq, sq, r, r);
                break;
            }
            case BOOK: {
                int bw = s * 3 / 4;
                int bx = x + (s - bw) / 2;
                g.drawRoundRect(bx, y, bw, s, 4, 4);
                g.drawLine(bx + bw / 2, y, bx + bw / 2, y + s);
                g.drawLine(bx + 4, y + s / 4, bx + bw / 2 - 4, y + s / 4);
                g.drawLine(bx + 4, y + s / 2, bx + bw / 2 - 4, y + s / 2);
                break;
            }
            case CLOCK: {
                int cx = x + s / 2, cy = y + s / 2, r = s / 2 - 1;
                g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
                g.drawLine(cx, cy, cx, cy - r + 3);
                g.drawLine(cx, cy, cx + r - 4, cy + r / 3);
                break;
            }
            case BELL: {
                int cx = x + s / 2;
                int w2 = s * 7 / 10, top = y + s / 6;
                g.drawArc(cx - w2 / 2, top, w2, s * 2 / 3, 0, 180);
                g.drawLine(cx - w2 / 2, top + s * 3 / 8, cx - w2 / 2, y + s * 7 / 10);
                g.drawLine(cx + w2 / 2, top + s * 3 / 8, cx + w2 / 2, y + s * 7 / 10);
                g.drawLine(cx - w2 / 2, y + s * 7 / 10, cx + w2 / 2, y + s * 7 / 10);
                g.fillOval(cx - s / 8, y + s * 7 / 10, s / 4, s / 4);
                break;
            }
            case USERS:
                drawPerson(g, x + s * 7 / 20, y + s * 3 / 10, s * 3 / 10);
                drawPerson(g, x + s * 13 / 20, y + s * 2 / 10, s * 3 / 10);
                break;
            case PLUS: {
                int m = s / 2, d = s * 3 / 10;
                g.drawLine(x + m - d, y + m, x + m + d, y + m);
                g.drawLine(x + m, y + m - d, x + m, y + m + d);
                break;
            }
            case LOGOUT: {
                g.drawRoundRect(x, y + s / 5, s * 3 / 5, s * 3 / 5, 3, 3);
                g.drawLine(x + s * 7 / 12, y + s / 2, x + s, y + s / 2);
                g.drawLine(x + s * 9 / 12, y + s * 5 / 16, x + s, y + s / 2);
                g.drawLine(x + s * 9 / 12, y + s * 11 / 16, x + s, y + s / 2);
                break;
            }
            case SEARCH: {
                int cx = x + s * 9 / 20, cy = y + s * 9 / 20, r = s / 4;
                g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
                g.drawLine(cx + r - 1, cy + r - 1, cx + r + s / 8, cy + r + s / 8);
                break;
            }
            case EDIT: {
                int x1 = x + s / 4, y1 = y + s * 3 / 4;
                int x2 = x + s * 3 / 4, y2 = y + s / 4;
                g.drawLine(x1, y1, x2, y2);
                g.drawLine(x2, y2, x2 + s / 5, y2 - s / 6);
                g.drawLine(x2, y2, x2 + s / 8, y2 + s / 8);
                g.drawLine(x2 + s / 5, y2 - s / 6, x2 + s / 8, y2 + s / 8);
                g.drawLine(x1, y1, x1 - s / 8, y1 - s / 12);
                break;
            }
            case TRASH: {
                g.drawLine(x + s / 4, y + s / 5, x + s * 3 / 4, y + s / 5);
                g.drawLine(x + s * 3 / 8, y + s / 5, x + s * 3 / 8, y + s / 10);
                g.drawLine(x + s * 5 / 8, y + s / 5, x + s * 5 / 8, y + s / 10);
                g.drawRoundRect(x + s / 4, y + s / 4, s / 2, s * 3 / 5, 2, 2);
                g.drawLine(x + s * 2 / 5, y + s * 2 / 5, x + s * 2 / 5, y + s * 3 / 4);
                g.drawLine(x + s * 3 / 5, y + s * 2 / 5, x + s * 3 / 5, y + s * 3 / 4);
                break;
            }
            case CLOSE: {
                g.drawLine(x + s / 4, y + s / 4, x + s * 3 / 4, y + s * 3 / 4);
                g.drawLine(x + s * 3 / 4, y + s / 4, x + s / 4, y + s * 3 / 4);
                break;
            }
            case CALENDAR: {
                g.drawRoundRect(x, y + s / 6, s, s * 5 / 6, 4, 4);
                g.drawLine(x + s / 6, y + s / 6, x + s / 6, y);
                g.drawLine(x + s * 5 / 6, y + s / 6, x + s * 5 / 6, y);
                g.drawLine(x + s / 6, y + s * 2 / 5, x + s * 5 / 6, y + s * 2 / 5);
                break;
            }
            case CHECK: {
                g.drawLine(x + s / 6, y + s / 2, x + s * 2 / 5, y + s * 7 / 10);
                g.drawLine(x + s * 2 / 5, y + s * 7 / 10, x + s * 5 / 6, y + s / 4);
                break;
            }
            case ARROW: {
                g.drawLine(x + s / 4, y + s / 2, x + s * 3 / 4, y + s / 2);
                g.drawLine(x + s * 3 / 4, y + s / 2, x + s * 9 / 16, y + s / 4);
                g.drawLine(x + s * 3 / 4, y + s / 2, x + s * 9 / 16, y + s * 3 / 4);
                break;
            }
            case REFRESH: {
                int cx = x + s / 2, cy = y + s / 2, r = s / 3;
                g.drawArc(cx - r, cy - r, 2 * r, 2 * r, 40, 290);
                int ax = cx, ay = cy - r - 1;
                g.drawLine(ax, ay - 3, ax - 3, ay + 4);
                g.drawLine(ax, ay - 3, ax + 3, ay + 4);
                break;
            }
        }
    }

    private static void drawPerson(Graphics2D g, int cx, int cy, int r) {
        int hr = r / 3;
        g.fillOval(cx - hr, cy, hr * 2, hr * 2);
        g.fillRoundRect(cx - r, cy + hr * 2, r * 2, r - hr, r, r);
    }
}
