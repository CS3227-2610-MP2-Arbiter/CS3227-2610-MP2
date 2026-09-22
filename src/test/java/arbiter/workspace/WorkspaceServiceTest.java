package arbiter.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests workspace creation, opening, metadata and the version guard. */
class WorkspaceServiceTest {
    @TempDir
    Path temporary;

    private final WorkspaceService service = new WorkspaceService();

    private Path workspaceFolder(String name) {
        return temporary.resolve(name);
    }

    @Test
    void createMakesTheWholeLayout() {
        WorkspacePaths paths = service.create(workspaceFolder("ws"));

        assertTrue(Files.isDirectory(paths.root()), "workspace folder");
        assertTrue(Files.isDirectory(paths.mediaDirectory()), "media folder");
        assertTrue(Files.isDirectory(paths.exportsDirectory()), "exports folder");
        assertTrue(Files.isDirectory(paths.logsDirectory()), "logs folder");
        assertTrue(Files.isRegularFile(paths.metadataFile()), "workspace.json");
        assertTrue(Files.isRegularFile(paths.databaseFile()), "arbiter.db");
    }

    @Test
    void metadataRecordsBothVersions() {
        WorkspacePaths paths = service.create(workspaceFolder("ws"));
        WorkspaceMetadata metadata = service.readMetadata(paths);

        assertEquals(WorkspaceMetadata.CURRENT_WORKSPACE_VERSION, metadata.workspaceVersion());
        assertEquals(WorkspaceMetadata.CURRENT_SCHEMA_VERSION, metadata.schemaVersion());
    }

    @Test
    void secondStartReopensWithoutRecreating() throws IOException {
        Path folder = workspaceFolder("ws");
        service.create(folder);
        Path marker = Files.writeString(folder.resolve("marker.txt"), "untouched");

        WorkspacePaths reopened = service.open(folder);

        assertEquals(folder.toAbsolutePath().normalize(), reopened.root());
        assertEquals("untouched", Files.readString(marker), "open must not rewrite the workspace");
    }

    @Test
    void multipleWorkspacesCanBeCreatedAndOpened() {
        Path first = workspaceFolder("first");
        Path second = workspaceFolder("second");
        service.create(first);
        service.create(second);

        assertEquals(first.toAbsolutePath().normalize(), service.open(first).root());
        assertEquals(second.toAbsolutePath().normalize(), service.open(second).root());
    }

    @Test
    void aNewerWorkspaceFailsClearlyAndIsLeftUntouched() throws IOException {
        Path folder = workspaceFolder("future");
        WorkspacePaths paths = service.create(folder);

        // Stamp the workspace as written by a later version, which may also add fields.
        int bumped = WorkspaceMetadata.CURRENT_WORKSPACE_VERSION + 3;
        String newer = "{\"workspaceVersion\": " + bumped + ", \"schemaVersion\": 1, "
                + "\"created\": \"2026-09-20T00:00:00Z\", \"addedLater\": true}";
        Files.writeString(paths.metadataFile(), newer, StandardCharsets.UTF_8);

        WorkspaceException failure = assertThrows(WorkspaceException.class, () ->
                service.open(folder));

        assertTrue(failure.getMessage().contains(String.valueOf(bumped)),
                "the message should name the newer version: " + failure.getMessage());
        assertTrue(failure.getMessage().toLowerCase().contains("newer"),
                "the message should say the workspace is newer: " + failure.getMessage());
        assertEquals(newer, Files.readString(paths.metadataFile(), StandardCharsets.UTF_8),
                "the workspace must be left exactly as it was");
    }

    @Test
    void malformedMetadataIsRefused() throws IOException {
        WorkspacePaths paths = service.create(workspaceFolder("ws"));

        Files.writeString(paths.metadataFile(), "{ this is not json", StandardCharsets.UTF_8);
        WorkspaceException notJson = assertThrows(WorkspaceException.class, () ->
                service.open(paths.root()));

        Files.writeString(paths.metadataFile(), "{\"workspaceVersion\": 1, \"created\": "
                + "\"2026-09-20T00:00:00Z\"}", StandardCharsets.UTF_8);
        WorkspaceException missingField = assertThrows(WorkspaceException.class, () ->
                service.open(paths.root()));

        assertTrue(notJson.getMessage().contains(WorkspacePaths.METADATA_FILE),
                "the message should name the file: " + notJson.getMessage());
        assertTrue(missingField.getMessage().contains("schemaVersion"),
                "the message should name the missing field: " + missingField.getMessage());
    }

    @Test
    void creatingOverAnExistingWorkspaceIsRefused() {
        Path folder = workspaceFolder("ws");
        service.create(folder);

        WorkspaceException failure = assertThrows(WorkspaceException.class, () ->
                service.create(folder));

        assertTrue(failure.getMessage().contains("already holds"),
                "the message should explain why: " + failure.getMessage());
    }

    @Test
    void openingAFolderThatIsNotAWorkspaceIsRefused() throws IOException {
        Path folder = Files.createDirectories(workspaceFolder("plain"));

        WorkspaceException failure = assertThrows(WorkspaceException.class, () ->
                service.open(folder));

        assertTrue(failure.getMessage().contains(WorkspacePaths.METADATA_FILE),
                "the message should name the missing file: " + failure.getMessage());
    }

    @Test
    void anIncompleteWorkspaceIsRefused() throws IOException {
        Path folder = workspaceFolder("broken");
        WorkspacePaths paths = service.create(folder);
        Files.delete(paths.mediaDirectory());

        WorkspaceException failure = assertThrows(WorkspaceException.class, () ->
                service.open(folder));

        assertTrue(failure.getMessage().contains(WorkspacePaths.MEDIA_DIRECTORY),
                "the message should name the missing folder: " + failure.getMessage());
    }

    @Test
    void everyPathComesFromTheOneObject() {
        Path folder = workspaceFolder("ws");
        WorkspacePaths paths = new WorkspacePaths(folder);
        Path root = folder.toAbsolutePath().normalize();

        assertEquals(root.resolve(WorkspacePaths.METADATA_FILE), paths.metadataFile());
        assertEquals(root.resolve(WorkspacePaths.DATABASE_FILE), paths.databaseFile());
        assertEquals(root.resolve(WorkspacePaths.MEDIA_DIRECTORY), paths.mediaDirectory());
        assertEquals(root.resolve(WorkspacePaths.EXPORTS_DIRECTORY), paths.exportsDirectory());
        assertEquals(root.resolve(WorkspacePaths.LOGS_DIRECTORY), paths.logsDirectory());
    }

    @Test
    void aRelativeRootIsResolvedOnce() {
        WorkspacePaths paths = new WorkspacePaths(Path.of("relative-folder"));

        assertTrue(paths.root().isAbsolute(), "the root should be absolute");
        assertEquals(paths.root(), paths.root(), "resolution should be stable");
    }
}
