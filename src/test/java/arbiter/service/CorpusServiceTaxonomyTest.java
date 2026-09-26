package arbiter.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import arbiter.data.json.JsonStore;
import arbiter.data.json.JsonStoreException;
import arbiter.model.project.Label;
import arbiter.model.project.TaxonomyKind;
import arbiter.testing.ClassificationWorkflow;
import arbiter.testing.Records;
import arbiter.testing.TestWorkspace;

/** Integration checks for setting up a project's taxonomy and its freeze at the first assignment (#26). */
class CorpusServiceTaxonomyTest {
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
    void addLabel_threeLabels_appendedInOrderAcrossReopen() {
        long projectId = workspace.newProject(TaxonomyKind.SINGLE);
        CorpusService service = service();

        List<Label> added = List.of(service.addLabel(projectId, "positive", "Upbeat"),
                service.addLabel(projectId, "negative", null),
                service.addLabel(projectId, "neutral", null));

        for (Label label : added) {
            assertNotNull(label.getId());
            assertEquals(projectId, label.getProjectId());
        }
        assertEquals(List.of("positive 1 Upbeat", "negative 2 null", "neutral 3 null"), added.stream()
                .map(label -> label.getKey() + " " + label.getSequence() + " " + label.getDescription()).toList());
        assertEquals(describe(added), describe(service.taxonomy(projectId).labels()));
        assertEquals(describe(added), describe(reopened().taxonomy(projectId).labels()));
    }

    @Test
    void addLabel_keyAtLengthBoundsOrWithPunctuation_storedStripped() {
        long projectId = workspace.newProject(TaxonomyKind.SINGLE);
        CorpusService service = service();
        String hundred = "k".repeat(49) + "  " + "k".repeat(49);
        String punctuation = "Very good: 5/5 (!) ~ok";

        assertEquals("x", service.addLabel(projectId, " \tx  ", null).getKey());
        assertEquals(hundred, service.addLabel(projectId, "  " + hundred + "\t", null).getKey());
        assertEquals(punctuation, service.addLabel(projectId, punctuation, null).getKey());

        assertEquals(List.of("x@1", hundred + "@2", punctuation + "@3"), order(reopened(), projectId));
    }

    @Test
    void addLabel_storedPositionsWithGap_appendedLast() {
        long projectId = workspace.newProject(TaxonomyKind.SINGLE);
        // Only a hand-edited data file leaves a gap, since every write here numbers labels from 1.
        workspace.store().write(session -> {
            session.labels().save(Records.label(projectId, "first", 1));
            return session.labels().save(Records.label(projectId, "second", 5));
        });

        service().addLabel(projectId, "third", null);

        assertEquals(List.of("first@1", "second@5", "third@6"), order(reopened(), projectId));
    }

    @Test
    void addAndEditLabel_keyBreakingNameRule_rejectedAndNothingStored() {
        long projectId = singleWith("pos");
        long pos = labelId(projectId, "pos");
        CorpusService service = service();
        List<String> keys = Arrays.asList(null, "", " \t ", " " + "k".repeat(101) + " ", "Café", "pos 😀",
                "pos\tneg");

        for (String key : keys) {
            assertRefused(() -> service.addLabel(projectId, key, "A valid description"), "add " + key);
            assertRefused(() -> service.editLabel(pos, key, "A valid description"), "edit " + key);
        }

        assertEquals(List.of("pos@1"), order(service, projectId));
    }

    @Test
    void addLabel_keyTakenIgnoringCase_rejectedButFreeInAnotherProject() {
        long projectId = singleWith("Positive");
        long otherId = workspace.newProject(TaxonomyKind.SINGLE);
        CorpusService service = service();

        assertRefused(() -> service.addLabel(projectId, "POSITIVE", null));
        assertRefused(() -> service.addLabel(projectId, "  positive ", "Upbeat"));
        assertEquals("positive", service.addLabel(otherId, "positive", null).getKey());

        assertEquals(List.of("Positive@1"), order(service, projectId));
    }

    @Test
    void addAndEditLabel_description_blankStoredAsNullOtherwiseStripped() {
        long projectId = workspace.newProject(TaxonomyKind.SINGLE);
        CorpusService service = service();
        service.addLabel(projectId, "none", null);
        service.addLabel(projectId, "empty", "");
        service.addLabel(projectId, "blank", " \t\n ");
        service.addLabel(projectId, "padded", "  Upbeat tone \n");
        long cleared = service.addLabel(projectId, "cleared", "Old").getId();
        long replaced = service.addLabel(projectId, "replaced", "Old").getId();

        service.editLabel(cleared, "cleared", " \t ");
        service.editLabel(replaced, "replaced", "  New tone ");

        assertEquals(Arrays.asList(null, null, null, "Upbeat tone", null, "New tone"),
                reopened().taxonomy(projectId).labels().stream().map(Label::getDescription).toList());
    }

    @Test
    void editLabel_newKeyAndDescription_changedInPlaceAcrossReopen() {
        long projectId = singleWith("positive", "negative", "neutral");
        long negative = labelId(projectId, "negative");

        Label edited = service().editLabel(negative, "  bad ", " Unhappy tone ");

        assertEquals(negative + " " + projectId + " bad 2 Unhappy tone", describe(List.of(edited)).getFirst());
        List<Label> stored = reopened().taxonomy(projectId).labels();
        assertEquals(List.of("positive@1", "bad@2", "neutral@3"), order(stored));
        assertEquals(describe(List.of(edited)), describe(stored.subList(1, 2)));
    }

    @Test
    void editLabel_ownKeyOrCaseOnlyChange_saved() {
        long projectId = singleWith("pos", "neg");
        long pos = labelId(projectId, "pos");
        CorpusService service = service();

        assertEquals("pos", service.editLabel(pos, "pos", "Same key").getKey());
        Label edited = service.editLabel(pos, " POS ", null);

        assertEquals("POS", edited.getKey());
        assertNull(edited.getDescription());
        assertEquals(List.of("POS@1", "neg@2"), order(reopened(), projectId));
    }

    @Test
    void editLabel_keyOfAnotherLabelIgnoringCase_rejectedAndNothingChanged() {
        long projectId = singleWith("pos", "neg");
        long pos = labelId(projectId, "pos");
        CorpusService service = service();

        assertRefused(() -> service.editLabel(pos, "neg", null));
        assertRefused(() -> service.editLabel(pos, " NEG ", "Unhappy"));
    }

    @Test
    void moveLabel_upAndDown_swappedWithNeighbour() {
        long projectId = singleWith("a", "b", "c");
        long a = labelId(projectId, "a");
        long c = labelId(projectId, "c");
        CorpusService service = service();

        service.moveLabelUp(c);
        assertEquals(List.of("a@1", "c@2", "b@3"), order(service, projectId));
        service.moveLabelUp(c);
        assertEquals(List.of("c@1", "a@2", "b@3"), order(service, projectId));
        service.moveLabelDown(a);
        assertEquals(List.of("c@1", "b@2", "a@3"), order(service, projectId));
        service.moveLabelDown(c);

        assertEquals(List.of("b@1", "c@2", "a@3"), order(reopened(), projectId));
    }

    @Test
    void moveLabel_storedPositionsWithGaps_numberedFromOne() {
        long projectId = workspace.newProject(TaxonomyKind.SINGLE);
        // Only a hand-edited data file leaves a gap, since every write here numbers labels from 1.
        workspace.store().write(session -> {
            session.labels().save(Records.label(projectId, "a", 2));
            session.labels().save(Records.label(projectId, "b", 5));
            return session.labels().save(Records.label(projectId, "c", 9));
        });

        service().moveLabelUp(labelId(projectId, "c"));

        assertEquals(List.of("a@1", "c@2", "b@3"), order(reopened(), projectId));
    }

    @Test
    void moveLabel_pastEitherEnd_rejectedAndNothingChanged() {
        long projectId = singleWith("a", "b", "c");
        long loneId = singleWith("only");
        CorpusService service = service();

        assertRefused(() -> service.moveLabelUp(labelId(projectId, "a")));
        assertRefused(() -> service.moveLabelDown(labelId(projectId, "c")));
        assertRefused(() -> service.moveLabelUp(labelId(loneId, "only")));
        assertRefused(() -> service.moveLabelDown(labelId(loneId, "only")));

        assertEquals(List.of("a@1", "b@2", "c@3"), order(service, projectId));
    }

    @Test
    void deleteLabel_firstOrMiddleLabel_restNumberedFromOneAndKeyFreed() {
        long projectId = singleWith("a", "b", "c", "d");
        CorpusService service = service();

        service.deleteLabel(labelId(projectId, "b"));
        assertEquals(List.of("a@1", "c@2", "d@3"), order(service, projectId));
        service.deleteLabel(labelId(projectId, "a"));
        assertEquals(List.of("c@1", "d@2"), order(service, projectId));
        service.addLabel(projectId, "B", null);

        assertEquals(List.of("c@1", "d@2", "B@3"), order(reopened(), projectId));
    }

    @Test
    void deleteLabel_usedByAnswerOrResolutionBeforeAssignment_rejectedAndNothingChanged() {
        // Only an inconsistent data file holds either before the first assignment. An answer needs an assignment,
        // so this one borrows another project's.
        ClassificationWorkflow other = ClassificationWorkflow.single("yes", "no").assign("annotator")
                .seed(workspace);
        long projectId = singleWith("answered", "resolved");
        long answered = labelId(projectId, "answered");
        long resolved = labelId(projectId, "resolved");
        workspace.store().write(session -> {
            long itemId = session.items().save(Records.item(projectId, "inconsistent")).getId();
            session.annotations().insert(Records.answer(itemId, other.assignmentId("annotator"),
                    other.annotatorId("annotator"), answered));
            return session.resolutions().save(Records.resolution(itemId, resolved));
        });
        CorpusService service = service();
        assertFalse(service.taxonomy(projectId).frozen());
        byte[] before = workspace.dataFileBytes();

        assertThrows(JsonStoreException.class, () -> service.deleteLabel(answered));
        assertThrows(JsonStoreException.class, () -> service.deleteLabel(resolved));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void labelAndRangeWrites_wrongKind_rejectedAndNothingStored() {
        long singleId = workspace.newProject(TaxonomyKind.SINGLE);
        long scaleId = workspace.newProject(TaxonomyKind.SCALE);
        CorpusService service = service();

        assertRefused(() -> service.addLabel(scaleId, "positive", null));
        assertRefused(() -> service.saveRange(singleId, "1", "5"));

        assertEquals(new TaxonomySummary(TaxonomyKind.SINGLE, List.of(), null, null, false),
                service.taxonomy(singleId));
        assertEquals(new TaxonomySummary(TaxonomyKind.SCALE, List.of(), null, null, false),
                service.taxonomy(scaleId));
    }

    @Test
    void saveRange_endsWithinBounds_storedAndReplacedAcrossReopen() {
        long projectId = workspace.newProject(TaxonomyKind.SCALE);
        CorpusService service = service();

        service.saveRange(projectId, "-10", "10");
        assertEquals(range(-10, 10), service.taxonomy(projectId));
        service.saveRange(projectId, "-9", "9");
        assertEquals(range(-9, 9), service.taxonomy(projectId));
        service.saveRange(projectId, " 3 ", "\t4\n");

        assertEquals(range(3, 4), reopened().taxonomy(projectId));
    }

    @Test
    void saveRange_endNotWholeNumberWithinBounds_rejectedAndSavedRangeKept() {
        long projectId = workspace.newProject(TaxonomyKind.SCALE);
        CorpusService service = service();
        service.saveRange(projectId, "1", "5");
        // U+0663 is the Arabic-Indic digit three.
        List<String> malformed = Arrays.asList(null, "", "   ", "abc", "2.5", "٣", "+3");

        for (String text : malformed) {
            assertRefused(() -> service.saveRange(projectId, text, "5"), "minimum " + text);
            assertRefused(() -> service.saveRange(projectId, "1", text), "maximum " + text);
        }
        for (String minimum : List.of("-11", "-99999999999999999999")) {
            assertRefused(() -> service.saveRange(projectId, minimum, "5"), "minimum " + minimum);
        }
        for (String maximum : List.of("11", "99999999999999999999")) {
            assertRefused(() -> service.saveRange(projectId, "1", maximum), "maximum " + maximum);
        }

        assertEquals(range(1, 5), service.taxonomy(projectId));
    }

    @Test
    void saveRange_minimumNotBelowMaximum_rejectedAndNothingStored() {
        long projectId = workspace.newProject(TaxonomyKind.SCALE);
        CorpusService service = service();

        assertRefused(() -> service.saveRange(projectId, "3", "3"));
        assertRefused(() -> service.saveRange(projectId, "4", "3"));
        assertRefused(() -> service.saveRange(projectId, "10", "-10"));

        assertEquals(new TaxonomySummary(TaxonomyKind.SCALE, List.of(), null, null, false),
                service.taxonomy(projectId));
    }

    @Test
    void taxonomyWrites_afterFirstAssignment_rejectedFromStaleViewAndAfterReopen() {
        long annotator = owner.createAnnotator("annotator", TestWorkspace.PASSWORD).id();
        CorpusService service = service();
        long singleId = singleWith("pos", "neg");
        long scaleId = workspace.newProject(TaxonomyKind.SCALE);
        service.saveRange(scaleId, "1", "5");
        long singleSplit = workspace.newSplits(singleId, 1).getFirst();
        long scaleSplit = workspace.newSplits(scaleId, 1).getFirst();
        TaxonomySummary stale = service.taxonomy(singleId);
        AssignmentService assignments = new AssignmentService(workspace.store(), owner);

        assignments.assign(singleSplit, "1", List.of(annotator));
        assignments.assign(scaleSplit, "1", List.of(annotator));

        assertFalse(stale.frozen());
        long pos = stale.labels().get(0).getId();
        long neg = stale.labels().get(1).getId();
        for (JsonStore store : List.of(workspace.store(), JsonStore.open(workspace.paths()))) {
            CorpusService current = serviceOn(store);
            assertRefused(() -> current.addLabel(singleId, "neutral", null));
            assertRefused(() -> current.editLabel(pos, "pos", "A new description only"));
            assertRefused(() -> current.moveLabelUp(neg));
            assertRefused(() -> current.deleteLabel(pos));
            assertRefused(() -> current.saveRange(scaleId, "1", "6"));
            assertTrue(current.taxonomy(singleId).frozen());
            assertEquals(List.of("pos@1", "neg@2"), order(current, singleId));
            assertEquals(new TaxonomySummary(TaxonomyKind.SCALE, List.of(), 1, 5, true), current.taxonomy(scaleId));
        }
    }

    @Test
    void taxonomyWrites_otherProjectAssigned_acceptedAndNotFrozen() {
        ClassificationWorkflow.single("yes", "no").assign("annotator").seed(workspace);
        ClassificationWorkflow.scale(1, 5).assign("rater").seed(workspace);
        long singleId = workspace.newProject(TaxonomyKind.SINGLE);
        long scaleId = workspace.newProject(TaxonomyKind.SCALE);
        CorpusService service = service();

        long a = service.addLabel(singleId, "a", null).getId();
        service.addLabel(singleId, "b", null);
        service.editLabel(a, "A", null);
        service.moveLabelDown(a);
        service.deleteLabel(a);
        service.saveRange(scaleId, "-2", "2");

        assertFalse(service.taxonomy(singleId).frozen());
        assertEquals(List.of("b@1"), order(service, singleId));
        assertEquals(new TaxonomySummary(TaxonomyKind.SCALE, List.of(), -2, 2, false), service.taxonomy(scaleId));
    }

    @Test
    void taxonomyMethods_missingProjectOrLabel_rejectedAndNothingChanged() {
        ProjectService projects = new ProjectService(workspace.store(), owner);
        long deletedSingle = workspace.newProject(TaxonomyKind.SINGLE);
        long deletedScale = workspace.newProject(TaxonomyKind.SCALE);
        projects.delete(deletedSingle);
        projects.delete(deletedScale);
        long projectId = singleWith("kept", "gone");
        long gone = labelId(projectId, "gone");
        CorpusService service = service();
        service.deleteLabel(gone);

        for (long missing : List.of(deletedSingle, deletedScale, deletedScale + 1_000)) {
            assertRefused(() -> service.taxonomy(missing), "taxonomy " + missing);
            assertRefused(() -> service.addLabel(missing, "new", null), "add " + missing);
            assertRefused(() -> service.saveRange(missing, "1", "5"), "range " + missing);
        }
        for (long missing : List.of(gone, gone + 1_000)) {
            assertRefused(() -> service.editLabel(missing, "new", null), "edit " + missing);
            assertRefused(() -> service.moveLabelUp(missing), "up " + missing);
            assertRefused(() -> service.moveLabelDown(missing), "down " + missing);
            assertRefused(() -> service.deleteLabel(missing), "delete " + missing);
        }

        assertEquals(List.of("kept@1"), order(service, projectId));
    }

    @Test
    void taxonomyMethods_signedOutAfterUse_authExceptionAndNothingChanged() {
        long singleId = singleWith("pos", "neg");
        long scaleId = workspace.newProject(TaxonomyKind.SCALE);
        long pos = labelId(singleId, "pos");
        AuthService auth = workspace.signIn(TestWorkspace.OWNER);
        CorpusService service = new CorpusService(workspace.store(), auth, workspace.paths());
        service.taxonomy(singleId);
        auth.logout();
        byte[] before = workspace.dataFileBytes();

        assertThrows(AuthException.class, () -> service.taxonomy(singleId));
        assertThrows(AuthException.class, () -> service.taxonomy(singleId + 1_000));
        assertThrows(AuthException.class, () -> service.addLabel(singleId, "neutral", null));
        assertThrows(AuthException.class, () -> service.addLabel(singleId, "", null));
        assertThrows(AuthException.class, () -> service.editLabel(pos, "good", null));
        assertThrows(AuthException.class, () -> service.editLabel(pos, "neg", null));
        assertThrows(AuthException.class, () -> service.moveLabelDown(pos));
        assertThrows(AuthException.class, () -> service.moveLabelUp(pos));
        assertThrows(AuthException.class, () -> service.deleteLabel(pos));
        assertThrows(AuthException.class, () -> service.deleteLabel(pos + 1_000));
        assertThrows(AuthException.class, () -> service.saveRange(scaleId, "1", "5"));
        assertThrows(AuthException.class, () -> service.saveRange(scaleId, "abc", "5"));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void taxonomyMethods_annotator_authExceptionAndNothingChanged() {
        long singleId = singleWith("pos", "neg");
        long scaleId = workspace.newProject(TaxonomyKind.SCALE);
        long pos = labelId(singleId, "pos");
        owner.createAnnotator("annotator", TestWorkspace.PASSWORD);
        CorpusService service = new CorpusService(workspace.store(), workspace.signIn("annotator"),
                workspace.paths());
        byte[] before = workspace.dataFileBytes();

        assertThrows(AuthException.class, () -> service.taxonomy(singleId));
        assertThrows(AuthException.class, () -> service.addLabel(singleId, "neutral", null));
        assertThrows(AuthException.class, () -> service.editLabel(pos, "good", null));
        assertThrows(AuthException.class, () -> service.moveLabelDown(pos));
        assertThrows(AuthException.class, () -> service.deleteLabel(pos));
        assertThrows(AuthException.class, () -> service.saveRange(scaleId, "1", "5"));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    /** Asserts that the call is rejected with a {@link ProjectException} and leaves the data file unchanged. */
    private void assertRefused(Executable call) {
        assertRefused(call, null);
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

    /** Returns a service for the owner. */
    private CorpusService service() {
        return new CorpusService(workspace.store(), owner, workspace.paths());
    }

    /** Returns a service for the owner on this store, as after a restart. */
    private CorpusService serviceOn(JsonStore store) {
        AuthService auth = new AuthService(store);
        auth.login(TestWorkspace.OWNER, TestWorkspace.PASSWORD);
        return new CorpusService(store, auth, workspace.paths());
    }

    /** Returns a service for the owner on a newly opened store. */
    private CorpusService reopened() {
        return serviceOn(JsonStore.open(workspace.paths()));
    }

    /** Creates a SINGLE project and adds labels with these keys, in this order, through the services. */
    private long singleWith(String... keys) {
        long projectId = workspace.newProject(TaxonomyKind.SINGLE);
        CorpusService service = service();
        for (String key : keys) {
            service.addLabel(projectId, key, null);
        }
        return projectId;
    }

    /** Returns the identifier of the project's label with this key. */
    private long labelId(long projectId, String key) {
        return service().taxonomy(projectId).labels().stream().filter(label -> label.getKey().equals(key))
                .findFirst().orElseThrow().getId();
    }

    /** Returns the project's labels, as this service reads them, as {@code key@sequence} in order. */
    private static List<String> order(CorpusService service, long projectId) {
        return order(service.taxonomy(projectId).labels());
    }

    private static List<String> order(List<Label> labels) {
        return labels.stream().map(label -> label.getKey() + "@" + label.getSequence()).toList();
    }

    private static List<String> describe(List<Label> labels) {
        return labels.stream().map(label -> label.getId() + " " + label.getProjectId() + " " + label.getKey() + " "
                + label.getSequence() + " " + label.getDescription()).toList();
    }

    /** Returns the taxonomy of a SCALE project with this saved range and no first assignment. */
    private static TaxonomySummary range(int minimum, int maximum) {
        return new TaxonomySummary(TaxonomyKind.SCALE, List.of(), minimum, maximum, false);
    }
}
