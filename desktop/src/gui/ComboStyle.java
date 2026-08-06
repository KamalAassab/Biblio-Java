import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Applies the application's field styling to a {@link JComboBox}. */
public final class ComboStyle {

    private ComboStyle() {}

    public static void apply(JComboBox<?> combo) {
        combo.setFont(Theme.BODY);
        combo.setForeground(Theme.TEXT);
        combo.setBackground(Theme.FIELD);
        combo.setOpaque(false);
        combo.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        combo.setPreferredSize(new Dimension(240, 52));
        combo.setFocusable(false);
        combo.setMaximumRowCount(9);

        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                // The arrow is painted as part of the field, so the default button is suppressed.
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setVisible(false);
                return b;
            }

            @Override
            public void paint(Graphics g0, JComponent c) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.aa(g);
                int w = c.getWidth();
                int h = c.getHeight();

                Theme.fillRound(g, 0, 0, w, h, Theme.RADIUS_SM, Theme.FIELD);
                g.setColor(Theme.BORDER);
                g.drawRoundRect(0, 0, w - 1, h - 1, Theme.RADIUS_SM * 2, Theme.RADIUS_SM * 2);
                Icons.paint(g, Icons.Kind.CHEVRON_DOWN, w - 34, (h - 16) / 2, 16, Theme.MUTED);
                g.dispose();

                // Let the superclass render the selected value on top.
                super.paint(g0, c);
            }

            @Override
            protected ComboPopup createPopup() {
                ComboPopup popup = super.createPopup();
                if (popup instanceof JComponent jc) {
                    jc.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                    jc.setBackground(Theme.SURFACE);
                }
                return popup;
            }
        });

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                Component c = super.getListCellRendererComponent(
                        list, value, index, selected, focused);
                setFont(Theme.BODY);
                setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
                setOpaque(true);
                if (index < 0) {
                    // The closed-field rendering sits inside the painted pill.
                    setBackground(new Color(0, 0, 0, 0));
                    setOpaque(false);
                    setForeground(Theme.TEXT);
                } else {
                    setBackground(selected ? Theme.PRIMARY_SOFT : Theme.SURFACE);
                    setForeground(Theme.TEXT);
                }
                return c;
            }
        });
    }
}
