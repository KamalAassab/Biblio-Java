import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Locates bundled files (fonts, the crest, the .env configuration) regardless of the
 * directory the application was launched from.
 *
 * <p>The same build gets started in several ways — from the repository root, from
 * {@code desktop/}, from an IDE run configuration, or from an installed copy where
 * everything sits beside the executable. Rather than repeating a list of guesses at
 * each call site, every lookup goes through here.
 */
public final class Resources {

    private Resources() {}

    /**
     * Directories a relative resource path is resolved against, in priority order.
     * The first hit wins.
     */
    private static final String[] ROOTS = {
        ".",          // launched from desktop/, or an installed copy
        "desktop",    // launched from the repository root
        "..",         // launched from desktop/out or a similar build directory
        "../..",
    };

    /** Returns the first existing file matching {@code relative}, or {@code null}. */
    public static File find(String relative) {
        Path base = Paths.get(System.getProperty("user.dir", "."));
        for (String root : ROOTS) {
            File candidate = base.resolve(root).resolve(relative).normalize().toFile();
            if (candidate.isFile()) return candidate;
        }
        return null;
    }

    /** Same as {@link #find}, but for directories. */
    public static File findDirectory(String relative) {
        Path base = Paths.get(System.getProperty("user.dir", "."));
        for (String root : ROOTS) {
            File candidate = base.resolve(root).resolve(relative).normalize().toFile();
            if (candidate.isDirectory()) return candidate;
        }
        return null;
    }

    /** Returns the first existing path matching {@code relative}, or {@code null}. */
    public static Path findPath(String relative) {
        File file = find(relative);
        return file == null ? null : file.toPath();
    }
}
