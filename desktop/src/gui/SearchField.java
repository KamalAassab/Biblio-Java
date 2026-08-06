import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class SearchField extends RoundedTextField {
    public SearchField() {
        super("Rechercher un livre, un auteur, un genre...");
        setBorder(new EmptyBorder(12, 44, 12, 16));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Theme.MUTED);
        int cx = 20, cy = getHeight() / 2, r = 6;
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        g.drawLine(cx + r, cy + r, cx + r + 5, cy + r + 5);
        g.dispose();
    }
}
