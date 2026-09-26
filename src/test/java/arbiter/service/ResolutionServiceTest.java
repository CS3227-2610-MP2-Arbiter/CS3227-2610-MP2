package arbiter.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.data.json.JsonStore;
import arbiter.model.annotation.Annotation;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.resolution.Resolution;
import arbiter.model.resolution.ResolutionMethod;
import arbiter.testing.ClassificationWorkflow;
import arbiter.testing.Records;
import arbiter.testing.TestWorkspace;

/** Unit checks of automatic resolution (#27), and integration checks of it inside submission (#17). */
class ResolutionServiceTest {
    private static final long ITEM = 7;
    private static final long POSITIVE = 1;
    private static final long NEGATIVE = 2;
    private static final long NEUTRAL = 3;

    @TempDir
    Path temporary;

    private TestWorkspace workspace;

    @BeforeEach
    void createWorkspace() {
        workspace = TestWorkspace.create(temporary.resolve("workspace"));
    }

    @Test
    void decide_singleStrictMajority_majorityToThatLabel() {
        assertMajority(POSITIVE, decide(TaxonomyKind.SINGLE, 1, labels(POSITIVE)));
        assertMajority(POSITIVE, decide(TaxonomyKind.SINGLE, 3, labels(NEGATIVE, POSITIVE, POSITIVE)));
        assertMajority(NEGATIVE, decide(TaxonomyKind.SINGLE, 4, labels(POSITIVE, NEGATIVE, NEGATIVE, NEGATIVE)));
    }

    @Test
    void decide_singleWithoutStrictMajority_empty() {
        assertEquals(Optional.empty(), decide(TaxonomyKind.SINGLE, 2, labels(POSITIVE, NEGATIVE)));
        assertEquals(Optional.empty(), decide(TaxonomyKind.SINGLE, 3, labels(POSITIVE, NEGATIVE, NEUTRAL)));
        assertEquals(Optional.empty(), decide(TaxonomyKind.SINGLE, 4, labels(POSITIVE, NEGATIVE, POSITIVE,
                NEGATIVE)));
        // A plurality that is not a strict majority.
        assertEquals(Optional.empty(), decide(TaxonomyKind.SINGLE, 4, labels(POSITIVE, POSITIVE, NEGATIVE,
                NEUTRAL)));
    }

    @Test
    void decide_notExactlyKAnswers_empty() {
        // Each set is unanimous, so it would resolve if it numbered k.
        assertEquals(Optional.empty(), decide(TaxonomyKind.SINGLE, 3, labels(POSITIVE, POSITIVE)));
        assertEquals(Optional.empty(), decide(TaxonomyKind.SINGLE, 2, labels(POSITIVE, POSITIVE, POSITIVE)));
        assertEquals(Optional.empty(), decide(TaxonomyKind.SCALE, 2, ratings(3)));
    }

    @Test
    void decide_scaleRatings_autoScaleToUnroundedMean() {
        assertMean(2.5, decide(TaxonomyKind.SCALE, 2, ratings(2, 3)));
        assertMean(3.0, decide(TaxonomyKind.SCALE, 2, ratings(1, 5)));
        assertMean(-2.5, decide(TaxonomyKind.SCALE, 2, ratings(-3, -2)));
        assertMean(4.0 / 3, decide(TaxonomyKind.SCALE, 3, ratings(1, 1, 2)));
    }

    @Test
    void decide_earlierAnswerStampedAfterTheKth_itemResolvedByNoOneWhenTheKthWasSubmitted() {
        List<Annotation> answers = labels(POSITIVE, POSITIVE, POSITIVE);
        // The first annotator's clock ran ahead of the kth annotator's.
        answers.get(0).setSubmittedAt(Records.NOW.plusSeconds(300));
        Instant kth = Records.NOW.plusSeconds(60);
        answers.get(2).setSubmittedAt(kth);

        Resolution resolution = ResolutionService.decide(TaxonomyKind.SINGLE, 3, answers, kth).orElseThrow();

        assertEquals(ITEM, resolution.getItemId());
        assertNull(resolution.getDecidedByUserId());
        assertEquals(kth, resolution.getDecidedAt());
    }

    @Test
    void resolveAutomatically_earlierAnswerStampedAfterTheKth_decidedWhenTheKthWasSubmitted() {
        // Alice's answer is stamped Records.NOW, and Bob's clock runs a minute behind hers.
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob").seed(workspace);
        Annotation kth = Records.answer(flow.itemId(0), flow.assignmentId("bob"), flow.annotatorId("bob"),
                flow.labelId("positive"));
        kth.setSubmittedAt(Records.NOW.minusSeconds(60));

        workspace.store().write(session -> {
            session.annotations().insert(kth);
            ResolutionService.resolveAutomatically(session, kth);
            return null;
        });

        assertEquals(Records.NOW.minusSeconds(60), resolutionOf(flow.itemId(0)).orElseThrow().getDecidedAt());
    }

    @Test
    void resolveAutomatically_calledAgain_resolutionUnchanged() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob").seed(workspace);
        submit(flow, "bob", new Answer.LabelChoice(flow.labelId("positive")));
        byte[] resolved = workspace.dataFileBytes();

        workspace.store().write(session -> {
            ResolutionService.resolveAutomatically(session, session.annotations()
                    .findByItemAndAnnotator(flow.itemId(0), flow.annotatorId("bob")).orElseThrow());
            return null;
        });

        assertArrayEquals(resolved, workspace.dataFileBytes());
    }

    @Test
    void submit_kthAnswerWithStrictMajority_majorityStoredBeforeAssignmentsFinish() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(2)
                .assign("alice", "positive").assign("bob", "negative").assign("carol").seed(workspace);

        submit(flow, "carol", new Answer.LabelChoice(flow.labelId("positive")));

        assertMajority(flow.labelId("positive"), resolutionOf(flow.itemId(0)));
        for (String annotator : List.of("alice", "bob", "carol")) {
            assertEquals(AssignmentStatus.IN_PROGRESS, workspace.store().read(session -> session.assignments()
                    .findById(flow.assignmentId(annotator)).orElseThrow()).getStatus());
        }
    }

    @Test
    void submit_kthAnswerWithoutStrictMajority_nothingStored() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob").seed(workspace);

        submit(flow, "bob", new Answer.LabelChoice(flow.labelId("negative")));

        assertEquals(Optional.empty(), resolutionOf(flow.itemId(0)));
    }

    @Test
    void submit_fewerThanKAnswers_nothingStored() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob").assign("carol").seed(workspace);

        submit(flow, "bob", new Answer.LabelChoice(flow.labelId("positive")));

        assertEquals(Optional.empty(), resolutionOf(flow.itemId(0)));
    }

    @Test
    void submit_kthRating_meanPersistsWithItsContributorsAfterRestart() {
        ClassificationWorkflow flow = ClassificationWorkflow.scale(1, 5).assign("alice", 2).assign("bob")
                .seed(workspace);
        long itemId = flow.itemId(0);
        long bobId = flow.annotatorId("bob");
        submit(flow, "bob", new Answer.Rating(3));

        JsonStore reopened = JsonStore.open(workspace.paths());
        Resolution stored = reopened.read(session -> session.resolutions().findByItem(itemId)).orElseThrow();
        List<Annotation> contributors = reopened.read(session -> session.annotations().listByItem(itemId));

        assertMean(2.5, Optional.of(stored));
        assertNull(stored.getDecidedByUserId());
        assertEquals(contributors.stream().filter(answer -> answer.getAnnotatorId() == bobId).findFirst()
                .orElseThrow().getSubmittedAt(), stored.getDecidedAt());
        assertEquals(Map.of(flow.annotatorId("alice"), 2, bobId, 3), contributors.stream()
                .collect(Collectors.toMap(Annotation::getAnnotatorId, Annotation::getScaleValue)));
    }

    @Test
    void submit_itemAlreadyResolved_existingResolutionKept() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob").seed(workspace);
        long itemId = flow.itemId(0);
        // Not the resolution the answers give, so an overwrite would show.
        workspace.store().write(session -> session.resolutions().save(Records.resolution(itemId,
                flow.labelId("negative"))));

        submit(flow, "bob", new Answer.LabelChoice(flow.labelId("positive")));

        List<Resolution> stored = workspace.store().read(session -> session.resolutions()
                .listByItems(List.of(itemId)));
        assertEquals(1, stored.size());
        assertEquals(flow.labelId("negative"), stored.getFirst().getLabelId());
        assertEquals(Records.NOW, stored.getFirst().getDecidedAt());
    }

    private void submit(ClassificationWorkflow flow, String username, Answer answer) {
        new AnnotationService(workspace.store(), workspace.signIn(username), workspace.paths())
                .submit(flow.assignmentId(username), flow.itemId(0), answer);
    }

    /** Returns the item's resolution as Arbiter reads it after a restart. */
    private Optional<Resolution> resolutionOf(long itemId) {
        return JsonStore.open(workspace.paths()).read(session -> session.resolutions().findByItem(itemId));
    }

    /** Decides these answers as {@link ResolutionService#decide} does, with the kth submitted at Records.NOW. */
    private static Optional<Resolution> decide(TaxonomyKind kind, int k, List<Annotation> answers) {
        return ResolutionService.decide(kind, k, answers, Records.NOW);
    }

    private static void assertMajority(long labelId, Optional<Resolution> decided) {
        Resolution resolution = decided.orElseThrow();
        assertEquals(ResolutionMethod.MAJORITY, resolution.getMethod());
        assertEquals(labelId, resolution.getLabelId());
    }

    private static void assertMean(double mean, Optional<Resolution> decided) {
        Resolution resolution = decided.orElseThrow();
        assertEquals(ResolutionMethod.AUTO_SCALE, resolution.getMethod());
        assertEquals(mean, resolution.getScaleValue());
    }

    /** Returns one answer per annotator for {@link #ITEM}, choosing these labels. */
    private static List<Annotation> labels(long... labelIds) {
        List<Annotation> answers = new ArrayList<>();
        for (int index = 0; index < labelIds.length; index++) {
            answers.add(Records.answer(ITEM, index + 1, index + 1, labelIds[index]));
        }
        return answers;
    }

    /** Returns one answer per annotator for {@link #ITEM}, giving these ratings. */
    private static List<Annotation> ratings(int... ratings) {
        List<Annotation> answers = new ArrayList<>();
        for (int index = 0; index < ratings.length; index++) {
            answers.add(Records.scaleAnswer(ITEM, index + 1, index + 1, ratings[index]));
        }
        return answers;
    }
}
