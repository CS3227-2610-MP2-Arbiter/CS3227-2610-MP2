package arbiter.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceServiceTest {
    @TempDir
    Path temporary;

    private final WorkspaceService service = new WorkspaceService();

    @Test
    void create_newFolder_layoutCreated() {
        Path folder = temporary.resolve("workspace");

        WorkspacePaths paths = service.create(folder);

        assertEquals(folder, paths.root());
        assertLayout(folder);
    }

    @Test
    void create_newFolder_currentVersionsRecorded() {
        WorkspacePaths paths = service.create(temporary.resolve("workspace"));

        WorkspaceMetadata metadata = service.readMetadata(paths);

        assertEquals(WorkspaceMetadata.CURRENT_WORKSPACE_VERSION, metadata.workspaceVersion());
        assertEquals(WorkspaceMetadata.CURRENT_SCHEMA_VERSION, metadata.schemaVersion());
    }

    @Test
    void create_folderWithOtherFiles_filesLeftAlone() throws IOException {
        Path folder = Files.createDirectories(temporary.resolve("workspace"));
        Path notes = Files.writeString(folder.resolve("notes.txt"), "notes");
        Path media = Files.createDirectories(folder.resolve("media"));
        Path image = Files.writeString(media.resolve("cat.jpg"), "cat");

        service.create(folder);

        assertLayout(folder);
        assertEquals("notes", Files.readString(notes));
        assertEquals("cat", Files.readString(image));
    }

    @Test
    void create_folderHoldsWorkspace_exceptionThrown() throws IOException {
        WorkspacePaths paths = service.create(temporary.resolve("workspace"));
        String metadata = Files.readString(paths.metadataFile());

        assertThrows(WorkspaceException.class, () -> service.create(paths.root()));
        assertEquals(metadata, Files.readString(paths.metadataFile()));
    }

    @Test
    void create_folderHoldsDatabase_databaseLeftAlone() throws IOException {
        Path folder = Files.createDirectories(temporary.resolve("workspace"));
        Path database = Files.writeString(folder.resolve("arbiter.db"), "existing data");

        assertThrows(WorkspaceException.class, () -> service.create(folder));
        assertEquals("existing data", Files.readString(database));
    }

    @Test
    void create_folderIsFile_exceptionThrown() throws IOException {
        Path file = Files.writeString(temporary.resolve("workspace"), "not a folder");

        assertThrows(WorkspaceException.class, () -> service.create(file));
    }

    @Test
    void open_olderVersion_workspaceOpened() throws IOException {
        WorkspacePaths paths = service.create(temporary.resolve("workspace"));
        Files.writeString(paths.metadataFile(), metadataJson(WorkspaceMetadata.CURRENT_WORKSPACE_VERSION - 1, ""));

        assertEquals(paths.root(), service.open(paths.root()).root());
    }

    @Test
    void open_currentVersion_workspaceOpened() throws IOException {
        Path folder = temporary.resolve("workspace");
        WorkspacePaths created = service.create(folder);
        String metadata = Files.readString(created.metadataFile());

        WorkspacePaths opened = service.open(folder);

        assertEquals(folder, opened.root());
        assertEquals(metadata, Files.readString(opened.metadataFile()));
    }

    @Test
    void open_newerVersion_exceptionThrown() throws IOException {
        WorkspacePaths paths = service.create(temporary.resolve("workspace"));
        // A newer version may also add fields, which must not hide the version check.
        String newer = metadataJson(WorkspaceMetadata.CURRENT_WORKSPACE_VERSION + 1, ", \"addedLater\": true");
        Files.writeString(paths.metadataFile(), newer);

        WorkspaceException failure = assertThrows(WorkspaceException.class, () -> service.open(paths.root()));

        assertTrue(failure.getMessage().contains("newer"), failure.getMessage());
        assertEquals(newer, Files.readString(paths.metadataFile()));
    }

    @Test
    void open_severalWorkspaces_eachOpened() {
        Path first = temporary.resolve("first");
        Path second = temporary.resolve("second");
        service.create(first);
        service.create(second);

        assertEquals(first, service.open(first).root());
        assertEquals(second, service.open(second).root());
    }

    @Test
    void open_movedWorkspace_mediaResolvedUnderNewRoot() throws IOException {
        Path original = temporary.resolve("original");
        service.create(original);
        Path moved = Files.move(original, temporary.resolve("moved"));

        assertEquals(moved.resolve("media"), service.open(moved).mediaDirectory());
    }

    @Test
    void open_missingFolder_exceptionThrown() {
        assertThrows(WorkspaceException.class, () -> service.open(temporary.resolve("missing")));
    }

    @Test
    void open_folderWithoutMetadata_exceptionThrown() throws IOException {
        Path folder = Files.createDirectories(temporary.resolve("plain"));

        assertThrows(WorkspaceException.class, () -> service.open(folder));
    }

    @Test
    void open_layoutFolderMissing_exceptionThrown() throws IOException {
        for (String missing : List.of("media", "exports", "logs")) {
            Path folder = service.create(temporary.resolve("without-" + missing)).root();
            Files.delete(folder.resolve(missing));

            assertThrows(WorkspaceException.class, () -> service.open(folder), missing);
        }
    }

    @Test
    void open_databaseMissing_exceptionThrown() throws IOException {
        WorkspacePaths paths = service.create(temporary.resolve("workspace"));
        Files.delete(paths.databaseFile());

        assertThrows(WorkspaceException.class, () -> service.open(paths.root()));
    }

    @Test
    void readMetadata_validFile_metadataReturned() throws IOException {
        WorkspacePaths paths = new WorkspacePaths(temporary);
        Files.writeString(paths.metadataFile(),
                "{\"workspaceVersion\": 3, \"schemaVersion\": 7, \"created\": \"2026-09-20T08:30:00Z\"}");

        WorkspaceMetadata expected = new WorkspaceMetadata(3, 7, Instant.parse("2026-09-20T08:30:00Z"));
        assertEquals(expected, service.readMetadata(paths));
    }

    @Test
    void readMetadata_fileMissing_exceptionThrown() {
        WorkspacePaths paths = new WorkspacePaths(temporary);

        assertThrows(WorkspaceException.class, () -> service.readMetadata(paths));
    }

    @Test
    void readMetadata_notJson_exceptionThrown() throws IOException {
        WorkspacePaths paths = new WorkspacePaths(temporary);
        Files.writeString(paths.metadataFile(), "{ this is not json");

        assertThrows(WorkspaceException.class, () -> service.readMetadata(paths));
    }

    @Test
    void readMetadata_fieldMissing_exceptionThrown() throws IOException {
        WorkspacePaths paths = new WorkspacePaths(temporary);
        Files.writeString(paths.metadataFile(), "{\"schemaVersion\": 1, \"created\": \"2026-09-20T08:30:00Z\"}");

        assertThrows(WorkspaceException.class, () -> service.readMetadata(paths));
    }

    private static String metadataJson(int workspaceVersion, String extraFields) {
        return "{\"workspaceVersion\": " + workspaceVersion + ", \"schemaVersion\": 1, "
                + "\"created\": \"2026-09-20T08:30:00Z\"" + extraFields + "}";
    }

    private static void assertLayout(Path folder) {
        assertTrue(Files.isRegularFile(folder.resolve("workspace.json")), "workspace.json");
        assertTrue(Files.isRegularFile(folder.resolve("arbiter.db")), "arbiter.db");
        assertTrue(Files.isDirectory(folder.resolve("media")), "media");
        assertTrue(Files.isDirectory(folder.resolve("exports")), "exports");
        assertTrue(Files.isDirectory(folder.resolve("logs")), "logs");
    }
}
