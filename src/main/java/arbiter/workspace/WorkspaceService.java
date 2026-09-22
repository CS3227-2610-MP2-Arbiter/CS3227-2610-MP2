package arbiter.workspace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Creates and opens workspaces.
 *
 * <p>A workspace is a folder holding the database, the imported media, the exports and the logs. On
 * first launch there is no workspace and one has to be made; on later launches the remembered one is
 * reopened.
 */
public class WorkspaceService {
    private static final String VERSION_FIELD = "workspaceVersion";
    private static final String SCHEMA_FIELD = "schemaVersion";
    private static final String CREATED_FIELD = "created";

    private final RecentWorkspaces recent;

    /** Creates a service with a recent list to record workspaces in. */
    public WorkspaceService(RecentWorkspaces recent) {
        this.recent = recent;
    }

    /** Creates a service using the default recent list under the user's home folder. */
    public WorkspaceService() {
        this(RecentWorkspaces.atUserHome());
    }

    /**
     * Creates a workspace folder and its layout.
     *
     * @param folder the folder to create the workspace in
     * @return the paths of the new workspace
     * @throws WorkspaceException if the folder cannot be created or already holds a workspace
     */
    public WorkspacePaths create(Path folder) {
        WorkspacePaths paths = new WorkspacePaths(folder);
        try {
            Files.createDirectories(paths.root());
            if (Files.exists(paths.metadataFile())) {
                throw new WorkspaceException(
                        "That folder already holds an Arbiter workspace. Open it instead.");
            }
            Files.createDirectories(paths.mediaDirectory());
            Files.createDirectories(paths.exportsDirectory());
            Files.createDirectories(paths.logsDirectory());
            Files.createFile(paths.databaseFile());
            writeMetadata(paths, WorkspaceMetadata.createNow());
        } catch (IOException e) {
            throw new WorkspaceException("Could not create the workspace in " + paths.root(), e);
        }
        recent.load();
        recent.remember(paths.root());
        return paths;
    }

    /**
     * Opens an existing workspace.
     *
     * @param folder the workspace folder
     * @return the paths of the opened workspace
     * @throws WorkspaceException if the folder is not a workspace or its layout is incomplete
     */
    public WorkspacePaths open(Path folder) {
        WorkspacePaths paths = new WorkspacePaths(folder);
        if (!Files.isDirectory(paths.root())) {
            throw new WorkspaceException("There is no folder at " + paths.root());
        }
        if (!Files.isRegularFile(paths.metadataFile())) {
            throw new WorkspaceException(
                    "That folder is not an Arbiter workspace: " + WorkspacePaths.METADATA_FILE
                            + " is missing.");
        }
        WorkspaceMetadata metadata = readMetadata(paths);
        if (metadata.workspaceVersion() > WorkspaceMetadata.CURRENT_WORKSPACE_VERSION) {
            throw new WorkspaceException(
                    "This workspace was created by a newer version of Arbiter (layout version "
                            + metadata.workspaceVersion() + ", this build understands "
                            + WorkspaceMetadata.CURRENT_WORKSPACE_VERSION
                            + "). Update Arbiter before opening it. The workspace has not been "
                            + "changed.");
        }
        requireDirectory(paths.mediaDirectory());
        requireDirectory(paths.exportsDirectory());
        requireDirectory(paths.logsDirectory());
        recent.load();
        recent.remember(paths.root());
        return paths;
    }

    /**
     * Opens the workspace opened last, if one is remembered and still readable.
     *
     * @return the paths of the remembered workspace, or empty when there is none
     */
    public Optional<WorkspacePaths> openRemembered() {
        recent.load();
        Path last = recent.lastOpened();
        if (last == null || !Files.isRegularFile(new WorkspacePaths(last).metadataFile())) {
            return Optional.empty();
        }
        return Optional.of(open(last));
    }

    /** Returns the recent workspace list. */
    public RecentWorkspaces getRecent() {
        return recent;
    }

    /**
     * Reads a workspace's metadata.
     *
     * @param paths the workspace
     * @return the metadata
     * @throws WorkspaceMetadataException if the file is missing or malformed
     */
    public WorkspaceMetadata readMetadata(WorkspacePaths paths) {
        try {
            Map<String, Object> fields =
                    Json.read(Files.readString(paths.metadataFile(), StandardCharsets.UTF_8));
            return new WorkspaceMetadata(
                    intField(fields, VERSION_FIELD),
                    intField(fields, SCHEMA_FIELD),
                    java.time.Instant.parse(String.valueOf(fields.get(CREATED_FIELD))));
        } catch (IOException e) {
            throw new WorkspaceMetadataException(
                    "Could not read " + WorkspacePaths.METADATA_FILE, e);
        } catch (WorkspaceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new WorkspaceMetadataException(
                    WorkspacePaths.METADATA_FILE + " is malformed: " + e.getMessage(), e);
        }
    }

    private void writeMetadata(WorkspacePaths paths, WorkspaceMetadata metadata) throws IOException {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(VERSION_FIELD, metadata.workspaceVersion());
        fields.put(SCHEMA_FIELD, metadata.schemaVersion());
        fields.put(CREATED_FIELD, metadata.created().toString());
        Files.writeString(paths.metadataFile(), Json.write(fields), StandardCharsets.UTF_8);
    }

    private static int intField(Map<String, Object> fields, String name) {
        Object value = fields.get(name);
        if (!(value instanceof Number)) {
            throw new WorkspaceMetadataException(
                    WorkspacePaths.METADATA_FILE + " is missing the " + name + " field");
        }
        return ((Number) value).intValue();
    }

    private static void requireDirectory(Path path) {
        if (!Files.isDirectory(path)) {
            throw new WorkspaceException(
                    "The workspace is incomplete: " + path.getFileName() + " is missing.");
        }
    }
}
