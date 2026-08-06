import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/**
 * Fetches and caches book cover artwork.
 *
 * <p>Covers are hotlinked from Open Library / Google Books — the catalogue stores a URL
 * per book, filled in by the web project's {@code npm run db:covers}. Downloading 160
 * images on the Swing event thread would freeze the window, so every fetch happens on a
 * small background pool and the caller is handed a repaint callback instead of a result.
 *
 * <p>Two cache layers sit in front of the network:
 * <ul>
 *   <li><b>Memory</b> — decoded images, so scrolling the catalogue never re-decodes.</li>
 *   <li><b>Disk</b> — {@code %LOCALAPPDATA%\BiblioTech\covers}, so a restart does not
 *       re-download the whole shelf. Entries never expire; a cover is immutable for a
 *       given URL, and the folder can simply be deleted to force a refresh.</li>
 * </ul>
 *
 * <p>Failures are remembered too. A URL that 404s or times out is never retried for the
 * lifetime of the process, so a dead link costs one request rather than one per repaint.
 * The caller then draws the generated gradient cover, exactly as before covers existed.
 */
public final class CoverCache {

    private CoverCache() {}

    /** Decoded covers, keyed by URL. */
    private static final Map<String, BufferedImage> MEMORY = new ConcurrentHashMap<>();

    /** URLs currently being fetched, so a repaint storm cannot queue duplicates. */
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    /** URLs known to be unusable — missing, malformed, or unreachable. */
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    /**
     * Four threads keeps a screenful of covers arriving quickly without opening a
     * connection per book. Daemon threads so a pending fetch never holds the JVM open.
     */
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "cover-loader");
        thread.setDaemon(true);
        return thread;
    });

    private static final int CONNECT_TIMEOUT_MS = 6000;
    private static final int READ_TIMEOUT_MS = 10000;

    /** Refuses absurdly large downloads; a cover is tens of kilobytes. */
    private static final int MAX_BYTES = 4 * 1024 * 1024;

    /**
     * Returns the cover for {@code url}, or {@code null} if it is not ready yet.
     *
     * <p>When the image is not cached, a background fetch starts and {@code onLoaded}
     * runs on the event thread once it finishes. Callers should paint their fallback and
     * let the callback trigger a repaint.
     */
    public static BufferedImage get(String url, Runnable onLoaded) {
        if (url == null || url.isBlank()) return null;

        BufferedImage cached = MEMORY.get(url);
        if (cached != null) return cached;
        if (FAILED.contains(url)) return null;
        if (!IN_FLIGHT.add(url)) return null; // already being fetched

        POOL.submit(() -> {
            BufferedImage image = null;
            try {
                image = load(url);
            } catch (Exception e) {
                // Any failure - offline, DNS, TLS interception, malformed image - is
                // treated the same: give up on this URL and keep the generated cover.
                image = null;
            }

            if (image == null) {
                FAILED.add(url);
            } else {
                MEMORY.put(url, image);
            }
            IN_FLIGHT.remove(url);

            if (image != null && onLoaded != null) {
                SwingUtilities.invokeLater(onLoaded);
            }
        });
        return null;
    }

    /** Disk cache first, then the network. */
    private static BufferedImage load(String url) throws IOException {
        Path file = cacheFile(url);

        if (file != null && Files.isRegularFile(file)) {
            try {
                BufferedImage fromDisk = ImageIO.read(file.toFile());
                if (fromDisk != null) return fromDisk;
            } catch (IOException ignored) {
                // Truncated or corrupt cache entry - fall through and re-download.
            }
        }

        byte[] bytes = download(url);
        if (bytes == null) return null;

        BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        if (image == null) return null;

        if (file != null) {
            try {
                Files.createDirectories(file.getParent());
                // Write beside the target then move, so an interrupted run cannot
                // leave a half-written file that later reads as corrupt.
                Path temp = Files.createTempFile(file.getParent(), "cover", ".part");
                Files.write(temp, bytes);
                Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // A read-only or full disk is not a reason to lose the in-memory copy.
            }
        }
        return image;
    }

    private static byte[] download(String url) throws IOException {
        URL target = new URL(url);
        if (!"https".equalsIgnoreCase(target.getProtocol())
                && !"http".equalsIgnoreCase(target.getProtocol())) {
            return null;
        }

        HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "BiblioTech/1.0 (FST Settat)");
        connection.setRequestProperty("Accept", "image/*");

        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) return null;

            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    if (out.size() > MAX_BYTES) return null;
                }
                // Open Library answers a missing cover with a 1-pixel placeholder
                // rather than a 404; anything this small is not real artwork.
                return out.size() < 512 ? null : out.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    /** {@code %LOCALAPPDATA%\BiblioTech\covers\<sha1 of url>}, or null if unavailable. */
    private static Path cacheFile(String url) {
        try {
            String localAppData = System.getenv("LOCALAPPDATA");
            Path base = localAppData != null && !localAppData.isBlank()
                    ? Paths.get(localAppData)
                    : Paths.get(System.getProperty("user.home"), ".cache");

            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(url.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder name = new StringBuilder(hash.length * 2);
            for (byte b : hash) name.append(String.format("%02x", b));

            return base.resolve("BiblioTech").resolve("covers").resolve(name + ".img");
        } catch (Exception e) {
            return null;
        }
    }

    /** Deletes the on-disk cache. Exposed for a future "refresh artwork" action. */
    public static void clearDiskCache() {
        Path file = cacheFile("probe");
        if (file == null) return;
        File folder = file.getParent().toFile();
        File[] entries = folder.listFiles();
        if (entries == null) return;
        for (File entry : entries) entry.delete();
        MEMORY.clear();
        FAILED.clear();
    }
}
