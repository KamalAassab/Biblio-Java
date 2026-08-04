import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

public class WrapLayout extends FlowLayout {
    public WrapLayout() {
        super(FlowLayout.LEFT, 20, 20);
    }

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return layoutSize(target, false);
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getWidth();

            if (targetWidth == 0 && target.getParent() != null) {
                targetWidth = target.getParent().getWidth();
            }
            if (targetWidth == 0) {
                targetWidth = 980;
            }

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);
            if (maxWidth <= 0) maxWidth = targetWidth;

            int x = 0;
            int y = insets.top + vgap;
            int rowHeight = 0;
            int reqWidth = 0;

            for (Component m : target.getComponents()) {
                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (x == 0 || (x + d.width <= maxWidth)) {
                        if (x > 0) x += hgap;
                        x += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    } else {
                        reqWidth = Math.max(reqWidth, x);
                        x = d.width;
                        y += vgap + rowHeight;
                        rowHeight = d.height;
                    }
                }
            }
            y += rowHeight + vgap + insets.bottom;
            reqWidth = Math.max(reqWidth, x);
            return new Dimension(reqWidth + insets.left + insets.right + hgap * 2, y);
        }
    }
}
