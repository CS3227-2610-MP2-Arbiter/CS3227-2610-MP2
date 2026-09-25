package arbiter.testing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import arbiter.data.json.JsonStore;
import arbiter.service.AuthService;
import arbiter.workspace.ResolvedSource;
import arbiter.workspace.SourceResolver;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;

/**
 * A new workspace with an empty data snapshot, owned by one test.
 *
 * <p>Each instance lives in the folder its test supplies, normally under that test's
 * {@code @TempDir}, so no two tests share stored state. Seed it with {@link ClassificationWorkflow}.
 */
public final class TestWorkspace {
    /** Username of the workspace owner, the sole adjudicator. */
    public static final String OWNER = "owner";

    /** Password of every account a fixture creates, which satisfies the login rules. */
    public static final String PASSWORD = "password8";

    private final WorkspacePaths paths;
    private final JsonStore store;

    private TestWorkspace(WorkspacePaths paths, JsonStore store) {
        this.paths = paths;
        this.store = store;
    }

    /**
     * Creates a workspace and initializes its data snapshot.
     *
     * @param folder a folder that does not yet hold a workspace
     */
    public static TestWorkspace create(Path folder) {
        WorkspacePaths paths = new WorkspaceService().create(Objects.requireNonNull(folder, "folder"));
        return new TestWorkspace(paths, JsonStore.initializeNew(paths));
    }

    /** Returns the workspace's paths. */
    public WorkspacePaths paths() {
        return paths;
    }

    /** Returns the workspace's data store. */
    public JsonStore store() {
        return store;
    }

    /**
     * Writes a UTF-8 text file under {@code media/} and registers it as import would ([#10]).
     *
     * @param relativePath the path below {@code media/}, using {@code /} between segments
     * @return the stored path and content hash to record on an item
     */
    public ResolvedSource writeSource(String relativePath, String text) {
        String storedPath = WorkspacePaths.MEDIA_DIRECTORY + "/" + relativePath;
        Path file = paths.root().resolve(storedPath);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the test source " + storedPath, e);
        }
        return new SourceResolver(paths).resolveForImport(storedPath);
    }

    /** Returns a new authentication session signed in as this fixture account. */
    public AuthService signIn(String username) {
        AuthService auth = new AuthService(store);
        auth.login(username, PASSWORD);
        return auth;
    }
}
