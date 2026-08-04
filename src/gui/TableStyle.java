import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class TableStyle {
    private TableStyle() {}

    public static JTable create(String[] cols, Object[][] rows, Color[] textColors) {
        DefaultTableModel model = new DefaultTableModel(rows, cols) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable t = new JTable(model);
        t.setRowHeight(48);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(216, 230, 246));
        t.setSelectionForeground(Theme.TEXT);
        t.setFont(Theme.FONT);
        t.setForeground(Theme.TEXT);
        t.setFocusable(false);
        t.setOpaque(true);
        t.setBackground(Color.WHITE);
        t.setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0));
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setResizingAllowed(false);
        h.setPreferredSize(new Dimension(0, 44));
        h.setBackground(new Color(0xF7, 0xF9, 0xFC));
        h.setForeground(Theme.MUTED);
        h.setFont(Theme.SMALL_BOLD);
        h.setDefaultRenderer(new HeaderRenderer());
        t.setDefaultRenderer(Object.class, new BodyRenderer(textColors));
        return t;
    }

    static class HeaderRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBackground(new Color(0xF7, 0xF9, 0xFC));
            setForeground(Theme.MUTED);
            setFont(Theme.SMALL_BOLD);
            setBorder(new javax.swing.border.CompoundBorder(
                new javax.swing.border.MatteBorder(0, 0, 1, 0, new Color(0xE1, 0xE7, 0xF0)),
                new javax.swing.border.EmptyBorder(0, 18, 0, 18)));
            return this;
        }
    }

    static class BodyRenderer extends DefaultTableCellRenderer {
        private final Color[] colors;

        BodyRenderer(Color[] colors) {
            this.colors = colors;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            boolean lastRow = row == table.getRowCount() - 1;
            super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            setOpaque(true);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xFA, 0xFB, 0xFE));
            } else {
                setBackground(new Color(216, 230, 246));
            }
            Color c = colors != null && column < colors.length && colors[column] != null ? colors[column] : Theme.TEXT;
            if (value instanceof ChipText) {
                ChipText ct = (ChipText) value;
                setText(ct.text);
                c = ct.color;
            } else if (value instanceof ColorText) {
                ColorText ct = (ColorText) value;
                setText(ct.text);
                c = ct.color;
            } else {
                setText(value == null ? "" : value.toString());
            }
            setForeground(c);
            setFont(Theme.FONT);
            javax.swing.border.Border bottom = lastRow
                ? new javax.swing.border.EmptyBorder(0, 0, 0, 0)
                : new javax.swing.border.MatteBorder(0, 18, 1, 0, new Color(0xEC, 0xF0, 0xF6));
            setBorder(new javax.swing.border.CompoundBorder(
                bottom,
                new javax.swing.border.EmptyBorder(0, 18, 0, 18)));
            return this;
        }
    }

    public static class ColorText {
        public final String text;
        public final Color color;

        public ColorText(String text, Color color) {
            this.text = text;
            this.color = color;
        }
    }

    public static class ChipText {
        public final String text;
        public final Color color;
        public final Color bg;

        public ChipText(String text, Color color, Color bg) {
            this.text = text;
            this.color = color;
            this.bg = bg;
        }
    }

    public static class ChipCellRenderer extends DefaultTableCellRenderer {
        private Color chipBg;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            boolean lastRow = row == table.getRowCount() - 1;
            chipBg = null;
            if (value instanceof ChipText) {
                ChipText ct = (ChipText) value;
                lbl.setText(ct.text);
                lbl.setForeground(ct.color);
                lbl.setFont(Theme.SMALL_BOLD);
                chipBg = ct.bg;
            } else {
                lbl.setText(value == null ? "" : value.toString());
                lbl.setForeground(Theme.TEXT);
                lbl.setFont(Theme.FONT);
            }
            if (!isSelected) {
                lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xFA, 0xFB, 0xFE));
            } else {
                lbl.setBackground(new Color(216, 230, 246));
            }
            javax.swing.border.Border bottom = lastRow
                ? javax.swing.border.BorderFactory.createEmptyBorder()
                : new javax.swing.border.MatteBorder(0, 14, 1, 0, new Color(0xEC, 0xF0, 0xF6));
            lbl.setBorder(new javax.swing.border.CompoundBorder(
                bottom,
                new javax.swing.border.EmptyBorder(0, 14, 0, 14)));
            lbl.setOpaque(false);
            return lbl;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (chipBg != null) {
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(getText());
                int chipH = 24;
                int chipW = textW + 16;
                int chipX = 14;
                int chipY = (getHeight() - chipH) / 2;
                g2.setColor(chipBg);
                g2.fillRoundRect(chipX, chipY, chipW, chipH, chipH, chipH);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
