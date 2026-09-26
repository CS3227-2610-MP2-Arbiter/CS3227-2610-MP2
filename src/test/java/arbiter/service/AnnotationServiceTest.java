package arbiter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.SplitItem;
import arbiter.testing.ClassificationWorkflow;
import arbiter.testing.Records;
import arbiter.testing.TestWorkspace;

/** Integration checks for the annotator's own assignments and progress (#12) and their queue (#13). */
class AnnotationServiceTest {
    @TempDir
    Path temporary;

    private TestWorkspace workspace;

    @BeforeEach
    void createWorkspace() {
        workspace = TestWorkspace.create(temporary.resolve("workspace"));
    }

    @Test
    void forCurrentUser_annotatorsSharingASplit_eachSeesOnlyTheirOwn() {
        ClassificationWorkflow shared = ClassificationWorkflow.single("positive", "negative").items(2)
                .assign("alice", "positive").assign("bob", "negative", "negative").seed(workspace);
        ClassificationWorkflow bobsOther = ClassificationWorkflow.single("positive").assign("bob").seed(workspace);

        List<AssignmentProgress> alice = service("alice").forCurrentUser();
        List<AssignmentProgress> bob = service("bob").forCurrentUser();

        assertEquals(List.of(shared.assignmentId("alice")), ids(alice));
        assertEquals(List.of(bobsOther.assignmentId("bob"), shared.assignmentId("bob")), ids(bob));
    }

    @Test
    void forCurrentUser_progress_countsOnlyTheSignedInAnnotatorsAnswers() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(3)
                .assign("alice", "positive").assign("bob", "negative", "negative", "positive").seed(workspace);

        AssignmentProgress alice = service("alice").forCurrentUser().getFirst();

        assertEquals(new AssignmentProgress(flow.assignmentId("alice"), "Synthetic project", "Batch 1",
                AssignmentStatus.IN_PROGRESS, 1, 3, flow.itemId(1)), alice);
        assertFalse(alice.finished());
    }

    @Test
    void forCurrentUser_nothingAnswered_nextIsTheFirstFileInSavedOrder() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").items(3).assign("alice")
                .seed(workspace);
        reverseSplitOrder(flow);

        AssignmentProgress alice = service("alice").forCurrentUser().getFirst();

        assertEquals(0, alice.submitted());
        assertEquals(flow.itemId(2), alice.nextItemId());
    }

    @Test
    void forCurrentUser_answeredFileLastInSavedOrder_nextIsFirstUnanswered() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").items(3).assign("alice", "positive")
                .seed(workspace);
        // Alice answered item 0, which the reversed order puts last, so the next file is the new first one.
        reverseSplitOrder(flow);

        AssignmentProgress alice = service("alice").forCurrentUser().getFirst();

        assertEquals(1, alice.submitted());
        assertEquals(flow.itemId(2), alice.nextItemId());
    }

    @Test
    void forCurrentUser_everyFileAnswered_finishedWithNoNextFile() {
        ClassificationWorkflow.single("positive").items(2).assign("alice", "positive", "positive").seed(workspace);

        AssignmentProgress alice = service("alice").forCurrentUser().getFirst();

        assertEquals(AssignmentStatus.SUBMITTED, alice.status());
        assertEquals(2, alice.submitted());
        assertNull(alice.nextItemId());
        assertTrue(alice.finished());
    }

    @Test
    void forCurrentUser_noAssignments_empty() {
        ClassificationWorkflow.single("positive").annotator("alice").seed(workspace);

        assertEquals(List.of(), service("alice").forCurrentUser());
    }

    @Test
    void forCurrentUser_mixedStatuses_unfinishedFirstThenSubmittedEachInAssignmentOrder() {
        ClassificationWorkflow finishedFirst = ClassificationWorkflow.single("positive").assign("alice", "positive")
                .seed(workspace);
        ClassificationWorkflow started = ClassificationWorkflow.single("positive").items(2)
                .assign("alice", "positive").seed(workspace);
        ClassificationWorkflow untouched = ClassificationWorkflow.single("positive").assign("alice")
                .seed(workspace);

        List<AssignmentProgress> alice = service("alice").forCurrentUser();

        assertEquals(List.of(started.assignmentId("alice"), untouched.assignmentId("alice"),
                finishedFirst.assignmentId("alice")), ids(alice));
    }

    @Test
    void forCurrentUser_adjudicatorOrSignedOut_rejected() {
        ClassificationWorkflow.single("positive").assign("alice").seed(workspace);
        AuthService signedOut = workspace.signIn("alice");
        signedOut.logout();
        AnnotationService owner = new AnnotationService(workspace.store(), workspace.signInOwner(), workspace.paths());
        AnnotationService nobody = new AnnotationService(workspace.store(), signedOut, workspace.paths());

        assertThrows(AuthException.class, owner::forCurrentUser);
        assertThrows(AuthException.class, nobody::forCurrentUser);
    }

    @Test
    void forCurrentUser_accountDisabledAfterSignIn_rejected() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").assign("alice").seed(workspace);
        AnnotationService alice = service("alice");

        workspace.signInOwner().deactivateAnnotator(flow.annotatorId("alice"));

        assertThrows(AuthException.class, alice::forCurrentUser);
    }

    @Test
    void forCurrentUserQueue_firstFileAnswered_nextFileWithItsTextAndPosition() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").items(3).assign("alice", "positive")
                .seed(workspace);

        QueueView queue = service("alice").forCurrentUser(flow.assignmentId("alice"));

        assertEquals(1, queue.assignment().submitted());
        assertEquals(3, queue.assignment().total());
        assertEquals(new QueueItem(flow.itemId(1), "media/corpus-1/item-2.txt", "Synthetic item 2 of corpus 1",
                null), queue.current());
        assertTrue(queue.current().readable());
        assertFalse(queue.finished());
    }

    @Test
    void forCurrentUserQueue_savedOrderDiffersFromIdentifiers_opensAtFirstFileInSavedOrder() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").items(3).assign("alice")
                .seed(workspace);
        reverseSplitOrder(flow);

        QueueView queue = service("alice").forCurrentUser(flow.assignmentId("alice"));

        assertEquals(flow.itemId(2), queue.current().itemId());
    }

    @Test
    void forCurrentUserQueue_restartBeforeAndAfterAnAnswerIsStored_resumesAtFirstUnansweredFile() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").items(2).assign("alice")
                .seed(workspace);
        long assignmentId = flow.assignmentId("alice");

        long beforeFirst = service("alice").forCurrentUser(assignmentId).current().itemId();
        long beforeRestart = service("alice").forCurrentUser(assignmentId).current().itemId();
        // Stands in for Submit & next (#17), which is not built yet.
        workspace.store().write(session -> session.annotations().insert(Records.answer(flow.itemId(0), assignmentId,
                flow.annotatorId("alice"), flow.labelId("positive"))));
        long afterRestart = service("alice").forCurrentUser(assignmentId).current().itemId();

        assertEquals(flow.itemId(0), beforeFirst);
        assertEquals(flow.itemId(0), beforeRestart);
        assertEquals(flow.itemId(1), afterRestart);
    }

    @Test
    void forCurrentUserQueue_everyFileAnswered_finishedWithNoFile() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").items(2)
                .assign("alice", "positive", "positive").seed(workspace);

        QueueView queue = service("alice").forCurrentUser(flow.assignmentId("alice"));

        assertTrue(queue.finished());
        assertNull(queue.current());
        assertEquals(2, queue.assignment().submitted());
    }

    @Test
    void forCurrentUserQueue_anotherAnnotatorsAnswers_doNotMoveThePosition() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").items(3).assign("alice")
                .assign("bob", "positive", "positive").seed(workspace);

        QueueView queue = service("alice").forCurrentUser(flow.assignmentId("alice"));

        assertEquals(0, queue.assignment().submitted());
        assertEquals(flow.itemId(0), queue.current().itemId());
    }

    @Test
    void forCurrentUserQueue_anotherAnnotatorsAssignment_refusedExactlyLikeAMissingOne() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").assign("alice").assign("bob")
                .seed(workspace);
        AnnotationService alice = service("alice");

        long bobs = flow.assignmentId("bob");

        ProjectException foreign = assertThrows(ProjectException.class, () -> alice.forCurrentUser(bobs));
        ProjectException missing = assertThrows(ProjectException.class, () -> alice.forCurrentUser(999_999L));

        assertEquals(missing.getMessage(), foreign.getMessage());
    }

    @Test
    void forCurrentUserQueue_changedSource_errorInsteadOfText() throws IOException {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").assign("alice").seed(workspace);
        Files.writeString(workspace.paths().root().resolve("media/corpus-1/item-1.txt"), "Edited after import");

        QueueItem current = service("alice").forCurrentUser(flow.assignmentId("alice")).current();

        assertFalse(current.readable());
        assertNull(current.text());
        assertTrue(current.sourceError().contains("media/corpus-1/item-1.txt"), current.sourceError());
    }

    @Test
    void forCurrentUserQueue_missingSource_errorInsteadOfText() throws IOException {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").assign("alice").seed(workspace);
        Files.delete(workspace.paths().root().resolve("media/corpus-1/item-1.txt"));

        QueueItem current = service("alice").forCurrentUser(flow.assignmentId("alice")).current();

        assertFalse(current.readable());
        assertTrue(current.sourceError().contains("media/corpus-1/item-1.txt"), current.sourceError());
    }

    @Test
    void forCurrentUserQueue_adjudicator_rejected() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").assign("alice").seed(workspace);
        AnnotationService owner = new AnnotationService(workspace.store(), workspace.signInOwner(), workspace.paths());

        assertThrows(AuthException.class, () -> owner.forCurrentUser(flow.assignmentId("alice")));
    }

    private AnnotationService service(String username) {
        return new AnnotationService(workspace.store(), workspace.signIn(username), workspace.paths());
    }

    private void reverseSplitOrder(ClassificationWorkflow flow) {
        workspace.store().write(session -> {
            List<SplitItem> members = session.splitItems().listBySplit(flow.splitId());
            for (SplitItem member : members) {
                member.setSequence(members.size() + 1 - member.getSequence());
                session.splitItems().save(member);
            }
            return null;
        });
    }

    private static List<Long> ids(List<AssignmentProgress> assignments) {
        return assignments.stream().map(AssignmentProgress::assignmentId).toList();
    }
}
