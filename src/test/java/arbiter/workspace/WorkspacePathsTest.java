package arbiter.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class WorkspacePathsTest {
    private static final Path WORKING_DIRECTORY = Path.of("").toAbsolutePath();

    @Test
    void constructor_nullRoot_exceptionThrown() {
        assertThrows(NullPointerException.class, () -> new WorkspacePaths(null));
    }

    @Test
    void constructor_relativeRoot_madeAbsolute() {
        WorkspacePaths paths = new WorkspacePaths(Path.of("workspace"));

        assertEquals(WORKING_DIRECTORY.resolve("workspace"), paths.root());
    }

    @Test
    void constructor_nonNormalisedRoot_normalised() {
        Path root = WORKING_DIRECTORY.resolve("other").resolve("..").resolve("workspace");

        WorkspacePaths paths = new WorkspacePaths(root);

        assertEquals(WORKING_DIRECTORY.resolve("workspace"), paths.root());
    }

    @Test
    void layoutPaths() {
        Path root = WORKING_DIRECTORY.resolve("workspace");
        WorkspacePaths paths = new WorkspacePaths(root);

        assertEquals(root, paths.root());
        assertEquals(root.resolve("workspace.json"), paths.metadataFile());
        assertEquals(root.resolve("arbiter.json"), paths.dataFile());
        assertEquals(root.resolve("media"), paths.mediaDirectory());
        assertEquals(root.resolve("exports"), paths.exportsDirectory());
        assertEquals(root.resolve("logs"), paths.logsDirectory());
    }
}
