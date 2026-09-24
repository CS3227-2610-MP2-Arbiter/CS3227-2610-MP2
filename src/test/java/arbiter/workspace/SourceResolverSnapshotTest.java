package arbiter.workspace;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.data.json.JsonStore;
import arbiter.model.project.Item;
import arbiter.model.project.OutputFormat;
import arbiter.model.project.Project;

/** Checks that reading a source never changes the stored project records. */
class SourceResolverSnapshotTest {
    private static final String TEXT_PATH = "media/corpus/review.txt";
    private static final String REGISTERED_TEXT = "Registered text";

    @TempDir
    Path temporary;

    private WorkspacePaths paths;
    private JsonStore store;
    private SourceResolver resolver;
    private Item registered;

    @BeforeEach
    void setUp() throws IOException {
        paths = new WorkspaceService().create(temporary.resolve("workspace"));
        store = JsonStore.initializeNew(paths);
        resolver = new SourceResolver(paths);
        byte[] bytes = REGISTERED_TEXT.getBytes(StandardCharsets.UTF_8);
        Path file = paths.root().resolve(TEXT_PATH);
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
        Project project = new Project();
        project.setName("Project");
        project.setOutputFormat(OutputFormat.JSON);
        project.setCreatedAt(Instant.parse("2026-09-20T08:30:00Z"));
        long projectId = store.write(session -> session.projects().save(project).getId());
        Item item = new Item();
        item.setProjectId(projectId);
        item.setPath(TEXT_PATH);
        item.setContentHash(SourceResolver.hash(bytes));
        item.setImportedAt(Instant.parse("2026-09-20T08:30:00Z"));
        registered = store.write(session -> session.items().save(item));
    }

    @Test
    void resolve_sourceMissing_snapshotUnchanged() throws IOException {
        byte[] before = Files.readAllBytes(paths.dataFile());
        Files.delete(paths.root().resolve(TEXT_PATH));

        assertThrows(SourceException.class, () -> resolver.resolve(registered.getPath(), registered.getContentHash()));

        assertArrayEquals(before, Files.readAllBytes(paths.dataFile()));
        assertEquals(registered.getContentHash(), storedItem().getContentHash());
        assertEquals(TEXT_PATH, storedItem().getPath());
    }

    @Test
    void resolve_sourceChanged_snapshotUnchanged() throws IOException {
        writeFile("replaced");
        byte[] before = Files.readAllBytes(paths.dataFile());

        assertThrows(SourceException.class, () -> resolver.resolve(registered.getPath(), registered.getContentHash()));

        assertArrayEquals(before, Files.readAllBytes(paths.dataFile()));
        assertEquals(registered.getContentHash(), storedItem().getContentHash());
    }

    @Test
    void resolve_originalBytesRestoredNoStoreChange_resolved() throws IOException {
        byte[] before = Files.readAllBytes(paths.dataFile());
        writeFile("replaced");
        assertThrows(SourceException.class, () -> resolver.resolve(registered.getPath(), registered.getContentHash()));
        writeFile(REGISTERED_TEXT);

        ResolvedSource source = resolver.resolve(registered.getPath(), registered.getContentHash());

        assertEquals(REGISTERED_TEXT, source.text());
        assertArrayEquals(before, Files.readAllBytes(paths.dataFile()));
    }

    private void writeFile(String text) throws IOException {
        Files.writeString(paths.root().resolve(TEXT_PATH), text, StandardCharsets.UTF_8);
    }

    private Item storedItem() {
        return store.read(session -> session.items().findById(registered.getId()).orElseThrow());
    }
}
