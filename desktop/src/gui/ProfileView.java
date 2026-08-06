import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Graphics2D;
import java.util.function.Consumer;

/**
 * The signed-in user's own account: identity summary, editable details, a password
 * change form, and their personal activity counts.
 */
public class ProfileView extends JPanel {

    private Utilisateur user;
    private final Consumer<Utilisateur> onUpdated;

    private final PageHeader header = new PageHeader();
    private final IdentityCard identity = new IdentityCard();

    private final RoundedTextField nameField = new RoundedTextField("");
    private final RoundedTextField emailField = new RoundedTextField("").withIcon(Icons.Kind.MAIL);
    private final RoundedTextField phoneField = new RoundedTextField("").withIcon(Icons.Kind.PHONE);

    private final RoundedPasswordField currentPassword = new RoundedPasswordField("");
    private final RoundedPasswordField newPassword = new RoundedPasswordField("");
    private final RoundedPasswordField confirmPassword = new RoundedPasswordField("");

    private final JLabel detailsTitle = new JLabel();
    private final JLabel securityTitle = new JLabel();
    private final JLabel passwordHint = new JLabel();
    private final RoundedButton saveDetails =
            new RoundedButton("", RoundedButton.Style.PRIMARY);
    private final RoundedButton savePassword =
            new RoundedButton("", RoundedButton.Style.SECONDARY);

    private final StatCard loansCard =
            new StatCard("profile.loans", Theme.AMBER, Icons.Kind.CLOCK);
    private final StatCard reservationsCard =
            new StatCard("profile.reservations", Theme.ACCENT, Icons.Kind.BOOKMARK);

    public ProfileView(Utilisateur user, Consumer<Utilisateur> onUpdated) {
        this.user = user;
        this.onUpdated = onUpdated;

        setOpaque(false);
        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JComponent buildBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(0, 10, 20, 10));

        identity.setAlignmentX(LEFT_ALIGNMENT);
        identity.setMaximumSize(new Dimension(Integer.MAX_VALUE, 186));
        body.add(identity);
        body.add(Box.createVerticalStrut(6));

        JPanel activity = new JPanel(new GridLayout(1, 2, 4, 0));
        activity.setOpaque(false);
        activity.setAlignmentX(LEFT_ALIGNMENT);
        activity.setMaximumSize(new Dimension(Integer.MAX_VALUE, 156));
        activity.add(loansCard);
        activity.add(reservationsCard);
        body.add(activity);
        body.add(Box.createVerticalStrut(6));

        JPanel forms = new JPanel(new GridLayout(1, 2, 4, 0));
        forms.setOpaque(false);
        forms.setAlignmentX(LEFT_ALIGNMENT);
        forms.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
        forms.add(buildDetailsCard());
        forms.add(buildSecurityCard());
        body.add(forms);

        body.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(body);
        Theme.styleScroll(scroll);
        return scroll;
    }

    private JComponent buildDetailsCard() {
        Card card = new Card(Theme.RADIUS_LG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(
                Card.shadowInset() + 24, Card.shadowInset() + 24,
                Card.shadowInset() + 20, Card.shadowInset() + 24));

        detailsTitle.setFont(Theme.H3);
        detailsTitle.setForeground(Theme.TEXT);
        detailsTitle.setAlignmentX(LEFT_ALIGNMENT);
        card.add(detailsTitle);
        card.add(Box.createVerticalStrut(16));

        card.add(labelled("user.name", nameField));
        card.add(Box.createVerticalStrut(12));
        card.add(labelled("user.email", emailField));
        card.add(Box.createVerticalStrut(12));
        card.add(labelled("user.phone", phoneField));
        card.add(Box.createVerticalStrut(18));

        saveDetails.setAlignmentX(LEFT_ALIGNMENT);
        saveDetails.setPreferredSize(new Dimension(180, 46));
        saveDetails.setMaximumSize(new Dimension(180, 46));
        saveDetails.addActionListener(e -> saveDetails());
        card.add(saveDetails);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JComponent buildSecurityCard() {
        Card card = new Card(Theme.RADIUS_LG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(
                Card.shadowInset() + 24, Card.shadowInset() + 24,
                Card.shadowInset() + 20, Card.shadowInset() + 24));

        securityTitle.setFont(Theme.H3);
        securityTitle.setForeground(Theme.TEXT);
        securityTitle.setAlignmentX(LEFT_ALIGNMENT);
        card.add(securityTitle);
        card.add(Box.createVerticalStrut(16));

        card.add(labelled("profile.password.current", currentPassword));
        card.add(Box.createVerticalStrut(12));
        card.add(labelled("profile.password.new", newPassword));
        card.add(Box.createVerticalStrut(12));
        card.add(labelled("profile.password.confirm", confirmPassword));
        card.add(Box.createVerticalStrut(10));

        passwordHint.setFont(Theme.TINY);
        passwordHint.setForeground(Theme.MUTED);
        passwordHint.setAlignmentX(LEFT_ALIGNMENT);
        card.add(passwordHint);
        card.add(Box.createVerticalStrut(14));

        savePassword.setAlignmentX(LEFT_ALIGNMENT);
        savePassword.setPreferredSize(new Dimension(220, 46));
        savePassword.setMaximumSize(new Dimension(220, 46));
        savePassword.addActionListener(e -> changePassword());
        card.add(savePassword);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JComponent labelled(String labelKey, JComponent field) {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setAlignmentX(LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));

        JLabel label = new JLabel(I18n.t(labelKey));
        label.setFont(Theme.SMALL_BOLD);
        label.setForeground(Theme.TEXT_SOFT);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.putClientProperty("labelKey", labelKey);

        field.setAlignmentX(LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        wrap.add(label);
        wrap.add(Box.createVerticalStrut(6));
        wrap.add(field);
        return wrap;
    }

    public void onShow() {
        header.setTitle(I18n.t("profile.title"), I18n.t("profile.sub"));
        detailsTitle.setText(I18n.t("profile.details"));
        securityTitle.setText(I18n.t("profile.security"));
        passwordHint.setText("<html><body style='width:260px'>"
                + escape(I18n.t("profile.hint.password")) + "</body></html>");
        saveDetails.setText(I18n.t("action.save"));
        savePassword.setText(I18n.t("profile.password.change"));

        nameField.setText(user.getNom());
        emailField.setText(user.getEmail() == null ? "" : user.getEmail());
        phoneField.setText(user.getNumero() > 0 ? String.valueOf(user.getNumero()) : "");
        currentPassword.setText("");
        newPassword.setText("");
        confirmPassword.setText("");

        identity.refresh();
        refreshActivity();
        revalidate();
        repaint();
    }

    private void refreshActivity() {
        int loans = 0;
        for (Emprunt e : DatabaseConnection.getEmprunts()) {
            if (e.getUtilisateur().getId() == user.getId()) loans++;
        }
        int reservations = 0;
        for (Reservation r : DatabaseConnection.getReservations()) {
            if (r.getUtilisateur().getId() == user.getId()) reservations++;
        }
        loansCard.setValue(loans);
        reservationsCard.setValue(reservations);
    }

    private void saveDetails() {
        try {
            String name = Validate.username(nameField.getText());
            String email = Validate.email(emailField.getText());
            int phone = Validate.phone(phoneField.getText());

            if (!DatabaseConnection.updateProfile(user.getId(), name, email, phone)) {
                Toast.show(this, I18n.t("toast.error"));
                return;
            }
            user.setNom(name);
            user.setEmail(email);
            user.setNumero(phone);

            Toast.show(this, I18n.t("profile.updated"));
            identity.refresh();
            if (onUpdated != null) onUpdated.accept(user);
        } catch (Validate.Invalid e) {
            Toast.show(this, e.getLocalizedMessage());
            if (e.key().startsWith("error.email")) emailField.markError();
            else if (e.key().startsWith("error.phone")) phoneField.markError();
            else nameField.markError();
        }
    }

    private void changePassword() {
        String current = new String(currentPassword.getPassword());
        String next = new String(newPassword.getPassword());
        String confirm = new String(confirmPassword.getPassword());

        if (!next.equals(confirm)) {
            confirmPassword.markError();
            Toast.show(this, I18n.t("profile.password.mismatch"));
            return;
        }
        try {
            Validate.password(next);
        } catch (Validate.Invalid e) {
            newPassword.markError();
            Toast.show(this, e.getLocalizedMessage());
            return;
        }

        if (!DatabaseConnection.changePassword(user.getId(), current, next)) {
            currentPassword.markError();
            Toast.show(this, I18n.t("profile.password.wrong"));
            return;
        }

        currentPassword.setText("");
        newPassword.setText("");
        confirmPassword.setText("");
        Toast.show(this, I18n.t("profile.password.changed"));
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** The banner across the top: avatar, name, role badge and contact details. */
    private class IdentityCard extends Card {

        IdentityCard() {
            super(Theme.RADIUS_XL);
            setPreferredSize(new Dimension(0, 186));
        }

        void refresh() {
            repaint();
        }

        @Override
        protected void paintCardContent(Graphics2D g, int x, int y, int w, int h) {
            // A deep blue band with a gold arc, matching the sidebar credit card.
            Theme.gradientRound(g, x, y, w, Math.round(h * 0.62f), Theme.RADIUS_XL,
                    Theme.PRIMARY_DARK, Theme.PRIMARY);
            g.setColor(Theme.alpha(Theme.ACCENT, 52));
            g.fillOval(x + w - 150, y - 78, 190, 190);
            // Square off the band's lower corners so it meets the white body cleanly.
            g.setColor(Theme.PRIMARY);
            g.fillRect(x, y + Math.round(h * 0.40f), w, Math.round(h * 0.22f));

            int av = 88;
            int ax = x + 34;
            int ay = y + Math.round(h * 0.62f) - av / 2 - 8;

            g.setColor(Theme.SURFACE);
            g.fillOval(ax - 5, ay - 5, av + 10, av + 10);

            String initials = Avatar.initialsOf(user.getNom());
            int hash = Math.abs(initials.hashCode());
            float hue = (hash % 360) / 360f;
            g.setPaint(new java.awt.GradientPaint(ax, ay,
                    Color.getHSBColor((hue + 0.05f) % 1f, 0.62f, 0.80f),
                    ax + av, ay + av, Color.getHSBColor(hue, 0.55f, 0.62f)));
            g.fillOval(ax, ay, av, av);

            g.setFont(Theme.H2.deriveFont(30f));
            FontMetrics ifm = g.getFontMetrics();
            g.setColor(Color.WHITE);
            g.drawString(initials, ax + (av - ifm.stringWidth(initials)) / 2,
                    ay + (av - ifm.getHeight()) / 2 + ifm.getAscent());

            int textX = ax + av + 22;

            g.setFont(Theme.H2);
            FontMetrics nfm = g.getFontMetrics();
            g.setColor(Theme.TEXT);
            g.drawString(BookCover.ellipsise(user.getNom(), nfm, w - textX - 40),
                    textX, y + h - 52);

            boolean admin = user instanceof Admin;
            String role = I18n.t(admin ? "user.role.admin" : "user.role.reader");
            g.setFont(Theme.TINY);
            FontMetrics rfm = g.getFontMetrics();
            int rw = rfm.stringWidth(role) + 24;
            Theme.fillRound(g, textX, y + h - 40, rw, 26, Theme.PILL,
                    admin ? Theme.PRIMARY_SOFT : Theme.SURFACE_CHIP);
            g.setColor(admin ? Theme.PRIMARY : Theme.MUTED);
            g.drawString(role, textX + 12, y + h - 40 + (26 - rfm.getHeight()) / 2 + rfm.getAscent());

            String email = user.getEmail();
            if (email != null && !email.isBlank()) {
                g.setFont(Theme.SMALL);
                FontMetrics efm = g.getFontMetrics();
                g.setColor(Theme.MUTED);
                g.drawString(BookCover.ellipsise(email, efm, w - textX - rw - 60),
                        textX + rw + 14, y + h - 40 + (26 - efm.getHeight()) / 2 + efm.getAscent());
            }
        }
    }
}
