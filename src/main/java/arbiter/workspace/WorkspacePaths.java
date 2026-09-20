package arbiter.workspace;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Resolves every path inside one workspace.
 *
 * <p>This is the only place the workspace layout is written down. Callers ask for a path rather than
 * joining names themselves, so the layout cannot drift between features.
 */
public final class WorkspacePaths {
    /** Name of the workspace metadata file. */
    public static final String METADATA_FILE = "workspace.json";

    /** Name of the SQLite database file. */
    public static final String DATABASE_FILE = "arbiter.db";

    /** Name of the folder holding imported media. */
    public static final String MEDIA_DIRECTORY = "media";

    /** Name of the folder holding finished exports. */
    public static final String EXPORTS_DIRECTORY = "exports";

    /** Name of the folder holding application logs. */
    public static final String LOGS_DIRECTORY = "logs";

    private final Path root;

    /**
     * Creates paths rooted at a workspace folder.
     *
     * @param root the workspace folder
     */
    public WorkspacePaths(Path root) {
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    }

    /** Returns the workspace folder itself. */
    public Path root() {
        return root;
    }

    /** Returns the workspace metadata file. */
    public Path metadataFile() {
        return root.resolve(METADATA_FILE);
    }

    /** Returns the SQLite database file. */
    public Path databaseFile() {
        return root.resolve(DATABASE_FILE);
    }

    /** Returns the folder holding imported media. */
    public Path mediaDirectory() {
        return root.resolve(MEDIA_DIRECTORY);
    }

    /** Returns the folder holding finished exports. */
    public Path exportsDirectory() {
        return root.resolve(EXPORTS_DIRECTORY);
    }

    /** Returns the folder holding application logs. */
    public Path logsDirectory() {
        return root.resolve(LOGS_DIRECTORY);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WorkspacePaths)) {
            return false;
        }
        return root.equals(((WorkspacePaths) other).root);
    }

    @Override
    public int hashCode() {
        return root.hashCode();
    }

    @Override
    public String toString() {
        return root.toString();
    }
}
