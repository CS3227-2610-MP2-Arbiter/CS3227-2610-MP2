package arbiter.testing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Creates file-system links for tests of sources reached through them.
 *
 * <p>Each method reports whether the link was made rather than throwing, so a test can skip where
 * the platform refuses one.
 */
public final class FileLinks {
    private static final long MKLINK_TIMEOUT_SECONDS = 30;

    private FileLinks() {
    }

    /** Creates a symbolic link, returning false if the platform or the process's privileges refuse it. */
    public static boolean createSymlink(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
            return true;
        } catch (IOException | UnsupportedOperationException e) {
            return false;
        }
    }

    /**
     * Links a directory the way the platform does it: a junction on Windows, a directory symbolic
     * link elsewhere. A junction needs no special privilege, so the Windows job really exercises one.
     */
    public static boolean createDirectoryLink(Path link, Path target) {
        if (File.separatorChar != '\\') {
            return createSymlink(link, target);
        }
        try {
            Process process = new ProcessBuilder("cmd", "/c", "mklink", "/J",
                    link.toString(), target.toString()).redirectErrorStream(true).start();
            try {
                if (!process.waitFor(MKLINK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    return false;
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                return false;
            }
            return process.exitValue() == 0 && Files.exists(link, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
}
