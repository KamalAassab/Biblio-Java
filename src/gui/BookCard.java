import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;

public class BookCard extends JPanel {
    private final Livre livre;
    private final boolean admin;
    private final Runnable onOpen;
    private float t;
    private boolean over;
    private Timer anim;

    public BookCard(Livre livre, boolean admin, Runnable onOpen) {
        this.livre = livre;
        this.admin = admin;
        this.onOpen = onOpen;
        setOpaque(false);
        setPreferredSize(new Dimension(238, 322));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                over = true;
                start();
            }

            public void mouseExited(MouseEvent e) {
                over = false;
                start();
            }

            public void mouseClicked(MouseEvent e) {
                if (onOpen != null) onOpen.run();
            }
        });
        anim = new Timer(16, e -> {
            float target = over ? 1f : 0f;
            t += (target - t) * 0.22f;
            if (Math.abs(target - t) < 0.01f) {
                t = target;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
    }

    private void start() {
        if (!anim.isRunning()) anim.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int r = 20;
        int bx = 2, by = 3, bw = w - 6, bh = h - 7;

        g.setColor(new Color(15, 23, 42, Math.round(22 + 18 * t)));
        g.fillRoundRect(bx + 3, by + 6, bw, bh, r * 2, r * 2);
        Theme.fillRound(g, bx, by, bw, bh, r, Color.WHITE);

        if (t > 0) {
            g.setStroke(new BasicStroke(1.8f));
            g.setColor(new Color(Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(), Theme.PRIMARY.getBlue(), Math.round(150 * t)));
            g.drawRoundRect(bx, by, bw, bh, r * 2, r * 2);
        }

        int bandH = 80;
        g.setClip(new RoundRectangle2D.Float(bx, by, bw, bh, r * 2, r * 2));
        Color[] gr = Theme.genreGradient(livre.getGenre());
        Color soft0 = Theme.mix(gr[0], Color.WHITE, 0.15f);
        Color soft1 = Theme.mix(gr[1], Color.WHITE, 0.15f);
        g.setPaint(new java.awt.GradientPaint(0, by, soft0, bw, by + bandH, soft1, true));
        g.fillRoundRect(bx, by, bw, bandH + r, r * 2, r * 2);
        g.setColor(new Color(255, 255, 255, 60));
        g.fillRoundRect(bx, by + bandH - 26, bw, 26 + r, r * 2, r * 2);

        Icons.paint(g, Icons.Kind.BOOK, bx + 16, by + 16, 26, new Color(255, 255, 255, 225));

        boolean dispo = livre.estDisponible();
        String badge = dispo ? "Disponible" : "Emprunté";
        g.setFont(Theme.SMALL_BOLD);
        FontMetrics bfm = g.getFontMetrics();
        int badW = bfm.stringWidth(badge) + 22;
        int badX = bx + bw - badW - 14, badY = by + 16;
        g.setColor(new Color(255, 255, 255, 55));
        g.fillRoundRect(badX, badY, badW, 26, 13, 13);
        g.setColor(Color.WHITE);
        g.drawString(badge, badX + 11, badY + 18);

        g.setClip(null);

        int txtX = bx + 18;
        int y1 = by + bandH + 38;
        g.setFont(Theme.BOLD_15);
        g.setColor(Theme.TEXT);
        FontMetrics fm = g.getFontMetrics();
        String[] lines = wrap(livre.getTitre(), fm, bw - 36, 2);
        g.drawString(lines.length > 0 ? lines[0] : "", txtX, y1);
        if (lines.length > 1) g.drawString(lines[1], txtX, y1 + 22);

        g.setFont(Theme.FONT);
        g.setColor(Theme.MUTED);
        g.drawString(livre.getAuteur(), txtX, y1 + 48);

        String genre = livre.getGenre() == null || livre.getGenre().isEmpty() ? "Général" : livre.getGenre();
        g.setFont(Theme.SMALL_BOLD);
        FontMetrics gfm = g.getFontMetrics();
        int pw = gfm.stringWidth(genre) + 24;
        int py = by + bh - 40;
        g.setColor(new Color(232, 240, 251));
        g.fillRoundRect(txtX, py, pw, 26, 13, 13);
        g.setColor(Theme.PRIMARY);
        g.drawString(genre, txtX + 12, py + 18);

        g.setColor(Theme.mix(Theme.MUTED, Theme.PRIMARY, t));
        Icons.paint(g, Icons.Kind.ARROW, bx + bw - 34, py + 4, 18, g.getColor());
        g.dispose();
    }

    private static String[] wrap(String text, FontMetrics fm, int maxW, int maxLines) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (line.length() > 0 && fm.stringWidth(line.toString() + c) > maxW) {
                lines.add(line.toString());
                line = new StringBuilder();
                if (lines.size() >= maxLines) break;
            }
            line.append(c);
        }
        if (lines.size() < maxLines && line.length() > 0) lines.add(line.toString());
        if (lines.size() < maxLines) {
            while (lines.size() < maxLines) lines.add("");
        }
        return lines.toArray(new String[0]);
    }
}
