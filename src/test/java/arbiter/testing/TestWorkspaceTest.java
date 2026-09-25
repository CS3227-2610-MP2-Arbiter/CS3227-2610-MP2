package arbiter.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.user.Role;
import arbiter.service.AuthException;
import arbiter.workspace.ResolvedSource;
import arbiter.workspace.SourceResolver;

/** Checks that each test workspace is fresh, separate and usable through the production boundaries. */
class TestWorkspaceTest {
    @TempDir
    Path temporary;

    @Test
    void create_newFolder_pristineWithInitializedSnapshot() {
        TestWorkspace workspace = TestWorkspace.create(temporary.resolve("workspace"));

        assertTrue(Files.isRegularFile(workspace.paths().dataFile()));
        assertTrue(isPristine(workspace.store()));
        assertTrue(isPristine(JsonStore.open(workspace.paths())));
    }

    @Test
    void create_twoWorkspaces_noSharedState() {
        TestWorkspace first = TestWorkspace.create(temporary.resolve("first"));
        TestWorkspace second = TestWorkspace.create(temporary.resolve("second"));

        first.store().write(session -> session.users().save(Records.user("alice")));

        assertFalse(isPristine(first.store()));
        assertTrue(isPristine(second.store()));
    }

    @Test
    void writeSource_nestedPath_registeredAndReadableWithRecordedHash() {
        TestWorkspace workspace = TestWorkspace.create(temporary.resolve("workspace"));

        ResolvedSource registered = workspace.writeSource("reviews/one.txt", "Great value");

        assertEquals("media/reviews/one.txt", registered.storedPath());
        ResolvedSource read = new SourceResolver(workspace.paths())
                .resolve(registered.storedPath(), registered.contentHash());
        assertEquals("Great value", read.text());
    }

    @Test
    void signIn_seededAccounts_signedInWithTheirRole() {
        TestWorkspace workspace = TestWorkspace.create(temporary.resolve("workspace"));
        ClassificationWorkflow.single("positive").assign("alice").seed(workspace);

        assertEquals(Role.ADJUDICATOR, workspace.signIn(TestWorkspace.OWNER).requireAdjudicator().role());
        assertEquals(Role.ANNOTATOR, workspace.signIn("alice").currentUser().orElseThrow().role());
    }

    @Test
    void signIn_unknownAccount_exceptionThrown() {
        TestWorkspace workspace = TestWorkspace.create(temporary.resolve("workspace"));
        ClassificationWorkflow.single("positive").seed(workspace);

        assertThrows(AuthException.class, () -> workspace.signIn("nobody"));
    }

    private static boolean isPristine(JsonStore store) {
        return store.read(RepositorySession::isPristine);
    }
}
