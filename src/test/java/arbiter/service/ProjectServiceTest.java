package arbiter.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.OutputFormat;
import arbiter.model.project.Project;
import arbiter.model.project.Split;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.project.TaxonomySettings;
import arbiter.testing.ClassificationWorkflow;
import arbiter.testing.Records;
import arbiter.testing.TestWorkspace;
import arbiter.workspace.ResolvedSource;

/** Integration checks for creating, listing and deleting projects (#24, #30). */
class ProjectServiceTest {
    @TempDir
    Path temporary;

    private TestWorkspace workspace;

    @BeforeEach
    void createWorkspace() {
        workspace = TestWorkspace.create(temporary.resolve("workspace"));
    }

    @Test
    void create_eachKindAndFormat_storedWithSettingsAcrossReopen() {
        ProjectService service = ownerService();
        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        Project single = service.create("Tweets", "Sentiment", TaxonomyKind.SINGLE, OutputFormat.CSV);
        Project scale = service.create("Reviews", "Ratings", TaxonomyKind.SCALE, OutputFormat.JSON);

        Instant after = Instant.now();
        JsonStore reopened = JsonStore.open(workspace.paths());
        for (Project returned : List.of(single, scale)) {
            Project stored = reopened.read(session -> session.projects().findById(returned.getId())).orElseThrow();
            assertEquals(returned.getName(), stored.getName());
            assertEquals(returned.getDescription(), stored.getDescription());
            assertEquals(returned.getOutputFormat(), stored.getOutputFormat());
            assertEquals(returned.getCreatedAt(), stored.getCreatedAt());
            assertFalse(stored.getCreatedAt().isBefore(before));
            assertFalse(stored.getCreatedAt().isAfter(after));
        }
        assertEquals("Tweets", single.getName());
        assertEquals("Sentiment", single.getDescription());
        assertEquals(OutputFormat.CSV, single.getOutputFormat());
        assertEquals(OutputFormat.JSON, scale.getOutputFormat());
        TaxonomySettings singleSettings = settings(reopened, single.getId());
        TaxonomySettings scaleSettings = settings(reopened, scale.getId());
        assertEquals(TaxonomyKind.SINGLE, singleSettings.getKind());
        assertEquals(TaxonomyKind.SCALE, scaleSettings.getKind());
        // The scale range is set later, under #26.
        assertNull(scaleSettings.getScaleMin());
        assertNull(scaleSettings.getScaleMax());
    }

    @Test
    void create_nameOneOrHundredCharactersAfterStripping_storedStripped() {
        ProjectService service = ownerService();
        String hundred = "n".repeat(49) + "  " + "n".repeat(49);

        Project shortest = service.create(" \tx  ", null, TaxonomyKind.SINGLE, OutputFormat.CSV);
        Project longest = service.create("  " + hundred + "\t", null, TaxonomyKind.SINGLE, OutputFormat.CSV);

        assertEquals("x", shortest.getName());
        assertEquals(hundred, longest.getName());
        assertEquals(List.of("x", hundred), JsonStore.open(workspace.paths()).<List<String>>read(session ->
                session.projects().listAll().stream().map(Project::getName).toList()));
    }

    @Test
    void create_nullEmptyOrWhitespaceName_rejectedAndNothingStored() {
        ProjectService service = ownerService();
        byte[] before = dataFile();

        for (String name : new String[] {null, "", " \t "}) {
            assertRejected(() -> service.create(name, "Sentiment", TaxonomyKind.SINGLE, OutputFormat.CSV));
        }

        assertArrayEquals(before, dataFile());
    }

    @Test
    void create_name101CharactersAfterStripping_rejectedAndNothingStored() {
        ProjectService service = ownerService();
        byte[] before = dataFile();

        assertRejected(() -> service.create(" " + "n".repeat(101) + " ", null, TaxonomyKind.SINGLE,
                OutputFormat.CSV));

        assertArrayEquals(before, dataFile());
    }

    @Test
    void create_nameWithNonAsciiOrControlCharacter_rejectedAndNothingStored() {
        ProjectService service = ownerService();
        byte[] before = dataFile();

        for (String name : new String[] {"Café reviews", "Tweets 😀", "Tweets\tv2"}) {
            assertRejected(() -> service.create(name, null, TaxonomyKind.SINGLE, OutputFormat.CSV));
        }

        assertArrayEquals(before, dataFile());
    }

    @Test
    void create_nameWithAsciiPunctuation_accepted() {
        String name = "Reviews: v2 (en-GB) & ~more!";

        assertEquals(name, ownerService().create(name, null, TaxonomyKind.SINGLE, OutputFormat.CSV).getName());
    }

    @Test
    void create_nameDuplicateIgnoringCase_rejectedAndNothingStored() {
        ProjectService service = ownerService();
        service.create("Film Reviews", null, TaxonomyKind.SINGLE, OutputFormat.CSV);
        byte[] before = dataFile();

        assertRejected(() -> service.create("FILM reviews", null, TaxonomyKind.SCALE, OutputFormat.JSON));
        assertRejected(() -> service.create("  film reviews ", null, TaxonomyKind.SINGLE, OutputFormat.CSV));

        assertArrayEquals(before, dataFile());
        assertEquals("Film Reviews 2",
                service.create("Film Reviews 2", null, TaxonomyKind.SINGLE, OutputFormat.CSV).getName());
    }

    @Test
    void create_nameOfDeletedProject_accepted() {
        ProjectService service = ownerService();
        Project deleted = service.create("Film Reviews", null, TaxonomyKind.SINGLE, OutputFormat.CSV);
        service.delete(deleted.getId());

        Project again = service.create("film reviews", null, TaxonomyKind.SCALE, OutputFormat.JSON);

        assertEquals(List.of("film reviews"), service.list().stream().map(ProjectSummary::name).toList());
        assertEquals(again.getId(), service.list().getFirst().id());
    }

    @Test
    void create_missingKind_rejectedAndNothingStored() {
        ProjectService service = ownerService();
        byte[] before = dataFile();

        assertRejected(() -> service.create("Tweets", null, null, OutputFormat.CSV));

        assertArrayEquals(before, dataFile());
    }

    @Test
    void create_missingFormat_rejectedAndNothingStored() {
        ProjectService service = ownerService();
        byte[] before = dataFile();

        assertRejected(() -> service.create("Tweets", null, TaxonomyKind.SINGLE, null));

        assertArrayEquals(before, dataFile());
    }

    @Test
    void create_description_blankStoredAsNullOtherwiseStripped() {
        ProjectService service = ownerService();
        long none = service.create("None", null, TaxonomyKind.SINGLE, OutputFormat.CSV).getId();
        long empty = service.create("Empty", "", TaxonomyKind.SINGLE, OutputFormat.CSV).getId();
        long blank = service.create("Blank", " \t\n ", TaxonomyKind.SINGLE, OutputFormat.CSV).getId();
        long padded = service.create("Padded", "  Tweets about films \n", TaxonomyKind.SINGLE, OutputFormat.CSV)
                .getId();

        JsonStore reopened = JsonStore.open(workspace.paths());
        assertNull(project(reopened, none).getDescription());
        assertNull(project(reopened, empty).getDescription());
        assertNull(project(reopened, blank).getDescription());
        assertEquals("Tweets about films", project(reopened, padded).getDescription());
    }

    @Test
    void list_noProjects_emptyList() {
        assertEquals(List.of(), ownerService().list());
    }

    @Test
    void list_createdProjects_creationOrderWithZeroCounts() {
        ProjectService service = ownerService();
        long beta = service.create("Beta", null, TaxonomyKind.SCALE, OutputFormat.CSV).getId();
        long alpha = service.create("alpha", "First letter", TaxonomyKind.SINGLE, OutputFormat.JSON).getId();
        long gamma = service.create("Gamma", null, TaxonomyKind.SINGLE, OutputFormat.CSV).getId();

        assertEquals(List.of(
                new ProjectSummary(beta, "Beta", TaxonomyKind.SCALE, OutputFormat.CSV, 0, 0, 0, 0),
                new ProjectSummary(alpha, "alpha", TaxonomyKind.SINGLE, OutputFormat.JSON, 0, 0, 0, 0),
                new ProjectSummary(gamma, "Gamma", TaxonomyKind.SINGLE, OutputFormat.CSV, 0, 0, 0, 0)),
                service.list());
    }

    @Test
    void list_seededProjects_countsEachProjectSeparately() {
        // Items 0 and 1 are resolved; item 2 is disputed and item 3 has one answer, so both are unresolved.
        ClassificationWorkflow first = ClassificationWorkflow.single("pos", "neg").items(4)
                .assign("submitted", "pos", "pos", "neg", "pos")
                .assign("inProgress", "pos", "pos", "pos")
                .annotator("notStarted")
                .majority(0, "pos")
                .majority(1, "pos")
                .seed(workspace);
        // A second split holds a fifth, unresolved item, assigned but not started.
        assignNewSplit(first.projectId(), first.annotatorId("notStarted"));
        ClassificationWorkflow second = ClassificationWorkflow.scale(1, 5).items(2)
                .assign("rater", 3, 4)
                .assign("slowRater", 5)
                .mean(0, 4.0)
                .seed(workspace);

        List<ProjectSummary> summaries = ownerService().list();

        assertEquals(List.of(
                new ProjectSummary(first.projectId(), "Synthetic project", TaxonomyKind.SINGLE, OutputFormat.JSON,
                        5, 2, 3, 3),
                new ProjectSummary(second.projectId(), "Synthetic project", TaxonomyKind.SCALE, OutputFormat.JSON,
                        2, 1, 2, 1)),
                summaries);
    }

    @Test
    void delete_projectBeforeAssignment_ownedRecordsRemovedAndRestKept() {
        ClassificationWorkflow doomed = ClassificationWorkflow.single("pos", "neg").items(2)
                .annotator("spare")
                .seed(workspace);
        ClassificationWorkflow kept = ClassificationWorkflow.single("yes", "no")
                .assign("annotator", "yes")
                .majority(0, "yes")
                .seed(workspace);
        ProjectService service = ownerService();
        ProjectSummary keptBefore = service.list().get(1);
        List<String> accountsBefore = accounts();
        Map<Path, String> mediaBefore = mediaFiles();

        service.delete(doomed.projectId());

        JsonStore reopened = JsonStore.open(workspace.paths());
        long projectId = doomed.projectId();
        assertTrue(reopened.<Boolean>read(session -> session.projects().findById(projectId).isEmpty()));
        assertTrue(reopened.<Boolean>read(session -> session.taxonomySettings().findByProject(projectId).isEmpty()));
        assertTrue(reopened.<Boolean>read(session -> session.labels().listByProject(projectId).isEmpty()));
        assertTrue(reopened.<Boolean>read(session -> session.labels().findById(doomed.labelId("pos")).isEmpty()));
        assertTrue(reopened.<Boolean>read(session -> session.items().listByProject(projectId).isEmpty()));
        assertTrue(reopened.<Boolean>read(session -> session.splits().listByProject(projectId).isEmpty()));
        assertTrue(reopened.<Boolean>read(session -> session.splitItems().listBySplit(doomed.splitId()).isEmpty()));
        for (long itemId : doomed.itemIds()) {
            assertTrue(reopened.<Boolean>read(session -> session.items().findById(itemId).isEmpty()));
            assertTrue(reopened.<Boolean>read(session -> session.splitItems().findByItem(itemId).isEmpty()));
        }
        assertEquals(List.of(keptBefore), service.list());
        assertEquals(2, reopened.<Integer>read(session -> session.labels().listByProject(kept.projectId()).size()));
        assertTrue(reopened.<Boolean>read(session -> session.annotations().findByItemAndAnnotator(kept.itemId(0),
                kept.annotatorId("annotator")).isPresent()));
        assertTrue(reopened.<Boolean>read(session -> session.resolutions().findByItem(kept.itemId(0)).isPresent()));
        assertEquals(accountsBefore, accounts());
        assertEquals(3, mediaBefore.size());
        assertEquals(mediaBefore, mediaFiles());
    }

    @Test
    void delete_notStartedAssignment_rejectedAndNothingChanged() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos").items(2).assign("annotator").seed(workspace);
        ProjectService service = ownerService();
        byte[] before = dataFile();

        assertRejected(() -> service.delete(flow.projectId()));

        assertArrayEquals(before, dataFile());
    }

    @Test
    void delete_assignedAnnotatorDisabled_rejectedAndNothingChanged() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos").items(2)
                .assign("annotator", "pos")
                .disabled("annotator")
                .seed(workspace);
        ProjectService service = ownerService();
        byte[] before = dataFile();

        assertRejected(() -> service.delete(flow.projectId()));

        assertArrayEquals(before, dataFile());
    }

    @Test
    void delete_assignedProjectAfterRestart_rejectedAndNothingChanged() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos").items(2).assign("annotator").seed(workspace);
        byte[] before = dataFile();
        JsonStore reopened = JsonStore.open(workspace.paths());
        AuthService auth = new AuthService(reopened);
        auth.login(TestWorkspace.OWNER, TestWorkspace.PASSWORD);

        assertRejected(() -> new ProjectService(reopened, auth).delete(flow.projectId()));

        assertArrayEquals(before, dataFile());
    }

    @Test
    void delete_assignmentOnLaterSplitAfterListRead_rejectedAndNothingChanged() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos").items(2)
                .annotator("annotator")
                .seed(workspace);
        ProjectService service = ownerService();
        ProjectSummary stale = service.list().getFirst();
        assertEquals(0, stale.assignmentCount());
        assignNewSplit(flow.projectId(), flow.annotatorId("annotator"));
        byte[] before = dataFile();

        assertRejected(() -> service.delete(stale.id()));

        assertArrayEquals(before, dataFile());
    }

    @Test
    void delete_missingProject_rejectedAndNothingChanged() {
        ClassificationWorkflow.single("pos").seed(workspace);
        ProjectService service = ownerService();
        long deleted = service.create("Tweets", null, TaxonomyKind.SINGLE, OutputFormat.CSV).getId();
        service.delete(deleted);
        byte[] before = dataFile();

        assertRejected(() -> service.delete(deleted));
        assertRejected(() -> service.delete(deleted + 1_000));

        assertArrayEquals(before, dataFile());
    }

    @Test
    void allMethods_signedOutAfterUse_authExceptionAndNothingChanged() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos").seed(workspace);
        AuthService auth = workspace.signIn(TestWorkspace.OWNER);
        ProjectService service = new ProjectService(workspace.store(), auth);
        service.list();
        auth.logout();
        byte[] before = dataFile();

        assertThrows(AuthException.class, () -> service.create("Tweets", null, TaxonomyKind.SINGLE,
                OutputFormat.CSV));
        assertThrows(AuthException.class, service::list);
        assertThrows(AuthException.class, () -> service.delete(flow.projectId()));
        assertThrows(AuthException.class, () -> service.delete(flow.projectId() + 1_000));

        assertArrayEquals(before, dataFile());
    }

    @Test
    void allMethods_annotator_authExceptionAndNothingChanged() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos").annotator("annotator").seed(workspace);
        ProjectService service = new ProjectService(workspace.store(), workspace.signIn("annotator"));
        byte[] before = dataFile();

        assertThrows(AuthException.class, () -> service.create("Tweets", null, TaxonomyKind.SINGLE,
                OutputFormat.CSV));
        assertThrows(AuthException.class, service::list);
        assertThrows(AuthException.class, () -> service.delete(flow.projectId()));

        assertArrayEquals(before, dataFile());
    }

    /** Returns a service for the owner, creating the owner first in a workspace that has none. */
    private ProjectService ownerService() {
        AuthService setup = new AuthService(workspace.store());
        if (setup.needsBootstrap()) {
            setup.bootstrapOwner(TestWorkspace.OWNER, TestWorkspace.PASSWORD);
        }
        return new ProjectService(workspace.store(), workspace.signIn(TestWorkspace.OWNER));
    }

    /** Adds a split holding one new item to a project and assigns it to this annotator, not started. */
    private void assignNewSplit(long projectId, long annotatorId) {
        ResolvedSource source = workspace.writeSource("later/item.txt", "A later synthetic item");
        workspace.store().write(session -> {
            long itemId = session.items().save(Records.item(projectId, source)).getId();
            Split split = Records.split(projectId, "Batch 2");
            split.setAnnotationsPerItem(1);
            split.setAssigned(true);
            long splitId = session.splits().save(split).getId();
            session.splitItems().save(Records.membership(splitId, itemId));
            Assignment assignment = Records.assignment(splitId, annotatorId);
            assignment.setStatus(AssignmentStatus.NOT_STARTED);
            session.assignments().save(assignment);
            return null;
        });
    }

    private static void assertRejected(Executable call) {
        ProjectException rejection = assertThrows(ProjectException.class, call);
        assertFalse(rejection.getMessage() == null || rejection.getMessage().isBlank());
    }

    private static Project project(JsonStore store, long projectId) {
        return store.read(session -> session.projects().findById(projectId)).orElseThrow();
    }

    private static TaxonomySettings settings(JsonStore store, long projectId) {
        return store.read(session -> session.taxonomySettings().findByProject(projectId)).orElseThrow();
    }

    private <T> T read(Function<RepositorySession, T> action) {
        return workspace.store().read(action);
    }

    private List<String> accounts() {
        return read(session -> session.users().listAll().stream()
                .map(user -> user.getId() + " " + user.getUsername() + " " + user.getRole() + " "
                        + user.getAccountStatus() + " " + user.getPasswordHash())
                .toList());
    }

    private byte[] dataFile() {
        try {
            return Files.readAllBytes(workspace.paths().dataFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map<Path, String> mediaFiles() {
        try (Stream<Path> files = Files.walk(workspace.paths().mediaDirectory())) {
            Map<Path, String> contents = new TreeMap<>();
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                contents.put(file, Files.readString(file));
            }
            return contents;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
