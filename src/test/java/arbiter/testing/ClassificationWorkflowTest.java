package arbiter.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.data.json.RepositorySession;
import arbiter.model.annotation.Annotation;
import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.Item;
import arbiter.model.project.Label;
import arbiter.model.project.Split;
import arbiter.model.project.SplitItem;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.project.TaxonomySettings;
import arbiter.model.resolution.Resolution;
import arbiter.model.resolution.ResolutionMethod;
import arbiter.model.user.AccountStatus;
import arbiter.model.user.Role;
import arbiter.model.user.User;
import arbiter.service.AuthException;
import arbiter.workspace.SourceResolver;

/** Checks that the classification fixture seeds exactly the described, reachable state. */
class ClassificationWorkflowTest {
    @TempDir
    Path temporary;

    private TestWorkspace workspace;

    @BeforeEach
    void createWorkspace() {
        workspace = TestWorkspace.create(temporary.resolve("workspace"));
    }

    @Test
    void seed_singleWithDefaults_oneUnassignedItemAndOrderedLabels() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").seed(workspace);

        TaxonomySettings settings = read(session -> session.taxonomySettings().findByProject(flow.projectId()))
                .orElseThrow();
        List<Label> labels = read(session -> session.labels().listByProject(flow.projectId()));
        Split split = read(session -> session.splits().findById(flow.splitId())).orElseThrow();
        List<SplitItem> members = read(session -> session.splitItems().listBySplit(flow.splitId()));
        assertEquals(TaxonomyKind.SINGLE, settings.getKind());
        assertEquals(List.of("positive", "negative"), labels.stream().map(Label::getKey).toList());
        assertEquals(flow.labelId("negative"), labels.get(1).getId());
        assertEquals(1, flow.itemIds().size());
        assertEquals(flow.itemIds(), members.stream().map(SplitItem::getItemId).toList());
        assertNull(split.getAnnotationsPerItem());
        assertEquals(List.of(), read(session -> session.assignments().listBySplit(flow.splitId())));
    }

    @Test
    void seed_scale_rangeStoredWithoutLabels() {
        ClassificationWorkflow flow = ClassificationWorkflow.scale(1, 5).seed(workspace);

        TaxonomySettings settings = read(session -> session.taxonomySettings().findByProject(flow.projectId()))
                .orElseThrow();
        assertEquals(TaxonomyKind.SCALE, settings.getKind());
        assertEquals(1, settings.getScaleMin());
        assertEquals(5, settings.getScaleMax());
        assertEquals(List.of(), read(session -> session.labels().listByProject(flow.projectId())));
    }

    @Test
    void start_emptyOrRepeatedLabelsOrInvertedScale_exceptionThrown() {
        assertThrows(IllegalArgumentException.class, () -> ClassificationWorkflow.single());
        assertThrows(IllegalArgumentException.class, () -> ClassificationWorkflow.single("positive", "positive"));
        assertThrows(IllegalArgumentException.class, () -> ClassificationWorkflow.scale(4, 3));
    }

    @Test
    void start_singleValueScaleAndOneOfEach_seeded() {
        ClassificationWorkflow flow = ClassificationWorkflow.scale(3, 3).items(1).annotationsPerItem(1)
                .assign("alice", 3).seed(workspace);

        assertEquals(3, answerOf(flow.itemId(0), flow.annotatorId("alice")).orElseThrow().getScaleValue());
    }

    @Test
    void counts_zero_exceptionThrown() {
        ClassificationWorkflow.Builder builder = ClassificationWorkflow.single("positive");

        assertThrows(IllegalArgumentException.class, () -> builder.items(0));
        assertThrows(IllegalArgumentException.class, () -> builder.annotationsPerItem(0));
    }

    @Test
    void seed_items_registeredInSplitOrderAndReadable() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").items(3).seed(workspace);

        List<SplitItem> members = read(session -> session.splitItems().listBySplit(flow.splitId()));
        assertEquals(flow.itemIds(), members.stream().map(SplitItem::getItemId).toList());
        SourceResolver resolver = new SourceResolver(workspace.paths());
        for (long itemId : flow.itemIds()) {
            Item item = read(session -> session.items().findById(itemId)).orElseThrow();
            assertTrue(resolver.resolve(item.getPath(), item.getContentHash()).text().startsWith("Synthetic item"));
        }
    }

    @Test
    void seed_assignedWithoutK_kIsAssigneeCount() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").assign("alice").assign("bob")
                .seed(workspace);

        Split split = read(session -> session.splits().findById(flow.splitId())).orElseThrow();
        assertEquals(2, split.getAnnotationsPerItem());
    }

    @Test
    void seed_kAboveAssignees_unfilledPlacesKept() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").annotationsPerItem(3)
                .assign("alice").assign("bob").seed(workspace);

        assertEquals(3, read(session -> session.splits().findById(flow.splitId())).orElseThrow()
                .getAnnotationsPerItem());
        assertEquals(2, read(session -> session.assignments().listBySplit(flow.splitId())).size());
    }

    @Test
    void seed_assigneesAboveK_exceptionThrownAndNothingWritten() {
        ClassificationWorkflow.Builder builder = ClassificationWorkflow.single("positive").annotationsPerItem(1)
                .assign("alice").assign("bob");

        assertThrows(IllegalArgumentException.class, () -> builder.seed(workspace));
        assertUntouched();
    }

    @Test
    void seed_answers_submittedForFirstItemsInSplitOrder() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(3)
                .assign("alice", "negative", "positive").seed(workspace);

        long alice = flow.annotatorId("alice");
        assertEquals(flow.labelId("negative"), answerOf(flow.itemId(0), alice).orElseThrow().getLabelId());
        assertEquals(flow.labelId("positive"), answerOf(flow.itemId(1), alice).orElseThrow().getLabelId());
        assertTrue(answerOf(flow.itemId(2), alice).isEmpty());
        assertEquals(flow.assignmentId("alice"), answerOf(flow.itemId(0), alice).orElseThrow().getAssignmentId());
    }

    @Test
    void seed_answerCount_setsAssignmentStatus() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").items(2)
                .assign("none").assign("some", "positive").assign("all", "positive", "positive").seed(workspace);

        assertEquals(AssignmentStatus.NOT_STARTED, statusOf(flow, "none"));
        assertEquals(AssignmentStatus.IN_PROGRESS, statusOf(flow, "some"));
        assertEquals(AssignmentStatus.SUBMITTED, statusOf(flow, "all"));
    }

    @Test
    void seed_moreAnswersThanItems_exceptionThrownAndNothingWritten() {
        ClassificationWorkflow.Builder builder = ClassificationWorkflow.single("positive").items(1)
                .assign("alice", "positive", "positive");

        assertThrows(IllegalArgumentException.class, () -> builder.seed(workspace));
        assertUntouched();
    }

    @Test
    void assign_unknownLabel_exceptionThrown() {
        ClassificationWorkflow.Builder builder = ClassificationWorkflow.single("positive");

        assertThrows(IllegalArgumentException.class, () -> builder.assign("alice", "neutral"));
    }

    @Test
    void assign_ratingsAtRangeEnds_stored() {
        ClassificationWorkflow flow = ClassificationWorkflow.scale(1, 5).items(2).assign("alice", 1, 5)
                .seed(workspace);

        long alice = flow.annotatorId("alice");
        assertEquals(1, answerOf(flow.itemId(0), alice).orElseThrow().getScaleValue());
        assertEquals(5, answerOf(flow.itemId(1), alice).orElseThrow().getScaleValue());
    }

    @Test
    void assign_ratingJustOutsideRange_exceptionThrown() {
        ClassificationWorkflow.Builder builder = ClassificationWorkflow.scale(1, 5);

        assertThrows(IllegalArgumentException.class, () -> builder.assign("alice", 0));
        assertThrows(IllegalArgumentException.class, () -> builder.assign("alice", 6));
    }

    @Test
    void assign_answerOfOtherKind_exceptionThrown() {
        ClassificationWorkflow.Builder single = ClassificationWorkflow.single("positive");
        ClassificationWorkflow.Builder scale = ClassificationWorkflow.scale(1, 5);

        assertThrows(IllegalArgumentException.class, () -> single.assign("alice", 1));
        assertThrows(IllegalArgumentException.class, () -> scale.assign("alice", "positive"));
    }

    @Test
    void assign_nameAlreadyUsed_exceptionThrown() {
        ClassificationWorkflow.Builder builder = ClassificationWorkflow.single("positive").assign("alice");

        assertThrows(IllegalArgumentException.class, () -> builder.assign("alice"));
        assertThrows(IllegalArgumentException.class, () -> builder.annotator("ALICE"));
        assertThrows(IllegalArgumentException.class, () -> builder.assign(TestWorkspace.OWNER));
    }

    @Test
    void seed_disabledAssignee_keepsAssignmentAndAnswersButCannotSignIn() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").assign("alice", "positive")
                .disabled("alice").seed(workspace);

        User alice = read(session -> session.users().findById(flow.annotatorId("alice"))).orElseThrow();
        assertEquals(AccountStatus.DISABLED, alice.getAccountStatus());
        assertTrue(read(session -> session.assignments().findById(flow.assignmentId("alice"))).isPresent());
        assertTrue(answerOf(flow.itemId(0), alice.getId()).isPresent());
        assertThrows(AuthException.class, () -> workspace.signIn("alice"));
    }

    @Test
    void seed_unassignedAnnotator_activeAccountWithoutAssignment() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive").annotator("dave").seed(workspace);

        User dave = read(session -> session.users().findById(flow.annotatorId("dave"))).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, dave.getAccountStatus());
        assertEquals(List.of(), read(session -> session.assignments().listByAnnotator(dave.getId())));
        assertThrows(IllegalArgumentException.class, () -> flow.assignmentId("dave"));
    }

    @Test
    void seed_disabledNameNotInWorkflow_exceptionThrown() {
        ClassificationWorkflow.Builder builder = ClassificationWorkflow.single("positive").disabled("alice");

        assertThrows(IllegalArgumentException.class, () -> builder.seed(workspace));
        assertUntouched();
    }

    @Test
    void seed_singleResolutions_recordedWithMethodAndDecider() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(2)
                .assign("alice", "positive", "positive").assign("bob", "positive", "negative")
                .majority(0, "positive").adjudicated(1, "negative").seed(workspace);

        Resolution majority = resolutionOf(flow.itemId(0));
        Resolution adjudicated = resolutionOf(flow.itemId(1));
        assertEquals(ResolutionMethod.MAJORITY, majority.getMethod());
        assertEquals(flow.labelId("positive"), majority.getLabelId());
        assertNull(majority.getDecidedByUserId());
        assertEquals(ResolutionMethod.ADJUDICATED, adjudicated.getMethod());
        assertEquals(flow.labelId("negative"), adjudicated.getLabelId());
        assertEquals(flow.ownerId(), adjudicated.getDecidedByUserId());
    }

    @Test
    void seed_scaleResolution_recordedAsMean() {
        ClassificationWorkflow flow = ClassificationWorkflow.scale(1, 5).assign("alice", 2).assign("bob", 3)
                .mean(0, 2.5).seed(workspace);

        Resolution resolution = resolutionOf(flow.itemId(0));
        assertEquals(ResolutionMethod.AUTO_SCALE, resolution.getMethod());
        assertEquals(2.5, resolution.getScaleValue());
    }

    @Test
    void seed_majorityJustAboveHalf_recorded() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob", "positive").assign("carol", "negative")
                .majority(0, "positive").seed(workspace);

        assertEquals(flow.labelId("positive"), resolutionOf(flow.itemId(0)).getLabelId());
    }

    @Test
    void seed_majorityOnTie_exceptionThrownAndNothingWritten() {
        ClassificationWorkflow.Builder builder = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob", "negative").majority(0, "positive");

        assertThrows(IllegalArgumentException.class, () -> builder.seed(workspace));
        assertUntouched();
    }

    @Test
    void seed_majorityForMinorityLabel_exceptionThrown() {
        ClassificationWorkflow.Builder builder = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob", "positive").assign("carol", "negative")
                .majority(0, "negative");

        assertThrows(IllegalArgumentException.class, () -> builder.seed(workspace));
    }

    @Test
    void seed_adjudicatedWithStrictMajority_exceptionThrown() {
        ClassificationWorkflow.Builder builder = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob", "positive").adjudicated(0, "negative");

        assertThrows(IllegalArgumentException.class, () -> builder.seed(workspace));
    }

    @Test
    void seed_meanOfRatings_mustBeExact() {
        ClassificationWorkflow.Builder wrong = ClassificationWorkflow.scale(1, 5).assign("alice", 2)
                .assign("bob", 3).mean(0, 2.4);
        ClassificationWorkflow repeating = ClassificationWorkflow.scale(1, 5).assign("alice", 1)
                .assign("bob", 2).assign("carol", 2).mean(0, 5.0 / 3).seed(workspace);

        assertThrows(IllegalArgumentException.class, () -> wrong.seed(workspace));
        assertEquals(5.0 / 3, resolutionOf(repeating.itemId(0)).getScaleValue());
    }

    @Test
    void seed_resolutionWithFewerThanKAnswers_exceptionThrownAndNothingWritten() {
        ClassificationWorkflow.Builder builder = ClassificationWorkflow.single("positive").annotationsPerItem(2)
                .assign("alice", "positive").assign("bob").majority(0, "positive");

        assertThrows(IllegalArgumentException.class, () -> builder.seed(workspace));
        assertUntouched();
    }

    @Test
    void seed_resolutionOnUnassignedSplit_exceptionThrown() {
        ClassificationWorkflow.Builder builder = ClassificationWorkflow.single("positive").majority(0, "positive");

        assertThrows(IllegalArgumentException.class, () -> builder.seed(workspace));
    }

    @Test
    void seed_resolutionOutsideItems_exceptionThrown() {
        assertThrows(IllegalArgumentException.class, () -> ClassificationWorkflow.single("positive").items(2)
                .assign("alice", "positive", "positive").majority(2, "positive").seed(workspace));
        assertThrows(IllegalArgumentException.class, () -> ClassificationWorkflow.single("positive")
                .assign("alice", "positive").majority(-1, "positive").seed(workspace));
    }

    @Test
    void resolve_otherKindOrSecondDecision_exceptionThrown() {
        ClassificationWorkflow.Builder single = ClassificationWorkflow.single("positive");
        ClassificationWorkflow.Builder scale = ClassificationWorkflow.scale(1, 5);
        ClassificationWorkflow.Builder decided = ClassificationWorkflow.single("positive").majority(0, "positive");

        assertThrows(IllegalArgumentException.class, () -> single.mean(0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> scale.majority(0, "x"));
        assertThrows(IllegalArgumentException.class, () -> decided.adjudicated(0, "positive"));
    }

    @Test
    void seed_twoWorkflowsInOneWorkspace_accountsReusedAndEarlierSourcesKept() {
        ClassificationWorkflow first = ClassificationWorkflow.single("positive").assign("alice").seed(workspace);
        ClassificationWorkflow second = ClassificationWorkflow.scale(1, 5).assign("alice").seed(workspace);

        assertEquals(first.annotatorId("alice"), second.annotatorId("alice"));
        assertEquals(first.ownerId(), second.ownerId());
        Item earlier = read(session -> session.items().findById(first.itemId(0))).orElseThrow();
        new SourceResolver(workspace.paths()).resolve(earlier.getPath(), earlier.getContentHash());
    }

    @Test
    void seed_existingAdjudicatorName_exceptionThrown() {
        ClassificationWorkflow.single("positive").seed(workspace);
        workspace.store().write(session -> session.users().save(Records.user("carol",
                Role.ADJUDICATOR)));

        ClassificationWorkflow.Builder builder = ClassificationWorkflow.single("positive").assign("carol");

        assertThrows(IllegalArgumentException.class, () -> builder.seed(workspace));
    }

    private void assertUntouched() {
        boolean pristine = read(RepositorySession::isPristine);
        assertTrue(pristine);
        try (Stream<Path> media = Files.list(workspace.paths().mediaDirectory())) {
            assertEquals(0, media.count());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private AssignmentStatus statusOf(ClassificationWorkflow flow, String annotator) {
        Assignment assignment = read(session -> session.assignments().findById(flow.assignmentId(annotator)))
                .orElseThrow();
        return assignment.getStatus();
    }

    private Optional<Annotation> answerOf(long itemId, long annotatorId) {
        return read(session -> session.annotations().findByItemAndAnnotator(itemId, annotatorId));
    }

    private Resolution resolutionOf(long itemId) {
        return read(session -> session.resolutions().findByItem(itemId)).orElseThrow();
    }

    private <T> T read(Function<RepositorySession, T> action) {
        return workspace.store().read(action);
    }
}
