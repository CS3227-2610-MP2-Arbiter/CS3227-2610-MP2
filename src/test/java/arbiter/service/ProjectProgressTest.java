package arbiter.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.Split;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.user.AccountStatus;
import arbiter.testing.ClassificationWorkflow;
import arbiter.testing.Records;
import arbiter.testing.TestWorkspace;
import arbiter.workspace.ResolvedSource;

/** Acceptance checks for the adjudicator's project progress snapshot (#33). */
class ProjectProgressTest {
    @TempDir
    Path temporary;

    private TestWorkspace workspace;

    @BeforeEach
    void createWorkspace() {
        workspace = TestWorkspace.create(temporary.resolve("workspace"));
    }

    @Test
    void progress_emptyProject_zeroCountsAndRows() {
        long projectId = workspace.newProject(TaxonomyKind.SINGLE);

        ProjectProgress progress = service().progress(projectId);

        assertEquals(0, progress.itemCount());
        assertEquals(0, progress.submitted());
        assertEquals(0, progress.total());
        assertEquals(0, progress.unresolvedCount());
        assertEquals(0, progress.unresolvedDisputeCount());
        assertTrue(progress.splits().isEmpty());
        assertTrue(progress.annotators().isEmpty());
    }

    @Test
    void progress_unassignedSplit_kUnsetAndNoAnswerOpportunities() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("yes", "no").items(2)
                .annotator("waiting").seed(workspace);
        byte[] before = workspace.dataFileBytes();

        ProjectProgress progress = service().progress(flow.projectId());

        assertEquals(2, progress.itemCount());
        assertEquals(0, progress.submitted());
        assertEquals(0, progress.total());
        assertEquals(2, progress.unresolvedCount());
        assertEquals(0, progress.unresolvedDisputeCount());
        assertTrue(progress.annotators().isEmpty());
        ProjectProgress.SplitProgress split = progress.splits().getFirst();
        assertEquals(flow.splitId(), split.summary().id());
        assertEquals(2, split.summary().itemIds().size());
        assertNull(split.summary().annotationsPerItem());
        assertEquals(0, split.summary().assignmentCount());
        assertNull(split.summary().vacantPlaces());
        assertEquals(0, split.total());
        assertTrue(split.assignments().isEmpty());
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void progress_partlyFilledKAndMultipleSplits_countsOnlyActualAssignmentsInThisProject() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("yes", "no").items(10)
                .annotationsPerItem(3)
                .assign("alice", "yes", "yes", "yes")
                .assign("bob", "no", "no", "no", "no", "no", "no", "no", "no", "no", "no")
                .annotator("carol")
                .seed(workspace);
        long secondSplit = addSplit(flow.projectId(), "Batch 2", 2, 2,
                flow.annotatorId("alice"), flow.annotatorId("carol"));
        ClassificationWorkflow.single("yes", "no").assign("alice", "yes").seed(workspace);

        ProjectProgress progress = service().progress(flow.projectId());

        assertEquals(flow.projectId(), progress.projectId());
        assertEquals(TaxonomyKind.SINGLE, progress.kind());
        assertEquals(12, progress.itemCount());
        assertEquals(13, progress.submitted());
        assertEquals(24, progress.total());
        assertEquals(12, progress.unresolvedCount());
        assertEquals(0, progress.unresolvedDisputeCount());
        assertEquals(2, progress.splits().size());

        ProjectProgress.SplitProgress first = progress.splits().getFirst();
        assertEquals(flow.splitId(), first.summary().id());
        assertEquals(10, first.summary().itemIds().size());
        assertEquals(Integer.valueOf(3), first.summary().annotationsPerItem());
        assertEquals(2, first.summary().assignmentCount());
        assertEquals(Integer.valueOf(1), first.summary().vacantPlaces());
        assertEquals(13, first.submitted());
        assertEquals(20, first.total());
        assertEquals(AssignmentStatus.IN_PROGRESS, assignment(first, flow.annotatorId("alice")).status());
        assertEquals(3, assignment(first, flow.annotatorId("alice")).submitted());
        assertEquals(10, assignment(first, flow.annotatorId("alice")).total());
        assertEquals(AssignmentStatus.SUBMITTED, assignment(first, flow.annotatorId("bob")).status());
        assertEquals(10, assignment(first, flow.annotatorId("bob")).submitted());

        ProjectProgress.SplitProgress second = progress.splits().get(1);
        assertEquals(secondSplit, second.summary().id());
        assertEquals(2, second.summary().itemIds().size());
        assertEquals(2, second.summary().assignmentCount());
        assertEquals(Integer.valueOf(0), second.summary().vacantPlaces());
        assertEquals(0, second.submitted());
        assertEquals(4, second.total());
        assertEquals(AssignmentStatus.NOT_STARTED, assignment(second, flow.annotatorId("alice")).status());
        assertEquals(AssignmentStatus.NOT_STARTED, assignment(second, flow.annotatorId("carol")).status());

        assertEquals(3, progress.annotators().size());
        assertEquals(3, annotator(progress, flow.annotatorId("alice")).submitted());
        assertEquals(12, annotator(progress, flow.annotatorId("alice")).total());
        assertEquals(10, annotator(progress, flow.annotatorId("bob")).submitted());
        assertEquals(10, annotator(progress, flow.annotatorId("bob")).total());
        assertEquals(0, annotator(progress, flow.annotatorId("carol")).submitted());
        assertEquals(2, annotator(progress, flow.annotatorId("carol")).total());
    }

    @Test
    void progress_submissionsAndDeactivation_refreshesCountsAndKeepsEarlierSnapshot() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("yes", "no").items(2)
                .assign("alice").seed(workspace);
        ProjectService owner = service();
        ProjectProgress before = owner.progress(flow.projectId());
        assertEquals(0, before.submitted());
        assertEquals(2, before.total());

        AnnotationService annotator = new AnnotationService(workspace.store(), workspace.signIn("alice"),
                workspace.paths());
        annotator.submit(flow.assignmentId("alice"), flow.itemId(0), new Answer.LabelChoice(flow.labelId("yes")));
        ProjectProgress afterOne = owner.progress(flow.projectId());
        assertEquals(1, afterOne.submitted());
        assertEquals(1, afterOne.unresolvedCount());
        assertEquals(AssignmentStatus.IN_PROGRESS,
                assignment(afterOne.splits().getFirst(), flow.annotatorId("alice")).status());

        annotator.submit(flow.assignmentId("alice"), flow.itemId(1), new Answer.LabelChoice(flow.labelId("no")));
        AuthService ownerAuth = workspace.signInOwner();
        ownerAuth.deactivateAnnotator(flow.annotatorId("alice"));
        byte[] afterWrites = workspace.dataFileBytes();
        ProjectProgress afterTwo = owner.progress(flow.projectId());

        assertEquals(0, before.submitted());
        assertEquals(AccountStatus.ACTIVE, annotator(before, flow.annotatorId("alice")).accountStatus());
        assertEquals(2, afterTwo.submitted());
        assertEquals(2, afterTwo.total());
        assertEquals(0, afterTwo.unresolvedCount());
        assertEquals(AccountStatus.DISABLED, annotator(afterTwo, flow.annotatorId("alice")).accountStatus());
        assertEquals(AssignmentStatus.SUBMITTED,
                assignment(afterTwo.splits().getFirst(), flow.annotatorId("alice")).status());
        assertEquals(AccountStatus.DISABLED,
                assignment(afterTwo.splits().getFirst(), flow.annotatorId("alice")).accountStatus());
        assertArrayEquals(afterWrites, workspace.dataFileBytes());
    }

    @Test
    void progress_singleDisputes_countsOnlyCompleteUndecidedTies() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("yes", "no", "maybe").items(5)
                .assign("alice", "yes", "yes", "yes", "yes")
                .assign("bob", "yes", "yes", "no", "no")
                .assign("carol", "no", "no", "maybe", "maybe")
                .majority(0, "yes")
                .adjudicated(3, "maybe")
                .seed(workspace);
        ResolvedSource extra = workspace.writeSource("outside/item.txt", "Not in a split");
        workspace.store().write(session -> session.items().save(Records.item(flow.projectId(), extra)));

        ProjectProgress progress = service().progress(flow.projectId());

        assertEquals(6, progress.itemCount());
        assertEquals(12, progress.submitted());
        assertEquals(15, progress.total());
        assertEquals(4, progress.unresolvedCount());
        assertEquals(1, progress.unresolvedDisputeCount());
        assertEquals(progress.unresolvedCount(), service().list().stream()
                .filter(summary -> summary.id() == flow.projectId()).findFirst().orElseThrow().unresolvedCount());
        ResolutionService resolutions = new ResolutionService(workspace.store(), workspace.signInOwner(),
                workspace.paths());
        assertEquals(2, resolutions.disputes(flow.projectId()).size());

        resolutions.adjudicate(flow.itemId(2), flow.labelId("yes"));
        ProjectProgress afterDecision = service().progress(flow.projectId());
        assertEquals(12, afterDecision.submitted());
        assertEquals(15, afterDecision.total());
        assertEquals(3, afterDecision.unresolvedCount());
        assertEquals(0, afterDecision.unresolvedDisputeCount());
        assertEquals(1, progress.unresolvedDisputeCount());
    }

    @Test
    void progress_scaleProject_hasNoClassificationDisputes() {
        ClassificationWorkflow flow = ClassificationWorkflow.scale(1, 5).items(2)
                .assign("alice", 1, 4)
                .assign("bob", 3)
                .mean(0, 2.0)
                .seed(workspace);

        ProjectProgress progress = service().progress(flow.projectId());

        assertEquals(TaxonomyKind.SCALE, progress.kind());
        assertEquals(3, progress.submitted());
        assertEquals(4, progress.total());
        assertEquals(1, progress.unresolvedCount());
        assertEquals(0, progress.unresolvedDisputeCount());
    }

    @Test
    void progress_unknownProject_refusedWithoutChangingStore() {
        long projectId = workspace.newProject(TaxonomyKind.SINGLE);
        byte[] before = workspace.dataFileBytes();

        assertThrows(ProjectException.class, () -> service().progress(projectId + 1_000));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void progress_signedOutOrAnnotator_refusedWithoutChangingStore() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("yes", "no")
                .annotator("alice").seed(workspace);
        AuthService signedOut = workspace.signInOwner();
        ProjectService formerOwner = new ProjectService(workspace.store(), signedOut);
        formerOwner.progress(flow.projectId());
        signedOut.logout();
        ProjectService alice = new ProjectService(workspace.store(), workspace.signIn("alice"));
        byte[] before = workspace.dataFileBytes();

        assertThrows(AuthException.class, () -> formerOwner.progress(flow.projectId()));
        assertThrows(AuthException.class, () -> alice.progress(flow.projectId()));
        assertThrows(AuthException.class, () -> alice.progress(flow.projectId() + 1_000));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    private ProjectService service() {
        return new ProjectService(workspace.store(), workspace.signInOwner());
    }

    private static ProjectProgress.AssignmentProgressRow assignment(ProjectProgress.SplitProgress split,
            long accountId) {
        return split.assignments().stream().filter(row -> row.annotatorId() == accountId).findFirst().orElseThrow();
    }

    private static ProjectProgress.AnnotatorProgress annotator(ProjectProgress progress, long accountId) {
        return progress.annotators().stream().filter(row -> row.annotatorId() == accountId).findFirst().orElseThrow();
    }

    private long addSplit(long projectId, String name, int itemCount, int k, long... annotatorIds) {
        List<ResolvedSource> sources = new ArrayList<>();
        for (int index = 1; index <= itemCount; index++) {
            sources.add(workspace.writeSource("second/item-" + index + ".txt", "Second split item " + index));
        }
        return workspace.store().write(session -> {
            Split split = Records.split(projectId, name);
            split.setAnnotationsPerItem(k);
            split.setRequestedBatchSize(itemCount);
            long splitId = session.splits().save(split).getId();
            for (int index = 0; index < sources.size(); index++) {
                long itemId = session.items().save(Records.item(projectId, sources.get(index))).getId();
                session.splitItems().save(Records.membership(splitId, itemId, index + 1));
            }
            for (long annotatorId : annotatorIds) {
                Assignment assignment = Records.assignment(splitId, annotatorId);
                assignment.setStatus(AssignmentStatus.NOT_STARTED);
                session.assignments().save(assignment);
            }
            return splitId;
        });
    }
}
