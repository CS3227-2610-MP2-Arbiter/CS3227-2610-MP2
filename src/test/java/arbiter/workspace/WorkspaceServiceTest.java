package arbiter.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests workspace creation, reopening, metadata and the version guard. */
class WorkspaceServiceTest {
    @TempDir
    Path temporary;

    private Path userHome;
    private WorkspaceService service;

    @BeforeEach
    void setUp() throws IOException {
        // A fake user home, so the tests never touch the real recent-workspaces file.
        userHome = Files.createDirectories(temporary.resolve("home"));
        service = new WorkspaceService(
                new RecentWorkspaces(userHome.resolve(RecentWorkspaces.FILE_NAME)));
    }

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

        assertEquals(WorkspaceMetadata.CURRENT_WORKSPACE_VERSION, metadata.getWorkspaceVersion());
        assertEquals(WorkspaceMetadata.CURRENT_SCHEMA_VERSION, metadata.getSchemaVersion());
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
    void rememberedWorkspaceIsReopenedOnTheNextStart() {
        Path first = workspaceFolder("first");
        Path second = workspaceFolder("second");
        service.create(first);
        service.create(second);

        // A fresh service, as if the app had been restarted.
        WorkspaceService restarted = new WorkspaceService(
                new RecentWorkspaces(userHome.resolve(RecentWorkspaces.FILE_NAME)));
        Optional<WorkspacePaths> reopened = restarted.openRemembered();

        assertTrue(reopened.isPresent(), "the last workspace should be remembered");
        assertEquals(second.toAbsolutePath().normalize(), reopened.get().root());
    }

    @Test
    void multipleWorkspacesStayOpenableAndLastOpenedWins() {
        Path first = workspaceFolder("first");
        Path second = workspaceFolder("second");
        service.create(first);
        service.create(second);

        assertEquals(second.toAbsolutePath().normalize(), service.getRecent().lastOpened());
        assertTrue(service.getRecent().getPaths().contains(first.toAbsolutePath().normalize()),
                "the earlier workspace is still remembered");
        assertEquals(first.toAbsolutePath().normalize(), service.open(first).root(),
                "the earlier workspace still opens");
        assertEquals(first.toAbsolutePath().normalize(), service.getRecent().lastOpened(),
                "opening a workspace makes it the most recent");
    }

    @Test
    void noWorkspaceRememberedGivesEmpty() {
        assertTrue(service.openRemembered().isEmpty());
    }

    @Test
    void aNewerWorkspaceFailsClearlyAndIsLeftUntouched() throws IOException {
        Path folder = workspaceFolder("future");
        WorkspacePaths paths = service.create(folder);

        // Stamp the workspace as written by a later version.
        String metadata = Files.readString(paths.metadataFile(), StandardCharsets.UTF_8);
        int bumped = WorkspaceMetadata.CURRENT_WORKSPACE_VERSION + 3;
        String newer = metadata.replace(
                "\"workspaceVersion\": " + WorkspaceMetadata.CURRENT_WORKSPACE_VERSION,
                "\"workspaceVersion\": " + bumped);
        assertNotEquals(metadata, newer, "the version field should have been rewritten");
        Files.writeString(paths.metadataFile(), newer, StandardCharsets.UTF_8);
        String before = Files.readString(paths.metadataFile(), StandardCharsets.UTF_8);

        WorkspaceException failure = assertThrows(WorkspaceException.class, () ->
                service.open(folder));

        assertTrue(failure.getMessage().contains(String.valueOf(bumped)),
                "the message should name the newer version: " + failure.getMessage());
        assertTrue(failure.getMessage().toLowerCase().contains("newer"),
                "the message should say the workspace is newer: " + failure.getMessage());
        assertEquals(before, Files.readString(paths.metadataFile(), StandardCharsets.UTF_8),
                "the workspace must be left exactly as it was");
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

    @Test
    void recentListKeepsNewestFirstAndIsBounded() {
        RecentWorkspaces recent = new RecentWorkspaces(
                userHome.resolve(RecentWorkspaces.FILE_NAME));
        recent.load();

        for (int i = 0; i < RecentWorkspaces.LIMIT + 4; i++) {
            recent.remember(temporary.resolve("workspace-" + i));
        }

        List<Path> remembered = recent.getPaths();
        assertEquals(RecentWorkspaces.LIMIT, remembered.size(), "the list should be bounded");
        assertEquals(temporary.resolve("workspace-" + (RecentWorkspaces.LIMIT + 3)),
                remembered.get(0),
                "the newest entry should be first");
    }

    @Test
    void rememberingAnExistingWorkspaceMovesItToTheFront() {
        RecentWorkspaces recent = new RecentWorkspaces(
                userHome.resolve(RecentWorkspaces.FILE_NAME));
        recent.load();
        Path alpha = temporary.resolve("alpha");
        Path beta = temporary.resolve("beta");
        recent.remember(alpha);
        recent.remember(beta);
        recent.remember(alpha);

        assertEquals(alpha, recent.lastOpened());
        assertEquals(2, recent.getPaths().size(), "no duplicate entry");
    }

    @Test
    void recentListSurvivesARoundTripThroughDisk() {
        Path file = userHome.resolve(RecentWorkspaces.FILE_NAME);
        RecentWorkspaces written = new RecentWorkspaces(file);
        written.load();
        written.remember(temporary.resolve("alpha"));
        written.remember(temporary.resolve("beta"));

        RecentWorkspaces read = new RecentWorkspaces(file);
        read.load();

        assertEquals(written.getPaths(), read.getPaths());
    }

    @Test
    void rememberedPathsAreStoredAbsoluteSoTheyResolveFromAnywhere() {
        RecentWorkspaces recent = new RecentWorkspaces(
                userHome.resolve(RecentWorkspaces.FILE_NAME));
        recent.load();

        recent.remember(Path.of("relative-workspace"));

        assertTrue(recent.lastOpened().isAbsolute(),
                "a relative path is resolved when it is remembered");
    }

    @Test
    void aDamagedRecentListIsIgnoredRatherThanFatal() throws IOException {
        Path file = Files.createDirectories(userHome).resolve(RecentWorkspaces.FILE_NAME);
        Files.writeString(file, "{ this is not json", StandardCharsets.UTF_8);

        RecentWorkspaces recent = new RecentWorkspaces(file);
        recent.load(); // must not throw

        assertTrue(recent.getPaths().isEmpty(), "a damaged list reads as empty");
        assertFalse(Files.isDirectory(userHome.resolve("nonexistent")));
    }
}
