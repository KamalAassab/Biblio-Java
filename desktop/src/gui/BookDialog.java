import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;

/**
 * Create, view and edit a book.
 *
 * <p>One dialog serves all three modes: read-only viewing shows the composed cover
 * beside the details, while create and edit swap in the form. Keeping them together
 * means the cover artwork and field layout cannot drift apart.
 */
public class BookDialog extends JDialog {

    private enum Mode { CREATE, VIEW, EDIT }

    private final Mode mode;
    private Livre livre;
    private final boolean isAdmin;
    private final Runnable onDone;

    private final RoundedTextField titre = new RoundedTextField(I18n.t("book.placeholder.title"));
    private final RoundedTextField auteur = new RoundedTextField(I18n.t("book.placeholder.author"));
    private final RoundedTextField genre = new RoundedTextField(I18n.t("book.placeholder.genre"));
    private final JTextArea resume = new JTextArea();
    private final TogglePill disponible = new TogglePill(true, null);

    public static void create(Window owner, Runnable onDone) {
        new BookDialog(owner, Mode.CREATE, null, true, onDone).setVisible(true);
    }

    public static void view(Window owner, Livre livre, boolean isAdmin, Runnable onDone) {
        new BookDialog(owner, Mode.VIEW, livre, isAdmin, onDone).setVisible(true);
    }

    public static void edit(Window owner, Livre livre, Runnable onDone) {
        new BookDialog(owner, Mode.EDIT, livre, true, onDone).setVisible(true);
    }

    private BookDialog(Window owner, Mode mode, Livre livre, boolean isAdmin, Runnable onDone) {
        super(owner, "", ModalityType.APPLICATION_MODAL);
        this.mode = mode;
        this.livre = livre;
        this.isAdmin = isAdmin;
        this.onDone = onDone;

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        Card card = new Card(Theme.RADIUS_XL);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(
                Card.shadowInset() + 28, Card.shadowInset() + 30,
                Card.shadowInset() + 24, Card.shadowInset() + 30));

        card.add(buildHeader(), BorderLayout.NORTH);
        card.add(mode == Mode.VIEW ? buildViewBody() : buildForm(), BorderLayout.CENTER);
        card.add(buildButtons(), BorderLayout.SOUTH);

        setContentPane(card);
        pack();
        setSize(mode == Mode.VIEW ? 700 : 620, mode == Mode.VIEW ? 520 : 690);
        setLocationRelativeTo(owner);

        getRootPane().registerKeyboardAction(e -> dispose(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        if (livre != null) populate();
    }

    private JComponent buildHeader() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(0, 0, 20, 0));

        String titleKey = switch (mode) {
            case CREATE -> "book.add.title";
            case EDIT -> "book.edit.title";
            default -> "book.view.title";
        };
        String subKey = switch (mode) {
            case CREATE -> "book.add.sub";
            case EDIT -> "book.edit.sub";
            default -> null;
        };

        JLabel title = new JLabel(I18n.t(titleKey));
        title.setFont(Theme.H1);
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);

        if (subKey != null) {
            JLabel sub = new JLabel(I18n.t(subKey));
            sub.setFont(Theme.BODY);
            sub.setForeground(Theme.MUTED);
            sub.setAlignmentX(LEFT_ALIGNMENT);
            p.add(Box.createVerticalStrut(6));
            p.add(sub);
        }
        return p;
    }

    private JComponent buildViewBody() {
        JPanel body = new JPanel(new BorderLayout(26, 0));
        body.setOpaque(false);

        JPanel cover = new JPanel() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.aa(g);
                int w = 186;
                int h = BookCover.heightFor(w);
                BookCover.paint(g, 8, (getHeight() - h) / 2, w, h, livre, 0.5f);
                g.dispose();
            }
        };
        cover.setOpaque(false);
        cover.setPreferredSize(new Dimension(210, 0));
        body.add(cover, BorderLayout.WEST);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        JLabel bookTitle = new JLabel("<html><body style='width:340px'>"
                + escape(livre.getTitre()) + "</body></html>");
        bookTitle.setFont(Theme.H2);
        bookTitle.setForeground(Theme.TEXT);
        bookTitle.setAlignmentX(LEFT_ALIGNMENT);
        details.add(bookTitle);
        details.add(Box.createVerticalStrut(8));

        JLabel bookAuthor = new JLabel(livre.getAuteur());
        bookAuthor.setFont(Theme.BODY);
        bookAuthor.setForeground(Theme.MUTED);
        bookAuthor.setAlignmentX(LEFT_ALIGNMENT);
        details.add(bookAuthor);
        details.add(Box.createVerticalStrut(16));

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badges.setOpaque(false);
        badges.setAlignmentX(LEFT_ALIGNMENT);
        badges.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        badges.add(livre.estDisponible()
                ? Chip.success(I18n.t("book.available"))
                : Chip.warning(I18n.t("book.borrowed")));
        if (livre.getGenre() != null && !livre.getGenre().isBlank()) {
            badges.add(Chip.neutral(livre.getGenre()));
        }
        details.add(badges);
        details.add(Box.createVerticalStrut(18));

        String summary = livre.getResume();
        if (summary != null && !summary.isBlank()) {
            JLabel label = new JLabel(I18n.t("book.summary"));
            label.setFont(Theme.SMALL_BOLD);
            label.setForeground(Theme.TEXT_SOFT);
            label.setAlignmentX(LEFT_ALIGNMENT);
            details.add(label);
            details.add(Box.createVerticalStrut(6));

            JLabel text = new JLabel("<html><body style='width:350px'>"
                    + escape(summary) + "</body></html>");
            text.setFont(Theme.BODY);
            text.setForeground(Theme.MUTED);
            text.setAlignmentX(LEFT_ALIGNMENT);
            details.add(text);
        }
        details.add(Box.createVerticalGlue());

        body.add(details, BorderLayout.CENTER);
        return body;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(field("book.title", titre));
        form.add(Box.createVerticalStrut(14));
        form.add(field("book.author", auteur));
        form.add(Box.createVerticalStrut(14));
        form.add(field("book.genre", genre));
        form.add(Box.createVerticalStrut(14));

        JLabel summaryLabel = new JLabel(I18n.t("book.summary"));
        summaryLabel.setFont(Theme.SMALL_BOLD);
        summaryLabel.setForeground(Theme.TEXT_SOFT);
        summaryLabel.setAlignmentX(LEFT_ALIGNMENT);
        form.add(summaryLabel);
        form.add(Box.createVerticalStrut(6));

        resume.setFont(Theme.BODY);
        resume.setForeground(Theme.TEXT);
        resume.setLineWrap(true);
        resume.setWrapStyleWord(true);
        resume.setOpaque(false);
        resume.setBorder(new EmptyBorder(12, 14, 12, 14));
        resume.setCaretColor(Theme.PRIMARY);

        JScrollPane scroll = new JScrollPane(resume) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.aa(g);
                Theme.fillRound(g, 0, 0, getWidth(), getHeight(), Theme.RADIUS_SM, Theme.FIELD);
                g.setColor(Theme.BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        Theme.RADIUS_SM * 2, Theme.RADIUS_SM * 2);
                g.dispose();
            }
        };
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        Theme.styleScroll(scroll);
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        scroll.setPreferredSize(new Dimension(0, 130));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        form.add(scroll);
        form.add(Box.createVerticalStrut(18));

        JPanel availabilityRow = new JPanel(new BorderLayout());
        availabilityRow.setOpaque(false);
        availabilityRow.setAlignmentX(LEFT_ALIGNMENT);
        availabilityRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JLabel availabilityLabel = new JLabel(I18n.t("book.available"));
        availabilityLabel.setFont(Theme.BODY_MEDIUM);
        availabilityLabel.setForeground(Theme.TEXT_SOFT);
        availabilityRow.add(availabilityLabel, BorderLayout.WEST);

        JPanel toggleWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        toggleWrap.setOpaque(false);
        toggleWrap.add(disponible);
        availabilityRow.add(toggleWrap, BorderLayout.EAST);
        form.add(availabilityRow);

        form.add(Box.createVerticalGlue());
        return form;
    }

    private JComponent field(String labelKey, JComponent input) {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setAlignmentX(LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));

        JLabel label = new JLabel(I18n.t(labelKey));
        label.setFont(Theme.SMALL_BOLD);
        label.setForeground(Theme.TEXT_SOFT);
        label.setAlignmentX(LEFT_ALIGNMENT);

        input.setAlignmentX(LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        wrap.add(label);
        wrap.add(Box.createVerticalStrut(6));
        wrap.add(input);
        return wrap;
    }

    private JComponent buildButtons() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(22, 0, 0, 0));

        if (mode == Mode.VIEW) {
            if (isAdmin) {
                RoundedButton delete = new RoundedButton(I18n.t("action.delete"),
                        RoundedButton.Style.SECONDARY);
                delete.withIcon(Icons.Kind.TRASH);
                delete.setPreferredSize(new Dimension(140, 46));
                delete.addActionListener(e -> deleteBook());
                row.add(delete);

                RoundedButton edit = new RoundedButton(I18n.t("action.edit"),
                        RoundedButton.Style.PRIMARY);
                edit.withIcon(Icons.Kind.EDIT);
                edit.setPreferredSize(new Dimension(140, 46));
                edit.addActionListener(e -> {
                    dispose();
                    edit(getOwner(), livre, onDone);
                });
                row.add(edit);
            } else {
                RoundedButton close = new RoundedButton(I18n.t("action.close"),
                        RoundedButton.Style.PRIMARY);
                close.setPreferredSize(new Dimension(140, 46));
                close.addActionListener(e -> dispose());
                row.add(close);
            }
        } else {
            RoundedButton cancel = new RoundedButton(I18n.t("action.cancel"),
                    RoundedButton.Style.SECONDARY);
            cancel.setPreferredSize(new Dimension(130, 46));
            cancel.addActionListener(e -> dispose());
            row.add(cancel);

            RoundedButton save = new RoundedButton(I18n.t("action.save"),
                    RoundedButton.Style.PRIMARY);
            save.setPreferredSize(new Dimension(150, 46));
            save.addActionListener(e -> save());
            row.add(save);
            getRootPane().setDefaultButton(save);
        }
        return row;
    }

    private void populate() {
        titre.setText(livre.getTitre());
        auteur.setText(livre.getAuteur());
        genre.setText(livre.getGenre());
        resume.setText(livre.getResume());
        resume.setCaretPosition(0);
        disponible.setSelected(livre.estDisponible());
    }

    private void save() {
        try {
            Livre draft = new Livre(
                    livre == null ? 0 : livre.getId(),
                    titre.getText(), auteur.getText(), genre.getText(),
                    resume.getText(), disponible.isSelected());

            if (mode == Mode.CREATE) {
                if (DatabaseConnection.insertLivre(draft) < 0) {
                    Toast.error(getOwner(), I18n.t("toast.error"));
                    return;
                }
                Toast.success(getOwner(), I18n.t("toast.book.added"));
            } else {
                if (!DatabaseConnection.updateLivre(draft)) {
                    Toast.error(getOwner(), I18n.t("toast.error"));
                    return;
                }
                Toast.success(getOwner(), I18n.t("toast.book.updated"));
            }
            dispose();
            if (onDone != null) onDone.run();
        } catch (Validate.Invalid e) {
            String key = e.key();
            if (key.startsWith("error.title")) titre.markError();
            else if (key.startsWith("error.author")) auteur.markError();
            else if (key.startsWith("error.genre")) genre.markError();
            Toast.error(this, e.getLocalizedMessage());
        }
    }

    private void deleteBook() {
        boolean ok = Dialogs.confirm(this,
                I18n.t("confirm.delete.book.title"),
                I18n.t("confirm.delete.book.body", livre.getTitre()),
                I18n.t("action.delete"));
        if (!ok) return;

        if (DatabaseConnection.deleteLivre(livre.getId())) {
            Toast.success(getOwner(), I18n.t("toast.book.deleted"));
            dispose();
            if (onDone != null) onDone.run();
        } else {
            Toast.error(this, I18n.t("toast.error"));
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
