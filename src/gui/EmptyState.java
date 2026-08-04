import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;

public class EmptyState extends JPanel {
    public EmptyState(String title, String sub) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(CENTER_ALIGNMENT);

        Avatar icon = new Avatar("0", 84, new Color(226, 228, 252), new Color(211, 214, 250));
        icon.setAlignmentX(CENTER_ALIGNMENT);

        JLabel t = new JLabel(title, JLabel.CENTER);
        t.setFont(Theme.H2);
        t.setForeground(Theme.TEXT);
        t.setAlignmentX(CENTER_ALIGNMENT);

        JLabel s = new JLabel(sub, JLabel.CENTER);
        s.setFont(Theme.SMALL);
        s.setForeground(Theme.MUTED);
        s.setAlignmentX(CENTER_ALIGNMENT);

        add(Box.createVerticalGlue());
        add(icon);
        add(Box.createVerticalStrut(18));
        add(t);
        add(Box.createVerticalStrut(8));
        add(s);
        add(Box.createVerticalGlue());
    }
}
