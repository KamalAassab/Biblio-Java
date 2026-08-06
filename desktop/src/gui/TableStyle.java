import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Table presentation: tall rows, a quiet header, and status expressed as tinted pills
 * rather than coloured text.
 *
 * <p>Rows are deliberately spacious. These tables are read at a glance to answer "what
 * needs attention", not scanned for density, so vertical rhythm matters more than
 * fitting more rows on screen.
 */
public final class TableStyle {

    private TableStyle() {}

    public static final int ROW_HEIGHT = 60;

    public static void apply(JTable table) {
        table.setRowHeight(ROW_HEIGHT);
        table.setFont(Theme.BODY);
        table.setForeground(Theme.TEXT);
        table.setBackground(Theme.SURFACE);
        table.setOpaque(false);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(Theme.PRIMARY_SOFT);
        table.setSelectionForeground(Theme.TEXT);
        table.setRowSelectionAllowed(true);
        table.setFocusable(false);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        header.setPreferredSize(new Dimension(0, 48));
        header.setDefaultRenderer(new HeaderRenderer());
        header.setBorder(new EmptyBorder(0, 0, 0, 0));

        table.setDefaultRenderer(Object.class, new BodyRenderer());
    }

    /** Quiet uppercase column labels on the sunken surface tone. */
    private static class HeaderRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean selected, boolean focused,
                                                       int row, int column) {
            return new JLabel(String.valueOf(value)) {
                @Override
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    Theme.aa(g);
                    g.setColor(Theme.SURFACE_SUNK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Theme.DIVIDER);
                    g.fillRect(0, getHeight() - 1, getWidth(), 1);

                    g.setFont(Theme.EYEBROW);
                    FontMetrics fm = g.getFontMetrics();
                    g.setColor(Theme.MUTED);
                    g.drawString(getText().toUpperCase(), 18,
                            (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                    g.dispose();
                }
            };
        }
    }

    /** Body cells, with zebra-free separation carried by hairline rules. */
    private static class BodyRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean selected, boolean focused,
                                                       int row, int column) {
            if (value instanceof ChipText chip) {
                return new ChipCell(chip, selected);
            }
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, false, row, column);
            label.setBorder(new EmptyBorder(0, 18, 0, 12));
            label.setOpaque(false);

            boolean strong = value instanceof StrongText;
            label.setText(value == null ? "" : value.toString());
            label.setFont(strong ? Theme.BODY_BOLD : Theme.BODY);
            label.setForeground(strong ? Theme.TEXT : Theme.TEXT_SOFT);

            return new CellWrapper(label, selected);
        }
    }

    /** Paints the row background and separator behind whatever the cell renders. */
    private static class CellWrapper extends javax.swing.JPanel {
        private final boolean selected;

        CellWrapper(Component inner, boolean selected) {
            super(new java.awt.BorderLayout());
            this.selected = selected;
            setOpaque(false);
            add(inner, java.awt.BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            g.setColor(selected ? Theme.PRIMARY_SOFT : Theme.SURFACE);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Theme.DIVIDER);
            g.fillRect(0, getHeight() - 1, getWidth(), 1);
            g.dispose();
        }
    }

    /** Renders a {@link ChipText} value as a tinted pill. */
    private static class ChipCell extends javax.swing.JComponent {
        private final ChipText chip;
        private final boolean selected;

        ChipCell(ChipText chip, boolean selected) {
            this.chip = chip;
            this.selected = selected;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            g.setColor(selected ? Theme.PRIMARY_SOFT : Theme.SURFACE);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Theme.DIVIDER);
            g.fillRect(0, getHeight() - 1, getWidth(), 1);

            g.setFont(Theme.SMALL_BOLD);
            FontMetrics fm = g.getFontMetrics();
            int pw = fm.stringWidth(chip.text) + 30;
            int ph = 30;
            int px = 18;
            int py = (getHeight() - ph) / 2;

            Theme.fillRound(g, px, py, pw, ph, Theme.PILL, chip.background);
            g.setColor(chip.foreground);
            g.fillOval(px + 12, py + ph / 2 - 3, 6, 6);
            g.drawString(chip.text, px + 24, py + (ph - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    // ── Cell value wrappers ──────────────────────────────────────────────────

    /** Wrap a value in this to render it as a tinted status pill. */
    public static class ChipText {
        final String text;
        final Color background;
        final Color foreground;

        public ChipText(String text, Color background, Color foreground) {
            this.text = text == null ? "" : text;
            this.background = background;
            this.foreground = foreground;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** Wrap a value in this to render it in the emphasised body weight. */
    public static class StrongText {
        final String text;

        public StrongText(String text) {
            this.text = text == null ? "" : text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public static ChipText success(String text) {
        return new ChipText(text, Theme.SUCCESS_SOFT, new Color(0x11, 0x6B, 0x3D));
    }

    public static ChipText danger(String text) {
        return new ChipText(text, Theme.DANGER_SOFT, new Color(0xA3, 0x2C, 0x24));
    }

    public static ChipText warning(String text) {
        return new ChipText(text, Theme.AMBER_SOFT, new Color(0x92, 0x51, 0x05));
    }

    public static ChipText info(String text) {
        return new ChipText(text, Theme.PRIMARY_SOFT, Theme.PRIMARY);
    }

    public static ChipText neutral(String text) {
        return new ChipText(text, Theme.SURFACE_CHIP, Theme.MUTED);
    }
}
