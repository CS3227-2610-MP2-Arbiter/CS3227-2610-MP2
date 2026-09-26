package arbiter.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.model.annotation.Annotation;
import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.SplitItem;
import arbiter.model.project.TaxonomyKind;
import arbiter.testing.ClassificationWorkflow;
import arbiter.testing.Records;
import arbiter.testing.TestWorkspace;
import arbiter.workspace.SourceFailure;

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
        ClassificationWorkflow bobsOther = ClassificationWorkflow.single("positive", "negative").assign("bob")
                .seed(workspace);

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
                AssignmentStatus.IN_PROGRESS, 1, 3), alice);
        assertFalse(alice.finished());
    }

    @Test
    void forCurrentUser_everyFileAnswered_finished() {
        ClassificationWorkflow.single("positive", "negative").items(2).assign("alice", "positive", "positive")
                .seed(workspace);

        AssignmentProgress alice = service("alice").forCurrentUser().getFirst();

        assertEquals(AssignmentStatus.SUBMITTED, alice.status());
        assertEquals(2, alice.submitted());
        assertTrue(alice.finished());
    }

    @Test
    void forCurrentUser_noAssignments_empty() {
        ClassificationWorkflow.single("positive", "negative").annotator("alice").seed(workspace);

        assertEquals(List.of(), service("alice").forCurrentUser());
    }

    @Test
    void forCurrentUser_mixedStatuses_unfinishedFirstThenSubmittedEachInAssignmentOrder() {
        ClassificationWorkflow finishedFirst = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").seed(workspace);
        ClassificationWorkflow started = ClassificationWorkflow.single("positive", "negative").items(2)
                .assign("alice", "positive").seed(workspace);
        ClassificationWorkflow untouched = ClassificationWorkflow.single("positive", "negative").assign("alice")
                .seed(workspace);

        List<AssignmentProgress> alice = service("alice").forCurrentUser();

        assertEquals(List.of(started.assignmentId("alice"), untouched.assignmentId("alice"),
                finishedFirst.assignmentId("alice")), ids(alice));
    }

    @Test
    void forCurrentUser_adjudicatorOrSignedOut_rejected() {
        ClassificationWorkflow.single("positive", "negative").assign("alice").seed(workspace);
        AuthService signedOut = workspace.signIn("alice");
        signedOut.logout();
        AnnotationService owner = new AnnotationService(workspace.store(), workspace.signInOwner(), workspace.paths());
        AnnotationService nobody = new AnnotationService(workspace.store(), signedOut, workspace.paths());

        assertThrows(AuthException.class, owner::forCurrentUser);
        assertThrows(AuthException.class, nobody::forCurrentUser);
    }

    @Test
    void forCurrentUser_accountDisabledAfterSignIn_rejected() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").assign("alice")
                .seed(workspace);
        AnnotationService alice = service("alice");

        workspace.signInOwner().deactivateAnnotator(flow.annotatorId("alice"));

        assertThrows(AuthException.class, alice::forCurrentUser);
    }

    @Test
    void forCurrentUserQueue_firstFileAnswered_nextFileWithItsTextAndPosition() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(3)
                .assign("alice", "positive").seed(workspace);

        QueueView queue = service("alice").forCurrentUser(flow.assignmentId("alice"));

        assertEquals(1, queue.assignment().submitted());
        assertEquals(3, queue.assignment().total());
        assertEquals(new QueueItem(flow.itemId(1), "Synthetic item 2 of corpus 1", null), queue.current());
        assertTrue(queue.current().readable());
        assertFalse(queue.assignment().finished());
    }

    @Test
    void forCurrentUserQueue_savedOrderDiffersFromIdentifiers_opensAtFirstFileInSavedOrder() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(3).assign("alice")
                .seed(workspace);
        reverseSplitOrder(flow);

        QueueView queue = service("alice").forCurrentUser(flow.assignmentId("alice"));

        assertEquals(flow.itemId(2), queue.current().itemId());
    }

    @Test
    void forCurrentUserQueue_answeredFileLastInSavedOrder_opensAtFirstUnansweredFile() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(3)
                .assign("alice", "positive").seed(workspace);
        // Alice answered item 0, which the reversed order puts last, so the queue opens at the new first file.
        reverseSplitOrder(flow);

        QueueView queue = service("alice").forCurrentUser(flow.assignmentId("alice"));

        assertEquals(1, queue.assignment().submitted());
        assertEquals(flow.itemId(2), queue.current().itemId());
    }

    @Test
    void forCurrentUserQueue_restartBeforeAndAfterAnAnswerIsStored_resumesAtFirstUnansweredFile() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(2).assign("alice")
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
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(2)
                .assign("alice", "positive", "positive").seed(workspace);

        QueueView queue = service("alice").forCurrentUser(flow.assignmentId("alice"));

        assertTrue(queue.assignment().finished());
        assertNull(queue.current());
        assertEquals(2, queue.assignment().submitted());
    }

    @Test
    void forCurrentUserQueue_storedAsSubmittedWithAFileLeft_finishedWithoutReopening() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(2)
                .assign("alice", "positive").seed(workspace);
        workspace.store().write(session -> {
            Assignment assignment = session.assignments().findById(flow.assignmentId("alice")).orElseThrow();
            assignment.setStatus(AssignmentStatus.SUBMITTED);
            return session.assignments().save(assignment);
        });

        QueueView queue = service("alice").forCurrentUser(flow.assignmentId("alice"));

        assertTrue(queue.assignment().finished());
        assertNull(queue.current());
        assertEquals(1, queue.assignment().submitted());
    }

    @Test
    void forCurrentUserQueue_everyFileAnsweredButNotSubmitted_inconsistentStateRefused() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").assign("alice", "positive")
                .seed(workspace);
        workspace.store().write(session -> {
            Assignment assignment = session.assignments().findById(flow.assignmentId("alice")).orElseThrow();
            assignment.setStatus(AssignmentStatus.IN_PROGRESS);
            return session.assignments().save(assignment);
        });
        AnnotationService alice = service("alice");
        long assignmentId = flow.assignmentId("alice");

        assertThrows(IllegalStateException.class, () -> alice.forCurrentUser(assignmentId));
    }

    @Test
    void forCurrentUserQueue_anotherAnnotatorsAnswers_doNotMoveThePosition() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(3).assign("alice")
                .assign("bob", "positive", "positive").seed(workspace);

        QueueView queue = service("alice").forCurrentUser(flow.assignmentId("alice"));

        assertEquals(0, queue.assignment().submitted());
        assertEquals(flow.itemId(0), queue.current().itemId());
    }

    @Test
    void forCurrentUserQueue_anotherAnnotatorsAssignment_refusedExactlyLikeAMissingOne() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice").assign("bob").seed(workspace);
        AnnotationService alice = service("alice");

        long bobs = flow.assignmentId("bob");

        ProjectException foreign = assertThrows(ProjectException.class, () -> alice.forCurrentUser(bobs));
        ProjectException missing = assertThrows(ProjectException.class, () -> alice.forCurrentUser(999_999L));

        assertEquals(missing.getMessage(), foreign.getMessage());
    }

    @Test
    void forCurrentUserQueue_changedSource_errorInsteadOfText() throws IOException {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").assign("alice")
                .seed(workspace);
        Files.writeString(workspace.paths().root().resolve("media/corpus-1/item-1.txt"), "Edited after import");

        QueueItem current = service("alice").forCurrentUser(flow.assignmentId("alice")).current();

        assertFalse(current.readable());
        assertNull(current.text());
        assertEquals(SourceFailure.HASH_MISMATCH, current.failure());
    }

    @Test
    void forCurrentUserQueue_missingSource_errorInsteadOfText() throws IOException {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").assign("alice")
                .seed(workspace);
        Files.delete(workspace.paths().root().resolve("media/corpus-1/item-1.txt"));

        QueueItem current = service("alice").forCurrentUser(flow.assignmentId("alice")).current();

        assertFalse(current.readable());
        assertEquals(SourceFailure.MISSING, current.failure());
    }

    @Test
    void forCurrentUserQueue_singleProject_labelsInSavedOrderWithDescriptions() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").assign("alice")
                .seed(workspace);

        Taxonomy taxonomy = service("alice").forCurrentUser(flow.assignmentId("alice")).taxonomy();

        assertEquals(new Taxonomy(TaxonomyKind.SINGLE, List.of(
                new LabelOption(flow.labelId("positive"), "positive", "Synthetic label"),
                new LabelOption(flow.labelId("negative"), "negative", "Synthetic label")), null, null), taxonomy);
    }

    @Test
    void forCurrentUserQueue_scaleProject_rangeWithNoLabels() {
        ClassificationWorkflow flow = ClassificationWorkflow.scale(-2, 5).assign("alice").seed(workspace);

        Taxonomy taxonomy = service("alice").forCurrentUser(flow.assignmentId("alice")).taxonomy();

        assertEquals(new Taxonomy(TaxonomyKind.SCALE, List.of(), -2, 5), taxonomy);
    }

    @Test
    void forCurrentUserQueue_projectWithoutLabelsYet_unanswerable() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").assign("alice")
                .seed(workspace);
        workspace.store().write(session -> {
            session.labels().deleteById(flow.labelId("positive"));
            session.labels().deleteById(flow.labelId("negative"));
            return null;
        });

        Taxonomy taxonomy = service("alice").forCurrentUser(flow.assignmentId("alice")).taxonomy();

        assertFalse(taxonomy.answerable());
    }

    @Test
    void forCurrentUserQueue_opening_writesNothing() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(2).assign("alice")
                .seed(workspace);
        byte[] before = workspace.dataFileBytes();

        service("alice").forCurrentUser(flow.assignmentId("alice"));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void forCurrentUserQueue_adjudicator_rejected() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").assign("alice")
                .seed(workspace);
        AnnotationService owner = new AnnotationService(workspace.store(), workspace.signInOwner(), workspace.paths());

        assertThrows(AuthException.class, () -> owner.forCurrentUser(flow.assignmentId("alice")));
    }

    @Test
    void submit_validLabel_storedWithTimeInProgressAndNextFileReturned() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(2).assign("alice")
                .seed(workspace);
        long assignmentId = flow.assignmentId("alice");
        Instant before = Instant.now();

        QueueView next = service("alice").submit(assignmentId, flow.itemId(0), Answer.label(flow.labelId("negative")));

        Annotation stored = answerOf(flow.itemId(0), flow.annotatorId("alice")).orElseThrow();
        assertEquals(flow.labelId("negative"), stored.getLabelId());
        assertEquals(assignmentId, stored.getAssignmentId());
        assertFalse(stored.getSubmittedAt().isBefore(before));
        assertEquals(AssignmentStatus.IN_PROGRESS, statusOf(assignmentId));
        assertEquals(flow.itemId(1), next.current().itemId());
        assertEquals(1, next.assignment().submitted());
    }

    @Test
    void submit_lastFile_assignmentSubmittedAndQueueFinished() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").items(2).assign("alice", "positive")
                .seed(workspace);
        long assignmentId = flow.assignmentId("alice");

        QueueView next = service("alice").submit(assignmentId, flow.itemId(1), Answer.label(flow.labelId("positive")));

        assertEquals(AssignmentStatus.SUBMITTED, statusOf(assignmentId));
        assertTrue(next.assignment().finished());
        assertNull(next.current());
    }

    @Test
    void submit_scaleRating_stored() {
        ClassificationWorkflow flow = ClassificationWorkflow.scale(-2, 5).assign("alice").seed(workspace);

        service("alice").submit(flow.assignmentId("alice"), flow.itemId(0), Answer.rating(-2));

        assertEquals(-2, answerOf(flow.itemId(0), flow.annotatorId("alice")).orElseThrow().getScaleValue());
    }

    @Test
    void submit_sameFileTwice_secondRefusedAndNothingMoreStored() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").items(2).assign("alice")
                .seed(workspace);
        AnnotationService alice = service("alice");
        long assignmentId = flow.assignmentId("alice");
        Answer positive = Answer.label(flow.labelId("positive"));
        alice.submit(assignmentId, flow.itemId(0), positive);
        byte[] afterFirst = workspace.dataFileBytes();

        assertThrows(ProjectException.class, () -> alice.submit(assignmentId, flow.itemId(0), positive));

        assertArrayEquals(afterFirst, workspace.dataFileBytes());
    }

    @Test
    void submit_notTheNextFile_refusedAndNothingStored() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").items(2).assign("alice")
                .seed(workspace);

        assertRefusedUnchanged("alice", flow.assignmentId("alice"), flow.itemId(1),
                Answer.label(flow.labelId("positive")));
    }

    @Test
    void submit_finishedAssignment_refusedAndNothingStored() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").assign("alice", "positive")
                .seed(workspace);

        assertRefusedUnchanged("alice", flow.assignmentId("alice"), flow.itemId(0),
                Answer.label(flow.labelId("positive")));
    }

    @Test
    void submit_anotherAnnotatorsAssignment_refusedLikeAMissingOne() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").assign("alice").assign("bob")
                .seed(workspace);
        AnnotationService alice = service("alice");
        Answer positive = Answer.label(flow.labelId("positive"));
        long bobs = flow.assignmentId("bob");
        long item = flow.itemId(0);
        byte[] before = workspace.dataFileBytes();

        ProjectException foreign = assertThrows(ProjectException.class, () -> alice.submit(bobs, item, positive));
        ProjectException missing = assertThrows(ProjectException.class, () -> alice.submit(999_999L, item, positive));

        assertEquals(missing.getMessage(), foreign.getMessage());
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void submit_answerOutsideTheTaxonomy_refusedAndNothingStored() {
        ClassificationWorkflow labels = ClassificationWorkflow.single("positive").assign("alice").seed(workspace);
        ClassificationWorkflow scale = ClassificationWorkflow.scale(1, 5).assign("alice").seed(workspace);
        ClassificationWorkflow other = ClassificationWorkflow.single("elsewhere").seed(workspace);

        assertRefusedUnchanged("alice", labels.assignmentId("alice"), labels.itemId(0),
                Answer.label(other.labelId("elsewhere")));
        assertRefusedUnchanged("alice", labels.assignmentId("alice"), labels.itemId(0), Answer.rating(1));
        assertRefusedUnchanged("alice", scale.assignmentId("alice"), scale.itemId(0), Answer.rating(6));
        assertRefusedUnchanged("alice", scale.assignmentId("alice"), scale.itemId(0),
                Answer.label(labels.labelId("positive")));
    }

    @Test
    void submit_changedSource_refusedWithoutNamingTheFile() throws IOException {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").assign("alice").seed(workspace);
        Files.writeString(workspace.paths().root().resolve("media/corpus-1/item-1.txt"), "Edited after import");

        ProjectException refused = assertRefusedUnchanged("alice", flow.assignmentId("alice"), flow.itemId(0),
                Answer.label(flow.labelId("positive")));

        assertFalse(refused.getMessage().contains("item-1"), refused.getMessage());
    }

    @Test
    void submit_thenRestart_freshSessionResumesAtTheNextFile() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").items(3).assign("alice")
                .seed(workspace);
        long assignmentId = flow.assignmentId("alice");
        service("alice").submit(assignmentId, flow.itemId(0), Answer.label(flow.labelId("positive")));

        QueueView afterRestart = service("alice").forCurrentUser(assignmentId);

        assertEquals(flow.itemId(1), afterRestart.current().itemId());
    }

    @Test
    void submit_adjudicator_rejected() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").assign("alice").seed(workspace);
        AnnotationService owner = new AnnotationService(workspace.store(), workspace.signInOwner(), workspace.paths());
        long assignmentId = flow.assignmentId("alice");
        Answer positive = Answer.label(flow.labelId("positive"));

        assertThrows(AuthException.class, () -> owner.submit(assignmentId, flow.itemId(0), positive));
    }

    private ProjectException assertRefusedUnchanged(String username, long assignmentId, long itemId, Answer answer) {
        AnnotationService annotator = service(username);
        byte[] before = workspace.dataFileBytes();
        ProjectException refused = assertThrows(ProjectException.class, () -> annotator.submit(assignmentId,
                itemId, answer));
        assertArrayEquals(before, workspace.dataFileBytes());
        return refused;
    }

    private Optional<Annotation> answerOf(long itemId, long annotatorId) {
        return workspace.store().read(session -> session.annotations().findByItemAndAnnotator(itemId, annotatorId));
    }

    private AssignmentStatus statusOf(long assignmentId) {
        return workspace.store().read(session -> session.assignments().findById(assignmentId).orElseThrow())
                .getStatus();
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
