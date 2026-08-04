import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.Color;
import java.awt.Component;

public final class ComboStyle {
    private ComboStyle() {}

    public static void apply(JComboBox<?> cb) {
        cb.setFont(Theme.FONT);
        cb.setBackground(Color.WHITE);
        cb.setForeground(Theme.TEXT);
        cb.setRenderer(new BasicComboBoxRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? new Color(235, 238, 252) : Color.WHITE);
                setForeground(Theme.TEXT);
                setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                return this;
            }
        });
    }
}
