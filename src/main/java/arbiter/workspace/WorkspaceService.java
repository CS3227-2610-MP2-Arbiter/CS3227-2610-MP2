package arbiter.workspace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Creates and opens workspaces. */
public class WorkspaceService {
    // Unknown fields are ignored so that a newer workspace reaches the version check below.
    private static final JsonMapper JSON = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

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
            Files.writeString(paths.metadataFile(), JSON.writeValueAsString(WorkspaceMetadata.createNow()),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new WorkspaceException("Could not create the workspace in " + paths.root(), e);
        }
        return paths;
    }

    /**
     * Opens an existing workspace.
     *
     * @param folder the workspace folder
     * @return the paths of the opened workspace
     * @throws WorkspaceException if the folder is not a workspace, its layout is incomplete or it was
     *     written by a newer version
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
        requireFile(paths.databaseFile());
        requireDirectory(paths.mediaDirectory());
        requireDirectory(paths.exportsDirectory());
        requireDirectory(paths.logsDirectory());
        return paths;
    }

    /**
     * Reads a workspace's metadata.
     *
     * @param paths the workspace
     * @return the metadata
     * @throws WorkspaceException if the file cannot be read or is malformed
     */
    public WorkspaceMetadata readMetadata(WorkspacePaths paths) {
        try {
            String text = Files.readString(paths.metadataFile(), StandardCharsets.UTF_8);
            return JSON.readValue(text, WorkspaceMetadata.class);
        } catch (IOException e) {
            throw new WorkspaceException("Could not read " + WorkspacePaths.METADATA_FILE, e);
        } catch (JacksonException e) {
            throw new WorkspaceException(
                    WorkspacePaths.METADATA_FILE + " is malformed: " + e.getOriginalMessage(), e);
        }
    }

    private static void requireFile(Path path) {
        if (!Files.isRegularFile(path)) {
            throw incomplete(path);
        }
    }

    private static void requireDirectory(Path path) {
        if (!Files.isDirectory(path)) {
            throw incomplete(path);
        }
    }

    private static WorkspaceException incomplete(Path path) {
        return new WorkspaceException("The workspace is incomplete: " + path.getFileName() + " is missing.");
    }
}
