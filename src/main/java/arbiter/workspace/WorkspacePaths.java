package arbiter.workspace;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Resolves every path inside one workspace.
 *
 * @param root the workspace folder, made absolute and normalised
 */
public record WorkspacePaths(Path root) {
    /** Name of the workspace metadata file. */
    public static final String METADATA_FILE = "workspace.json";

    /** Name of the workspace data snapshot. */
    public static final String DATA_FILE = "arbiter.json";

    /** Name of the folder holding imported media. */
    public static final String MEDIA_DIRECTORY = "media";

    /** Name of the folder holding finished exports. */
    public static final String EXPORTS_DIRECTORY = "exports";

    /** Name of the folder holding application logs. */
    public static final String LOGS_DIRECTORY = "logs";

    /** Makes the root absolute and normalised. */
    public WorkspacePaths {
        root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    }

    /** Returns the workspace metadata file. */
    public Path metadataFile() {
        return root.resolve(METADATA_FILE);
    }

    /** Returns the workspace data snapshot. */
    public Path dataFile() {
        return root.resolve(DATA_FILE);
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
}
