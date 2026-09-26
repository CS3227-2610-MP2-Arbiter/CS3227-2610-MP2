package arbiter.testing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import arbiter.data.json.JsonStore;
import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.Split;
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
        return new SourceResolver(paths).resolveForImport(file);
    }

    /** Returns a new authentication session signed in as this fixture account. */
    public AuthService signIn(String username) {
        AuthService auth = new AuthService(store);
        auth.login(username, PASSWORD);
        return auth;
    }

    /** Returns a new authentication session signed in as the owner, creating the owner first if there is none. */
    public AuthService signInOwner() {
        ensureOwner();
        return signIn(OWNER);
    }

    /** Returns the bytes of the workspace's data file, to check that a rejected write changed nothing. */
    public byte[] dataFileBytes() {
        try {
            return Files.readAllBytes(paths.dataFile());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the data file", e);
        }
    }

    /** Adds a split holding one new item to a project and assigns it to this annotator, not started. */
    public void assignNewSplit(long projectId, long annotatorId) {
        ResolvedSource source = writeSource("later/item.txt", "A later synthetic item");
        store.write(session -> {
            long itemId = session.items().save(Records.item(projectId, source)).getId();
            Split split = Records.split(projectId, "Batch 2");
            split.setAnnotationsPerItem(1);
            long splitId = session.splits().save(split).getId();
            session.splitItems().save(Records.membership(splitId, itemId));
            session.assignments().save(notStarted(splitId, annotatorId));
            return null;
        });
    }

    /**
     * Gives a split its first assignment, to a new annotator account with placeholder credentials, not started,
     * and saves k = 1 with it, as #32 does.
     */
    public void assignSplit(long splitId, String username) {
        store.write(session -> {
            Split split = session.splits().findById(splitId).orElseThrow();
            split.setAnnotationsPerItem(1);
            session.splits().save(split);
            long annotatorId = session.users().save(Records.user(username)).getId();
            return session.assignments().save(notStarted(splitId, annotatorId));
        });
    }

    /** Creates the owner through {@link AuthService} if the workspace has none. */
    void ensureOwner() {
        AuthService setup = new AuthService(store);
        if (setup.needsBootstrap()) {
            setup.bootstrapOwner(OWNER, PASSWORD);
        }
    }

    private static Assignment notStarted(long splitId, long annotatorId) {
        Assignment assignment = Records.assignment(splitId, annotatorId);
        assignment.setStatus(AssignmentStatus.NOT_STARTED);
        return assignment;
    }
}
