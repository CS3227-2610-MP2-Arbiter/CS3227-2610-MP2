package arbiter.ui.shared;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * The only code that logs, so diagnostic detail has one shape and one destination.
 *
 * <p>A record holds a fixed action name written by a developer and the failure itself: its type,
 * message, causes and stack. It never takes field values, so a credential or source text reaches the
 * log only if code puts it in an exception message, which the error convention forbids.
 *
 * <p>Records always go to the console. While a workspace is open they also go to its log folder,
 * where the current file is {@code arbiter.0.log}; it rotates at {@link #FILE_LIMIT_BYTES} across
 * {@link #FILE_COUNT} files.
 */
public final class DiagnosticLog {
    /** Size at which the workspace log rotates. */
    public static final int FILE_LIMIT_BYTES = 1024 * 1024;

    /** How many rotated workspace log files are kept. */
    public static final int FILE_COUNT = 3;

    /** File name pattern in the log folder, where {@code %g} is 0 for the current file. */
    public static final String FILE_PATTERN = "arbiter.%g.log";

    private static final Logger LOGGER = Logger.getLogger("arbiter");
    private static FileHandler workspaceFile;

    private DiagnosticLog() {
    }

    /**
     * Starts writing records to a workspace's log folder, replacing any earlier workspace's file.
     * Call it only while holding that workspace's lock, so one instance writes the file.
     *
     * @throws IOException if the log file cannot be opened
     */
    public static synchronized void attach(Path logsDirectory) throws IOException {
        Objects.requireNonNull(logsDirectory, "logsDirectory");
        detach();
        // FileHandler patterns treat % as a placeholder, so a literal one in the folder name is doubled,
        // and / as the platform's separator.
        String pattern = logsDirectory.toAbsolutePath().toString().replace("%", "%%") + "/" + FILE_PATTERN;
        FileHandler handler = new FileHandler(pattern, FILE_LIMIT_BYTES, FILE_COUNT, true);
        handler.setFormatter(new SimpleFormatter());
        LOGGER.addHandler(handler);
        workspaceFile = handler;
    }

    /** Stops writing to the workspace log file, if one is attached, and releases it. */
    public static synchronized void detach() {
        if (workspaceFile != null) {
            LOGGER.removeHandler(workspaceFile);
            workspaceFile.close();
            workspaceFile = null;
        }
    }

    /**
     * Records a failure: a warning if its message was written for the user, otherwise an error.
     *
     * @param action what was being done, in words fixed in the code, such as "Open a workspace"
     */
    public static void record(String action, Throwable error) {
        Level level = ErrorMessages.isUserFacing(error) ? Level.WARNING : Level.SEVERE;
        LOGGER.log(level, "Failed: " + action, error);
    }
}
