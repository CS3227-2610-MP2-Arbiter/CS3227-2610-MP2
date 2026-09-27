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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import arbiter.data.json.JsonStore;
import arbiter.model.annotation.Annotation;
import arbiter.model.project.Item;
import arbiter.model.project.Label;
import arbiter.model.project.Split;
import arbiter.model.project.SplitItem;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.resolution.Resolution;
import arbiter.model.resolution.ResolutionMethod;
import arbiter.testing.ClassificationWorkflow;
import arbiter.testing.Records;
import arbiter.testing.TestWorkspace;
import arbiter.workspace.ResolvedSource;
import arbiter.workspace.SourceException;
import arbiter.workspace.SourceResolver;

/** Integration checks of the adjudicator's dispute list, comparison and manual resolution (#34). */
class ResolutionServiceDisputeTest {
    @TempDir
    Path temporary;

    private TestWorkspace workspace;

    @BeforeEach
    void createWorkspace() {
        workspace = TestWorkspace.create(temporary.resolve("workspace"));
    }

    @Test
    void disputes_mixedItems_onlyUndecidedDisputeThenAdjudicatedItemListed() {
        // k = 2. Item 0 is MAJORITY, 1 a tie, 2 ADJUDICATED, 3 a majority from before #27 with no resolution, and
        // 4 has one answer.
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative", "neutral").items(5)
                .assign("alice", "positive", "positive", "negative", "positive", "positive")
                .assign("bob", "positive", "negative", "positive", "positive")
                .majority(0, "positive")
                .adjudicated(2, "neutral")
                .seed(workspace);

        List<DisputeSummary> disputes = service().disputes(flow.projectId());

        assertEquals(List.of(
                new DisputeSummary(flow.itemId(1), "media/corpus-1/item-2.txt", "Batch 1", 2, null),
                new DisputeSummary(flow.itemId(2), "media/corpus-1/item-3.txt", "Batch 1", 3, "neutral")),
                disputes);
        assertFalse(disputes.get(0).decided());
        assertTrue(disputes.get(1).decided());
    }

    @Test
    void disputes_scaleOrUnknownProject_empty() {
        // Item 1 has k ratings and no resolution, as before #27.
        ClassificationWorkflow scale = ClassificationWorkflow.scale(1, 5).items(2).assign("alice", 2, 4)
                .assign("bob", 3, 4).mean(0, 2.5).seed(workspace);
        ResolutionService service = service();

        assertEquals(List.of(), service.disputes(scale.projectId()));
        assertEquals(List.of(), service.disputes(999_999L));
    }

    @Test
    void disputes_twoSplits_undecidedFirstThenBySplitAndPosition() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(3)
                .assign("alice", "positive", "positive", "negative")
                .assign("bob", "negative", "negative", "positive")
                .adjudicated(0, "negative")
                .seed(workspace);
        // Reversed, so position order differs from identifier order: items 2, 1 and 0 hold positions 1, 2 and 3.
        workspace.store().write(session -> {
            for (SplitItem member : session.splitItems().listBySplit(flow.splitId())) {
                member.setSequence(4 - member.getSequence());
                session.splitItems().save(member);
            }
            return null;
        });
        List<Long> second = addSplit(flow, List.of("negative", "positive"), List.of("positive", "negative"));
        workspace.store().write(session -> {
            Resolution decided = Records.resolution(second.get(1), flow.labelId("positive"));
            decided.setMethod(ResolutionMethod.ADJUDICATED);
            decided.setDecidedByUserId(flow.ownerId());
            return session.resolutions().save(decided);
        });

        assertEquals(List.of(
                new DisputeSummary(flow.itemId(2), "media/corpus-1/item-3.txt", "Batch 1", 1, null),
                new DisputeSummary(flow.itemId(1), "media/corpus-1/item-2.txt", "Batch 1", 2, null),
                new DisputeSummary(second.get(0), "media/second/item-1.txt", "Batch 2", 1, null),
                new DisputeSummary(flow.itemId(0), "media/corpus-1/item-1.txt", "Batch 1", 3, "negative"),
                new DisputeSummary(second.get(1), "media/second/item-2.txt", "Batch 2", 2, "positive")),
                service().disputes(flow.projectId()));
    }

    @Test
    void disputes_itemUnregisteredBeforeAssignment_positionsCountedInSplitOrder() {
        // Unregistering the middle file before the first assignment leaves stored sequences 1 and 3.
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(3)
                .annotator("alice").annotator("bob").seed(workspace);
        new CorpusService(workspace.store(), workspace.signInOwner(), workspace.paths()).unregister(flow.itemId(1));
        workspace.store().write(session -> {
            Split split = session.splits().findById(flow.splitId()).orElseThrow();
            split.setAnnotationsPerItem(2);
            session.splits().save(split);
            Map.of("alice", "positive", "bob", "negative").forEach((username, label) -> {
                long annotatorId = flow.annotatorId(username);
                long assignmentId = session.assignments().save(Records.assignment(flow.splitId(), annotatorId))
                        .getId();
                for (long itemId : List.of(flow.itemId(0), flow.itemId(2))) {
                    session.annotations().insert(Records.answer(itemId, assignmentId, annotatorId,
                            flow.labelId(label)));
                }
            });
            return null;
        });

        assertEquals(List.of(
                new DisputeSummary(flow.itemId(0), "media/corpus-1/item-1.txt", "Batch 1", 1, null),
                new DisputeSummary(flow.itemId(2), "media/corpus-1/item-3.txt", "Batch 1", 2, null)),
                service().disputes(flow.projectId()));
    }

    @Test
    void comparison_undecidedDispute_textAndAnswerKeysInTaxonomyOrder() {
        // Submitted as neutral, negative, positive, negative: a plurality with no strict majority of k = 4.
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative", "neutral")
                .assign("alice", "neutral").assign("bob", "negative").assign("carol", "positive")
                .assign("dave", "negative").seed(workspace);

        DisputeComparison comparison = service().comparison(flow.itemId(0));

        assertEquals("media/corpus-1/item-1.txt", comparison.path());
        assertEquals("Synthetic item 1 of corpus 1", comparison.text());
        assertNull(comparison.failure());
        assertTrue(comparison.readable());
        assertEquals(List.of("positive", "negative", "negative", "neutral"), comparison.answerKeys());
        assertNull(comparison.decisionKey());
        assertEquals(TaxonomyKind.SINGLE, comparison.taxonomy().kind());
        assertEquals(List.of("positive", "negative", "neutral"), keys(comparison.taxonomy()));
        assertTrue(comparison.taxonomy().frozen());
    }

    @Test
    void comparison_sourceEditedOrDeleted_resolverMessageInsteadOfText() throws IOException {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(2)
                .assign("alice", "positive", "positive").assign("bob", "negative", "negative").seed(workspace);
        Files.writeString(workspace.paths().root().resolve("media/corpus-1/item-1.txt"), "Edited after import");
        Files.delete(workspace.paths().root().resolve("media/corpus-1/item-2.txt"));
        ResolutionService service = service();

        for (long itemId : flow.itemIds()) {
            DisputeComparison comparison = service.comparison(itemId);

            assertNull(comparison.text());
            assertEquals(resolverMessage(itemId), comparison.failure());
            assertFalse(comparison.readable());
            assertEquals(List.of("positive", "negative"), comparison.answerKeys());
        }
    }

    @Test
    void disputesAndComparison_listedItems_nothingStored() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(2)
                .assign("alice", "positive", "positive").assign("bob", "negative", "negative")
                .adjudicated(1, "negative").seed(workspace);
        ResolutionService service = service();
        byte[] before = workspace.dataFileBytes();

        service.disputes(flow.projectId());
        service.comparison(flow.itemId(0));
        DisputeComparison decided = service.comparison(flow.itemId(1));

        assertEquals("negative", decided.decisionKey());
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void comparisonAndAdjudicate_itemNotListed_refusedAndNothingStored() {
        // k = 2. Item 0 is MAJORITY, 1 a majority from before #27 with no resolution, and 2 has one answer.
        ClassificationWorkflow single = ClassificationWorkflow.single("positive", "negative").items(3)
                .assign("alice", "positive", "positive", "positive").assign("bob", "positive", "positive")
                .majority(0, "positive").seed(workspace);
        ClassificationWorkflow scale = ClassificationWorkflow.scale(1, 5).assign("alice", 3).assign("bob", 4)
                .mean(0, 3.5).seed(workspace);
        // Its split has no k yet, as before its first assignment.
        ClassificationWorkflow unassigned = ClassificationWorkflow.single("positive", "negative").seed(workspace);
        ResolutionService service = service();
        long negative = single.labelId("negative");

        for (long itemId : List.of(single.itemId(0), single.itemId(1), single.itemId(2), 999_999L)) {
            assertRefused(() -> service.comparison(itemId), "compare " + itemId);
            assertRefused(() -> service.adjudicate(itemId, negative), "adjudicate " + itemId);
        }
        long scaleItem = scale.itemId(0);
        assertRefused(() -> service.comparison(scaleItem), "compare scale");
        // A SCALE project has no labels, so this label is also another project's.
        assertRefused(() -> service.adjudicate(scaleItem, negative), "adjudicate scale");
        long unassignedItem = unassigned.itemId(0);
        assertRefused(() -> service.comparison(unassignedItem), "compare unassigned");
        assertRefused(() -> service.adjudicate(unassignedItem, unassigned.labelId("negative")),
                "adjudicate unassigned");
    }

    @Test
    void adjudicate_submittedLabel_adjudicatedResolutionPersistsAndAnswersKept() {
        // k = 2. Items 0 and 2 are ties and item 1 is MAJORITY, so two items are unresolved.
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(3)
                .assign("alice", "positive", "positive", "positive").assign("bob", "negative", "positive", "negative")
                .majority(1, "positive").seed(workspace);
        long itemId = flow.itemId(0);
        List<String> answersBefore = describe(workspace.store().read(session -> session.annotations()
                .listByItem(itemId)));
        assertEquals(2, unresolved(flow.projectId()));
        Instant before = Instant.now();

        service().adjudicate(itemId, flow.labelId("positive"));

        Instant after = Instant.now();
        JsonStore reopened = JsonStore.open(workspace.paths());
        List<Resolution> stored = reopened.read(session -> session.resolutions().listByItems(List.of(itemId)));
        assertEquals(1, stored.size());
        assertAdjudicated(stored.getFirst(), flow.labelId("positive"), flow.ownerId(), before, after);
        List<Annotation> contributors = reopened.read(session -> session.annotations().listByItem(itemId));
        assertEquals(answersBefore, describe(contributors));
        assertEquals(Map.of(flow.annotatorId("alice"), flow.labelId("positive"), flow.annotatorId("bob"),
                flow.labelId("negative")), contributors.stream()
                .collect(Collectors.toMap(Annotation::getAnnotatorId, Annotation::getLabelId)));
        assertEquals(1, unresolved(flow.projectId()));
    }

    @Test
    void adjudicate_labelNoAnnotatorChose_storedAndListedAsDecided() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative", "neutral")
                .assign("alice", "positive").assign("bob", "negative").seed(workspace);
        long itemId = flow.itemId(0);
        ResolutionService service = service();

        service.adjudicate(itemId, flow.labelId("neutral"));

        Resolution stored = JsonStore.open(workspace.paths()).read(session -> session.resolutions()
                .findByItem(itemId)).orElseThrow();
        assertEquals(ResolutionMethod.ADJUDICATED, stored.getMethod());
        assertEquals(flow.labelId("neutral"), stored.getLabelId());
        assertEquals(List.of(new DisputeSummary(itemId, "media/corpus-1/item-1.txt", "Batch 1", 1, "neutral")),
                service.disputes(flow.projectId()));
        assertEquals("neutral", service.comparison(itemId).decisionKey());
    }

    @Test
    void adjudicate_alreadyAdjudicated_sameRecordReplacedAndAnswersKept() {
        // The fixture's decision was made at Records.NOW, so a new time shows.
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob", "negative").adjudicated(0, "negative").seed(workspace);
        long itemId = flow.itemId(0);
        Resolution earlier = workspace.store().read(session -> session.resolutions().findByItem(itemId))
                .orElseThrow();
        List<String> answersBefore = describe(workspace.store().read(session -> session.annotations()
                .listByItem(itemId)));
        ResolutionService service = service();
        Instant before = Instant.now();

        service.adjudicate(itemId, flow.labelId("positive"));

        Instant after = Instant.now();
        JsonStore reopened = JsonStore.open(workspace.paths());
        List<Resolution> stored = reopened.read(session -> session.resolutions().listByItems(List.of(itemId)));
        assertEquals(1, stored.size());
        assertEquals(earlier.getId(), stored.getFirst().getId());
        assertAdjudicated(stored.getFirst(), flow.labelId("positive"), flow.ownerId(), before, after);
        assertEquals(answersBefore, describe(reopened.read(session -> session.annotations().listByItem(itemId))));
        assertEquals("positive", service.disputes(flow.projectId()).getFirst().decisionKey());
    }

    @Test
    void adjudicate_labelOutsideProjectTaxonomy_refusedAndNothingStored() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob", "negative").seed(workspace);
        // Its label has the same key as one of the dispute's project, but is another project's.
        ClassificationWorkflow other = ClassificationWorkflow.single("positive", "elsewhere").seed(workspace);
        ResolutionService service = service();
        long itemId = flow.itemId(0);

        assertRefused(() -> service.adjudicate(itemId, other.labelId("positive")), "another project's label");
        assertRefused(() -> service.adjudicate(itemId, 999_999L), "unknown label");
    }

    @Test
    void adjudicate_sourceChangedAfterComparison_refusedAndNothingStored() throws IOException {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob", "negative").seed(workspace);
        ResolutionService service = service();
        long itemId = flow.itemId(0);
        assertTrue(service.comparison(itemId).readable());

        Files.writeString(workspace.paths().root().resolve("media/corpus-1/item-1.txt"), "Edited while open");

        assertRefused(() -> service.adjudicate(itemId, flow.labelId("positive")), "changed source");
    }

    @Test
    void disputeMethods_signedOutAfterUse_authExceptionAndNothingStored() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob", "negative").seed(workspace);
        AuthService auth = workspace.signInOwner();
        ResolutionService service = new ResolutionService(workspace.store(), auth, workspace.paths());
        service.disputes(flow.projectId());
        auth.logout();
        long itemId = flow.itemId(0);
        long positive = flow.labelId("positive");
        byte[] before = workspace.dataFileBytes();

        assertThrows(AuthException.class, () -> service.disputes(flow.projectId()));
        assertThrows(AuthException.class, () -> service.disputes(999_999L));
        assertThrows(AuthException.class, () -> service.comparison(itemId));
        assertThrows(AuthException.class, () -> service.comparison(999_999L));
        assertThrows(AuthException.class, () -> service.adjudicate(itemId, positive));
        assertThrows(AuthException.class, () -> service.adjudicate(999_999L, 999_999L));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void disputeMethods_annotator_authExceptionAndNothingStored() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob", "negative").seed(workspace);
        ResolutionService alice = new ResolutionService(workspace.store(), workspace.signIn("alice"),
                workspace.paths());
        long itemId = flow.itemId(0);
        long positive = flow.labelId("positive");
        byte[] before = workspace.dataFileBytes();

        assertThrows(AuthException.class, () -> alice.disputes(flow.projectId()));
        assertThrows(AuthException.class, () -> alice.comparison(itemId));
        assertThrows(AuthException.class, () -> alice.adjudicate(itemId, positive));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    /** Returns a service for the owner. */
    private ResolutionService service() {
        return new ResolutionService(workspace.store(), workspace.signInOwner(), workspace.paths());
    }

    /**
     * Adds a split named Batch 2 to the flow's project, with one new item for each of alice's labels, assigned to
     * alice and bob with k = 2, who chose these labels in split order. Returns its items in split order.
     */
    private List<Long> addSplit(ClassificationWorkflow flow, List<String> alice, List<String> bob) {
        List<ResolvedSource> sources = new ArrayList<>();
        for (int index = 1; index <= alice.size(); index++) {
            sources.add(workspace.writeSource("second/item-" + index + ".txt", "Second split item " + index));
        }
        return workspace.store().write(session -> {
            Split split = Records.split(flow.projectId(), "Batch 2");
            split.setAnnotationsPerItem(2);
            split.setRequestedBatchSize(alice.size());
            long splitId = session.splits().save(split).getId();
            long aliceId = flow.annotatorId("alice");
            long bobId = flow.annotatorId("bob");
            long aliceAssignment = session.assignments().save(Records.assignment(splitId, aliceId)).getId();
            long bobAssignment = session.assignments().save(Records.assignment(splitId, bobId)).getId();
            List<Long> itemIds = new ArrayList<>();
            for (int index = 0; index < sources.size(); index++) {
                long itemId = session.items().save(Records.item(flow.projectId(), sources.get(index))).getId();
                session.splitItems().save(Records.membership(splitId, itemId, index + 1));
                session.annotations().insert(Records.answer(itemId, aliceAssignment, aliceId,
                        flow.labelId(alice.get(index))));
                session.annotations().insert(Records.answer(itemId, bobAssignment, bobId,
                        flow.labelId(bob.get(index))));
                itemIds.add(itemId);
            }
            return itemIds;
        });
    }

    /**
     * Asserts that the call is rejected with a {@link ProjectException} whose message can be shown, and leaves the
     * data file unchanged, naming {@code context} if not.
     */
    private void assertRefused(Executable call, String context) {
        byte[] before = workspace.dataFileBytes();

        ProjectException rejection = assertThrows(ProjectException.class, call, context);

        assertFalse(rejection.getMessage() == null || rejection.getMessage().isBlank(), context);
        assertArrayEquals(before, workspace.dataFileBytes(), context);
    }

    /** Returns the message the resolver gives for this item's source, which must be unreadable. */
    private String resolverMessage(long itemId) {
        Item item = workspace.store().read(session -> session.items().findById(itemId)).orElseThrow();
        SourceResolver resolver = new SourceResolver(workspace.paths());
        return assertThrows(SourceException.class, () -> resolver.resolve(item.getPath(), item.getContentHash()))
                .getMessage();
    }

    /** Returns the project's Unresolved count on the adjudicator's project list. */
    private long unresolved(long projectId) {
        return new ProjectService(workspace.store(), workspace.signInOwner()).list().stream()
                .filter(project -> project.id() == projectId).findFirst().orElseThrow().unresolvedCount();
    }

    private static void assertAdjudicated(Resolution resolution, long labelId, long ownerId, Instant before,
            Instant after) {
        assertEquals(ResolutionMethod.ADJUDICATED, resolution.getMethod());
        assertEquals(labelId, resolution.getLabelId());
        assertNull(resolution.getScaleValue());
        assertEquals(ownerId, resolution.getDecidedByUserId());
        assertFalse(resolution.getDecidedAt().isBefore(before), resolution.getDecidedAt() + " before " + before);
        assertFalse(resolution.getDecidedAt().isAfter(after), resolution.getDecidedAt() + " after " + after);
    }

    private static List<String> keys(TaxonomySummary taxonomy) {
        return taxonomy.labels().stream().map(Label::getKey).toList();
    }

    /** Describes each answer by every stored field, in stored order. */
    private static List<String> describe(List<Annotation> answers) {
        return answers.stream().map(answer -> answer.getId() + " " + answer.getItemId() + " "
                + answer.getAssignmentId() + " " + answer.getAnnotatorId() + " " + answer.getLabelId() + " "
                + answer.getScaleValue() + " " + answer.getSubmittedAt()).toList();
    }
}
