package arbiter.testing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.Item;
import arbiter.model.project.Split;
import arbiter.model.project.SplitItem;
import arbiter.model.user.Role;
import arbiter.model.user.User;
import arbiter.service.AuthException;
import arbiter.service.CurrentUser;
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

    @Test
    void signInOwner_newWorkspace_ownerCreatedOnceAndSignedIn() {
        TestWorkspace workspace = TestWorkspace.create(temporary.resolve("workspace"));

        CurrentUser first = workspace.signInOwner().requireAdjudicator();
        CurrentUser second = workspace.signInOwner().requireAdjudicator();

        assertEquals(TestWorkspace.OWNER, first.username());
        assertEquals(first, second);
        assertEquals(1, workspace.store().<Integer>read(session -> session.users().listAll().size()));
    }

    @Test
    void dataFileBytes_beforeAndAfterWrite_bytesOnDisk() throws IOException {
        TestWorkspace workspace = TestWorkspace.create(temporary.resolve("workspace"));
        byte[] before = workspace.dataFileBytes();

        workspace.store().write(session -> session.users().save(Records.user("alice")));

        assertFalse(Arrays.equals(before, workspace.dataFileBytes()));
        assertArrayEquals(Files.readAllBytes(workspace.paths().dataFile()), workspace.dataFileBytes());
    }

    @Test
    void assignNewSplit_seededProject_readableItemInSplitAssignedNotStarted() {
        TestWorkspace workspace = TestWorkspace.create(temporary.resolve("workspace"));
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").annotator("alice").seed(workspace);

        workspace.assignNewSplit(flow.projectId(), flow.annotatorId("alice"));

        List<Split> splits = workspace.store().read(session -> session.splits().listByProject(flow.projectId()));
        assertEquals(2, splits.size());
        Split later = splits.stream().filter(split -> split.getId() != flow.splitId()).findFirst().orElseThrow();
        List<SplitItem> members = workspace.store().read(session -> session.splitItems().listBySplit(later.getId()));
        assertEquals(1, members.size());
        Item item = workspace.store().read(session -> session.items().findById(members.getFirst().getItemId()))
                .orElseThrow();
        assertEquals(flow.projectId(), item.getProjectId());
        new SourceResolver(workspace.paths()).resolve(item.getPath(), item.getContentHash());
        List<Assignment> assignments = workspace.store().read(session ->
                session.assignments().listBySplit(later.getId()));
        assertEquals(1, assignments.size());
        assertEquals(flow.annotatorId("alice"), assignments.getFirst().getAnnotatorId());
        assertEquals(AssignmentStatus.NOT_STARTED, assignments.getFirst().getStatus());
    }

    @Test
    void assignSplit_unassignedSplit_newAnnotatorAssignedNotStarted() {
        TestWorkspace workspace = TestWorkspace.create(temporary.resolve("workspace"));
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").seed(workspace);

        workspace.assignSplit(flow.splitId(), "alice");

        List<Assignment> assignments = workspace.store().read(session ->
                session.assignments().listBySplit(flow.splitId()));
        assertEquals(1, assignments.size());
        assertEquals(AssignmentStatus.NOT_STARTED, assignments.getFirst().getStatus());
        User annotator = workspace.store().read(session ->
                session.users().findById(assignments.getFirst().getAnnotatorId())).orElseThrow();
        assertEquals("alice", annotator.getUsername());
        assertEquals(Role.ANNOTATOR, annotator.getRole());
    }

    private static boolean isPristine(JsonStore store) {
        return store.read(RepositorySession::isPristine);
    }
}
