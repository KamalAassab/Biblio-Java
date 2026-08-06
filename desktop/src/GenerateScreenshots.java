import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.lang.reflect.Method;

public class GenerateScreenshots {
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("java.awt.headless", "false");
        try {
            System.out.println("Initializing Database schema & seeds...");
            try {
                DatabaseConnection.ensureSchema();
                DatabaseConnection.seedIfEmpty();
            } catch (Exception e) {
                System.out.println("Database init exception: " + e.getMessage());
            }

            // Written into the shared docs folder that the README links to.
            File dir = Resources.findDirectory("docs/screenshots");
            if (dir == null) {
                dir = new File("docs/screenshots");
                dir.mkdirs();
            }
            System.out.println("Writing to " + dir.getAbsolutePath());

            // 1. Capture Login Screen
            System.out.println("Capturing 01-login.png...");
            LoginScreen login = new LoginScreen();
            login.pack();
            login.setSize(1020, 660);
            login.setLocationRelativeTo(null);
            login.setVisible(true);
            Thread.sleep(400);

            captureWindow(login, new File(dir, "01-login.png").getPath(), 1020, 660);
            login.setVisible(false);
            login.dispose();

            // 2. Capture Admin Main GUI pages
            Admin admin = new Admin(1, "Kamal Aassab", "", 612345678, "kamal@fsts.ac.ma", null, null);
            BiblioGUI gui = new BiblioGUI(admin);
            gui.pack();
            gui.setSize(1380, 880);
            gui.setLocationRelativeTo(null);
            gui.setVisible(true);
            Thread.sleep(400);

            Method showPage = BiblioGUI.class.getDeclaredMethod("showPage", String.class);
            showPage.setAccessible(true);

            String[] pages = {"dashboard", "catalogue", "emprunts", "reservations", "utilisateurs", "profile"};
            String[] files = {"02-dashboard.png", "03-catalogue.png", "04-emprunts.png",
                              "05-reservations.png", "06-utilisateurs.png", "07-profile.png"};

            for (int i = 0; i < pages.length; i++) {
                System.out.println("Capturing " + files[i] + "...");
                showPage.invoke(gui, pages[i]);
                gui.revalidate();
                gui.repaint();
                Thread.sleep(600);
                captureWindow(gui, new File(dir, files[i]).getPath(), 1380, 880);
            }

            gui.setVisible(false);
            gui.dispose();
            System.out.println("All screenshots successfully generated!");
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void captureWindow(JFrame frame, String path, int width, int height) throws Exception {
        Container comp = frame.getContentPane();
        comp.setSize(width, height);
        comp.doLayout();
        comp.validate();
        comp.repaint();
        Thread.sleep(100);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        comp.paint(g2d);
        g2d.dispose();

        ImageIO.write(image, "png", new File(path));
    }
}
