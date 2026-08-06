import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The primary search control: a white pill holding a category selector, a divider,
 * a free-text field, and a filled action button — the composite from the reference layout.
 *
 * <p>Typing is debounced before the query fires, so filtering a large catalogue does
 * not re-run on every keystroke.
 */
public class SearchBar extends JPanel {

    private final JTextField field = new JTextField();
    private final CategoryButton category;
    private final ActionButton button;
    private BiConsumer<String, String> onSearch;
    private final Timer debounce;

    private static final int HEIGHT = 62;
    private static final int CATEGORY_WIDTH = 168;

    public SearchBar(String placeholder, List<String> categories, String allLabel) {
        setOpaque(false);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(760, HEIGHT));
        setMinimumSize(new Dimension(420, HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));

        category = new CategoryButton(categories, allLabel);
        add(category, BorderLayout.WEST);

        field.setBorder(new EmptyBorder(0, 46, 0, 12));
        field.setOpaque(false);
        field.setFont(Theme.BODY);
        field.setForeground(Theme.TEXT);
        field.setCaretColor(Theme.PRIMARY);
        field.putClientProperty("placeholder", placeholder);
        add(field, BorderLayout.CENTER);

        button = new ActionButton();
        add(button, BorderLayout.EAST);

        debounce = new Timer(220, e -> fire());
        debounce.setRepeats(false);

        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                debounce.restart();
                repaint();
            }

            public void removeUpdate(DocumentEvent e) {
                debounce.restart();
                repaint();
            }

            public void changedUpdate(DocumentEvent e) {
                debounce.restart();
                repaint();
            }
        });
        field.addActionListener(e -> fire());
        button.onClick = this::fire;
    }

    /** Receives (query, category) — category is {@code null} when "all" is selected. */
    public SearchBar onSearch(BiConsumer<String, String> handler) {
        this.onSearch = handler;
        return this;
    }

    public void setCategories(List<String> categories) {
        category.setCategories(categories);
    }

    public String getQuery() {
        return field.getText().trim();
    }

    public void setQuery(String q) {
        field.setText(q == null ? "" : q);
    }

    public String getCategory() {
        return category.selected;
    }

    public void reset() {
        field.setText("");
        category.selected = null;
        category.repaint();
    }

    private void fire() {
        if (onSearch != null) onSearch.accept(getQuery(), category.selected);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);
        int w = getWidth();
        int h = getHeight();

        Theme.shadow(g, 2, 2, w - 4, h - 4, Theme.PILL, 9, 4, 30);
        Theme.fillRound(g, 2, 2, w - 4, h - 4, Theme.PILL, Theme.SURFACE);

        // Divider between the category selector and the query field.
        g.setColor(Theme.BORDER_SOFT);
        g.fillRect(CATEGORY_WIDTH, 16, 1, h - 32);

        // Magnifier inside the text field.
        Icons.paint(g, Icons.Kind.SEARCH, CATEGORY_WIDTH + 18, (h - 19) / 2, 19, Theme.MUTED);

        if (field.getText().isEmpty() && !field.hasFocus()) {
            Object ph = field.getClientProperty("placeholder");
            if (ph != null) {
                g.setFont(Theme.BODY);
                g.setColor(Theme.FAINT);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(String.valueOf(ph), CATEGORY_WIDTH + 50,
                        (h - fm.getHeight()) / 2 + fm.getAscent());
            }
        }
        g.dispose();
    }

    /** Left segment: opens a menu of catalogue categories. */
    private class CategoryButton extends JPanel {
        private List<String> categories;
        private final String allLabel;
        private String selected;
        private boolean hovered;

        CategoryButton(List<String> categories, String allLabel) {
            this.categories = new ArrayList<>(categories);
            this.allLabel = allLabel;
            setOpaque(false);
            setPreferredSize(new Dimension(CATEGORY_WIDTH, HEIGHT));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (contains(e.getPoint())) showMenu();
                }
            });
        }

        void setCategories(List<String> next) {
            this.categories = new ArrayList<>(next);
            if (selected != null && !this.categories.contains(selected)) selected = null;
            repaint();
        }

        private void showMenu() {
            JPopupMenu menu = new JPopupMenu();
            menu.setBorder(new EmptyBorder(6, 6, 6, 6));
            menu.setBackground(Theme.SURFACE);

            JMenuItem all = styled(allLabel);
            all.addActionListener(e -> {
                selected = null;
                repaint();
                fire();
            });
            menu.add(all);

            for (String c : categories) {
                JMenuItem item = styled(c);
                item.addActionListener(e -> {
                    selected = c;
                    repaint();
                    fire();
                });
                menu.add(item);
            }
            menu.show(this, 10, HEIGHT - 6);
        }

        private JMenuItem styled(String text) {
            JMenuItem item = new JMenuItem(text);
            item.setFont(Theme.BODY);
            item.setForeground(Theme.TEXT);
            item.setBackground(Theme.SURFACE);
            item.setBorder(new EmptyBorder(9, 12, 9, 22));
            return item;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            int h = getHeight();

            if (hovered) {
                Theme.fillRound(g, 8, 10, CATEGORY_WIDTH - 18, h - 20, Theme.PILL,
                        Theme.alpha(Theme.PRIMARY, 12));
            }

            g.setFont(Theme.BODY_MEDIUM);
            FontMetrics fm = g.getFontMetrics();
            String label = selected != null ? selected : allLabel;
            label = BookCover.ellipsise(label, fm, CATEGORY_WIDTH - 70);
            g.setColor(selected != null ? Theme.PRIMARY : Theme.TEXT_SOFT);
            g.drawString(label, 22, (h - fm.getHeight()) / 2 + fm.getAscent());

            Icons.paint(g, Icons.Kind.CHEVRON_DOWN, CATEGORY_WIDTH - 38, (h - 16) / 2, 16, Theme.MUTED);
            g.dispose();
        }
    }

    /** Right segment: the filled submit button. */
    private static class ActionButton extends JPanel {
        Runnable onClick;
        private boolean hovered;
        private float t;
        private final Timer animator;

        ActionButton() {
            setOpaque(false);
            setPreferredSize(new Dimension(138, HEIGHT));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            animator = new Timer(Theme.Motion.TICK_MS, e -> {
                float target = hovered ? 1f : 0f;
                t = Theme.Motion.approach(t, target, Theme.Motion.FAST);
                if (t == target) ((Timer) e.getSource()).stop();
                repaint();
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    if (!animator.isRunning()) animator.start();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    if (!animator.isRunning()) animator.start();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (contains(e.getPoint()) && onClick != null) onClick.run();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            int w = getWidth();
            int h = getHeight();
            float p = Theme.Motion.easeOut(t);

            int bw = w - 20;
            int bh = h - 20;
            Color fill = Theme.mix(Theme.PRIMARY, Theme.PRIMARY_2, p);
            Theme.shadow(g, 8, 10, bw, bh, Theme.PILL, Math.round(6 + 4 * p), 3, 34);
            Theme.fillRound(g, 8, 10, bw, bh, Theme.PILL, fill);

            g.setFont(Theme.BODY_BOLD);
            FontMetrics fm = g.getFontMetrics();
            String label = I18n.t("action.search");
            g.setColor(Color.WHITE);
            g.drawString(label, 8 + (bw - fm.stringWidth(label)) / 2,
                    10 + (bh - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }
}
