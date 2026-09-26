package arbiter.service;

import static arbiter.service.ServiceAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.data.json.JsonStore;
import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.user.AccountStatus;
import arbiter.model.user.Role;
import arbiter.testing.ClassificationWorkflow;
import arbiter.testing.TestWorkspace;

/** Integration checks for assigning annotators to a split's places and the freezes that follow (#32). */
class AssignmentServiceTest {
    private static final String MISSING_SPLIT = "This split no longer exists";

    @TempDir
    Path temporary;

    private TestWorkspace workspace;
    private AuthService owner;

    @BeforeEach
    void createWorkspace() {
        workspace = TestWorkspace.create(temporary.resolve("workspace"));
        owner = workspace.signInOwner();
    }

    @Test
    void options_newSplit_noKAndOnlyActiveAnnotatorsOfferedInCreationOrder() {
        long zed = annotator("zed");
        long gone = annotator("gone");
        long amy = annotator("amy");
        owner.deactivateAnnotator(gone);
        long splitId = newSplit(2).splitId();

        AssignmentOptions options = service().options(splitId);

        assertEquals(new AssignmentOptions(null, 2, List.of(),
                List.of(new AnnotatorLoad(zed, "zed", 0, 0), new AnnotatorLoad(amy, "amy", 0, 0))), options);
        assertEquals(AssignmentService.DEFAULT_ANNOTATIONS_PER_ITEM, options.prefilledAnnotationsPerItem());
    }

    @Test
    void options_activeAnnotatorsFall_prefillLoweredToActiveCountButNotBelowOne() {
        long first = annotator("first");
        long second = annotator("second");
        long splitId = newSplit(1).splitId();
        AssignmentService service = service();

        owner.deactivateAnnotator(second);
        AssignmentOptions one = service.options(splitId);
        owner.deactivateAnnotator(first);
        AssignmentOptions none = service.options(splitId);

        assertEquals(1, one.activeAnnotators());
        assertEquals(1, one.prefilledAnnotationsPerItem());
        assertEquals(new AssignmentOptions(null, 0, List.of(), List.of()), none);
        assertEquals(1, none.prefilledAnnotationsPerItem());
    }

    @Test
    void options_loadAcrossProjects_unfinishedAssignmentsAndTheirFilesCounted() {
        long first = annotator("first");
        long second = annotator("second");
        long third = annotator("third");
        ClassificationWorkflow.single("pos").items(3).assign("first").seed(workspace);
        ClassificationWorkflow.single("pos").items(2).assign("first", "pos").assign("second").seed(workspace);
        ClassificationWorkflow.single("pos").items(1).assign("first", "pos").assign("second", "pos").seed(workspace);
        long splitId = newSplit(4).splitId();

        List<AnnotatorLoad> offered = service().options(splitId).offered();

        // first: not started (3 files) and in progress (2 files); a submitted assignment is not load.
        assertEquals(List.of(new AnnotatorLoad(first, "first", 2, 5), new AnnotatorLoad(second, "second", 1, 2),
                new AnnotatorLoad(third, "third", 0, 0)), offered);
    }

    @Test
    void options_afterAssignmentAndDeactivation_savedKAndAssigneesInAssignmentOrderWithStatus() {
        long first = annotator("first");
        long second = annotator("second");
        long third = annotator("third");
        long splitId = newSplit(2).splitId();
        AssignmentService service = service();
        service.assign(splitId, "3", List.of(third, first));
        owner.deactivateAnnotator(third);

        AssignmentOptions options = service.options(splitId);

        assertEquals(new AssignmentOptions(3, 2,
                List.of(new AccountSummary(third, "third", Role.ANNOTATOR, AccountStatus.DISABLED),
                        new AccountSummary(first, "first", Role.ANNOTATOR, AccountStatus.ACTIVE)),
                List.of(new AnnotatorLoad(second, "second", 0, 0))), options);
        assertEquals(3, options.prefilledAnnotationsPerItem());
    }

    @Test
    void allMethods_missingSplit_rejectedAndNothingStored() {
        long first = annotator("first");
        long deleted = newSplit(1).splitId();
        corpus().deleteSplit(deleted);
        AssignmentService service = service();
        byte[] before = workspace.dataFileBytes();

        for (long splitId : List.of(deleted, deleted + 1_000)) {
            assertEquals(MISSING_SPLIT, assertRejected(() -> service.options(splitId)).getMessage());
            assertEquals(MISSING_SPLIT, assertRejected(() -> service.check(splitId, "1", List.of(first))).getMessage());
            assertEquals(MISSING_SPLIT,
                    assertRejected(() -> service.assign(splitId, "1", List.of(first))).getMessage());
        }

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void check_validKBeforeFirstAssignment_kReturnedAndNothingStored() {
        long first = annotator("first");
        annotator("second");
        annotator("third");
        long splitId = newSplit(1).splitId();
        AssignmentService service = service();
        Map<String, Integer> expected = Map.of("1", 1, "3", 3, " 2 ", 2, "02", 2);
        byte[] before = workspace.dataFileBytes();

        expected.forEach((text, k) -> assertEquals(k, service.check(splitId, text, List.of(first)), text));

        assertArrayEquals(before, workspace.dataFileBytes());
        assertNull(service.options(splitId).annotationsPerItem());
    }

    @Test
    void checkAndAssign_invalidKBeforeFirstAssignment_rejectedAndNothingStored() {
        long first = annotator("first");
        annotator("second");
        annotator("third");
        owner.deactivateAnnotator(annotator("gone"));
        long splitId = newSplit(1).splitId();
        AssignmentService service = service();
        // U+0663 is the Arabic-Indic digit three.
        List<String> malformed = Arrays.asList(null, "", "   ", "0", "-1", "2.5", "abc", "٣");
        // 4 is one more than the active annotators but not more than every annotator account.
        List<String> tooMany = List.of("4", "99999999999999999999");

        for (String text : malformed) {
            assertRequestRejected(service, splitId, text, List.of(first),
                    "Annotators per file must be a positive whole number");
        }
        for (String text : tooMany) {
            assertRequestRejected(service, splitId, text, List.of(first),
                    "Annotators per file can be at most 3, the number of active annotators");
        }
    }

    @Test
    void assign_firstCommit_notStartedAssignmentsInPickOrderSharingOneTimeAndKSaved() {
        long first = annotator("first");
        annotator("second");
        long third = annotator("third");
        long splitId = newSplit(2).splitId();
        AssignmentService service = service();
        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        List<Assignment> stored = service.assign(splitId, " 3 ", List.of(third, first));

        Instant after = Instant.now();
        assertEquals(List.of(third, first), stored.stream().map(Assignment::getAnnotatorId).toList());
        Instant assignedAt = stored.getFirst().getAssignedAt();
        assertFalse(assignedAt.isBefore(before));
        assertFalse(assignedAt.isAfter(after));
        for (Assignment assignment : stored) {
            assertNotNull(assignment.getId());
            assertEquals(splitId, assignment.getSplitId());
            assertEquals(AssignmentStatus.NOT_STARTED, assignment.getStatus());
            assertEquals(assignedAt, assignment.getAssignedAt());
        }
        JsonStore reopened = JsonStore.open(workspace.paths());
        assertEquals(describe(stored), describe(reopened.read(session -> session.assignments().listBySplit(splitId)
                .stream().sorted(Comparator.comparing(Assignment::getId)).toList())));
        assertEquals(3, service.options(splitId).annotationsPerItem());
    }

    @Test
    void assign_partialFirstCommit_laterCommitFillsFreePlacesUntilFull() {
        long first = annotator("first");
        long second = annotator("second");
        long third = annotator("third");
        ClassificationWorkflow flow = newSplit(2);
        AssignmentService service = service();
        SplitSummary unassigned = split(flow);

        service.assign(flow.splitId(), "3", List.of(first));
        SplitSummary partial = split(flow);
        service.assign(flow.splitId(), "3", List.of(second, third));
        SplitSummary full = split(flow);

        String name = unassigned.name();
        assertEquals(new SplitSummary(flow.splitId(), name, flow.itemIds(), 0, null), unassigned);
        assertEquals(new SplitSummary(flow.splitId(), name, flow.itemIds(), 1, 3), partial);
        assertEquals(new SplitSummary(flow.splitId(), name, flow.itemIds(), 3, 3), full);
        assertEquals(List.of(false, true, true), List.of(unassigned.assigned(), partial.assigned(), full.assigned()));
        assertEquals(List.of(false, false, true), List.of(unassigned.full(), partial.full(), full.full()));
        assertEquals(List.of(first, second, third),
                service.options(flow.splitId()).assignees().stream().map(AccountSummary::id).toList());
    }

    @Test
    void checkAndAssign_afterFirstAssignment_savedKAppliesAndTypedTextIgnored() {
        long first = annotator("first");
        long second = annotator("second");
        long third = annotator("third");
        ClassificationWorkflow flow = newSplit(1);
        AssignmentService service = service();
        service.assign(flow.splitId(), "3", List.of(first));
        // The saved k applies even once it is more than the active annotators.
        owner.deactivateAnnotator(third);
        byte[] before = workspace.dataFileBytes();

        for (String text : Arrays.asList(null, "", "0", "abc", "2", "4")) {
            assertEquals(3, service.check(flow.splitId(), text, List.of(second)), text);
        }
        assertArrayEquals(before, workspace.dataFileBytes());
        service.assign(flow.splitId(), "1", List.of(second));

        assertEquals(3, service.options(flow.splitId()).annotationsPerItem());
        assertEquals(3, split(flow).annotationsPerItem());
    }

    @Test
    void checkAndAssign_invalidPick_rejectedAndNothingStored() {
        long first = annotator("first");
        annotator("second");
        long gone = annotator("gone");
        owner.deactivateAnnotator(gone);
        long ownerId = owner.requireAdjudicator().id();
        long unknown = gone + 1_000;
        long splitId = newSplit(1).splitId();
        AssignmentService service = service();
        Map<List<Long>, String> picks = Map.of(
                List.of(), "Choose at least one annotator",
                List.of(unknown), "A chosen account no longer exists",
                List.of(first, unknown), "A chosen account no longer exists",
                List.of(ownerId), "The workspace owner cannot be assigned",
                List.of(first, ownerId), "The workspace owner cannot be assigned",
                List.of(gone), "gone's account is disabled",
                List.of(first, gone), "gone's account is disabled",
                List.of(first, first), "first is chosen more than once");

        picks.forEach((pick, message) -> assertRequestRejected(service, splitId, "2", pick, message));
    }

    @Test
    void checkAndAssign_currentAssignee_rejectedAndNothingStored() {
        long first = annotator("first");
        long second = annotator("second");
        ClassificationWorkflow flow = newSplit(1);
        AssignmentService service = service();
        service.assign(flow.splitId(), "2", List.of(first));
        String message = "first is already assigned to " + split(flow).name();

        assertRequestRejected(service, flow.splitId(), "2", List.of(first), message);
        assertRequestRejected(service, flow.splitId(), "2", List.of(second, first), message);
    }

    @Test
    void checkAndAssign_morePicksThanFreePlaces_rejectedAndNothingStored() {
        long first = annotator("first");
        long second = annotator("second");
        long third = annotator("third");
        ClassificationWorkflow flow = newSplit(1);
        long splitId = flow.splitId();
        AssignmentService service = service();
        String name = split(flow).name();

        // Two free places on the first commit, then one, then none.
        assertRequestRejected(service, splitId, "2", List.of(first, second, third),
                "Only 2 places are free on " + name);
        service.assign(splitId, "2", List.of(first));
        assertRequestRejected(service, splitId, "2", List.of(second, third), "Only 1 place is free on " + name);
        service.assign(splitId, "2", List.of(second));
        assertRequestRejected(service, splitId, "2", List.of(third), name + " has no free places");
    }

    @Test
    void checkAndAssign_assigneeDeactivated_placeKeptAndReplacementBeyondKRejected() {
        long first = annotator("first");
        long second = annotator("second");
        long third = annotator("third");
        ClassificationWorkflow flow = newSplit(1);
        AssignmentService service = service();
        service.assign(flow.splitId(), "2", List.of(first));
        owner.deactivateAnnotator(first);

        String name = split(flow).name();

        // Two picks would fit only if the disabled assignee's place had been freed.
        assertRequestRejected(service, flow.splitId(), "2", List.of(second, third),
                "Only 1 place is free on " + name);
        service.assign(flow.splitId(), "2", List.of(second));
        assertRequestRejected(service, flow.splitId(), "2", List.of(third), name + " has no free places");

        assertEquals(List.of(first, second),
                service.options(flow.splitId()).assignees().stream().map(AccountSummary::id).toList());
        SplitSummary summary = split(flow);
        assertEquals(2, summary.assignmentCount());
        assertTrue(summary.full());
    }

    @Test
    void checkAndAssign_noActiveAnnotatorsBeforeFirstAssignment_rejectedAndNothingStored() {
        long gone = annotator("gone");
        owner.deactivateAnnotator(gone);
        long splitId = newSplit(1).splitId();

        assertRequestRejected(service(), splitId, "1", List.of(gone), "There are no active annotators to assign");
    }

    @Test
    void assign_firstAssignment_projectFrozenAlsoAfterReopenAndDeactivation() {
        long first = annotator("first");
        ClassificationWorkflow flow = newSplit(2);
        Path added = workspace.paths().root().resolve(workspace.writeSource("added.txt", "An added review")
                .storedPath());

        service().assign(flow.splitId(), "1", List.of(first));

        assertFrozen(workspace.store(), flow, added);
        assertFrozen(JsonStore.open(workspace.paths()), flow, added);
        owner.deactivateAnnotator(first);
        assertFrozen(workspace.store(), flow, added);
        assertFrozen(JsonStore.open(workspace.paths()), flow, added);
    }

    @Test
    void assign_eachAssignment_listedOnlyForItsAnnotatorAlsoAfterDeactivation() {
        long first = annotator("first");
        long second = annotator("second");
        long third = annotator("third");
        long splitId = newSplit(1).splitId();
        List<Assignment> stored = service().assign(splitId, "2", List.of(first, second));
        Map<Long, List<String>> expected = Map.of(first, describe(stored.subList(0, 1)),
                second, describe(stored.subList(1, 2)), third, List.of());

        assertListed(expected);
        owner.deactivateAnnotator(first);
        assertListed(expected);
    }

    @Test
    void allMethods_signedOutAfterUse_authExceptionAndNothingChanged() {
        long first = annotator("first");
        long splitId = newSplit(1).splitId();
        AuthService auth = workspace.signIn(TestWorkspace.OWNER);
        AssignmentService service = new AssignmentService(workspace.store(), auth);
        service.options(splitId);
        auth.logout();
        byte[] before = workspace.dataFileBytes();

        assertThrows(AuthException.class, () -> service.options(splitId));
        assertThrows(AuthException.class, () -> service.options(splitId + 1_000));
        assertThrows(AuthException.class, () -> service.check(splitId, "1", List.of(first)));
        assertThrows(AuthException.class, () -> service.check(splitId, "0", List.of()));
        assertThrows(AuthException.class, () -> service.assign(splitId, "1", List.of(first)));
        assertThrows(AuthException.class, () -> service.assign(splitId, "0", List.of()));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void allMethods_annotator_authExceptionAndNothingChanged() {
        long first = annotator("first");
        long splitId = newSplit(1).splitId();
        AssignmentService service = new AssignmentService(workspace.store(), workspace.signIn("first"));
        byte[] before = workspace.dataFileBytes();

        assertThrows(AuthException.class, () -> service.options(splitId));
        assertThrows(AuthException.class, () -> service.check(splitId, "1", List.of(first)));
        assertThrows(AuthException.class, () -> service.assign(splitId, "1", List.of(first)));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    /** Asserts that check and assign both reject this request with this message and leave the data unchanged. */
    private void assertRequestRejected(AssignmentService service, long splitId, String k, List<Long> picks,
            String message) {
        byte[] before = workspace.dataFileBytes();

        assertEquals(message, assertRejected(() -> service.check(splitId, k, picks)).getMessage(), k + " " + picks);
        assertEquals(message, assertRejected(() -> service.assign(splitId, k, picks)).getMessage(), k + " " + picks);

        assertArrayEquals(before, workspace.dataFileBytes(), k + " " + picks);
    }

    /**
     * Asserts, through services on this store, that the project's corpus is frozen, its assigned split and the
     * project cannot be deleted, and the split keeps k = 1.
     */
    private void assertFrozen(JsonStore store, ClassificationWorkflow flow, Path added) {
        AuthService auth = new AuthService(store);
        auth.login(TestWorkspace.OWNER, TestWorkspace.PASSWORD);
        CorpusService corpus = new CorpusService(store, auth, workspace.paths());
        ProjectService projects = new ProjectService(store, auth);
        byte[] before = workspace.dataFileBytes();

        assertRejected(() -> corpus.register(flow.projectId(), List.of(added)));
        assertRejected(() -> corpus.unregister(flow.itemId(0)));
        assertRejected(() -> corpus.deleteSplit(flow.splitId()));
        assertRejected(() -> projects.delete(flow.projectId()));

        assertArrayEquals(before, workspace.dataFileBytes());
        assertEquals(1, new AssignmentService(store, auth).options(flow.splitId()).annotationsPerItem());
    }

    /** Creates an active annotator through the owner's account management and returns its identifier. */
    private long annotator(String username) {
        return owner.createAnnotator(username, TestWorkspace.PASSWORD).id();
    }

    /** Seeds a project whose one split holds this many items and has no assignment. */
    private ClassificationWorkflow newSplit(int items) {
        return ClassificationWorkflow.single("pos").items(items).seed(workspace);
    }

    private AssignmentService service() {
        return new AssignmentService(workspace.store(), owner);
    }

    private CorpusService corpus() {
        return new CorpusService(workspace.store(), owner, workspace.paths());
    }

    /** Returns the workflow's split as the project page lists it. */
    private SplitSummary split(ClassificationWorkflow flow) {
        return corpus().listSplits(flow.projectId()).getFirst();
    }

    /**
     * Asserts that the repository lists each annotator exactly these assignments, on the open store and on a
     * reopened one.
     */
    private void assertListed(Map<Long, List<String>> expected) {
        for (JsonStore store : List.of(workspace.store(), JsonStore.open(workspace.paths()))) {
            expected.forEach((annotatorId, assignments) -> assertEquals(assignments,
                    describe(store.read(session -> session.assignments().listByAnnotator(annotatorId)))));
        }
    }

    private static List<String> describe(List<Assignment> assignments) {
        return assignments.stream().map(assignment -> assignment.getId() + " " + assignment.getSplitId() + " "
                + assignment.getAnnotatorId() + " " + assignment.getStatus() + " " + assignment.getAssignedAt())
                .toList();
    }
}
