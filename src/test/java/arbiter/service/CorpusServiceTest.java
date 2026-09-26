package arbiter.service;

import static arbiter.service.ServiceAssertions.assertRejected;
import static arbiter.testing.FileLinks.createDirectoryLink;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.project.Item;
import arbiter.model.project.OutputFormat;
import arbiter.model.project.Split;
import arbiter.model.project.SplitItem;
import arbiter.model.project.TaxonomyKind;
import arbiter.testing.ClassificationWorkflow;
import arbiter.testing.TestWorkspace;
import arbiter.workspace.SourceException;
import arbiter.workspace.SourceFailure;
import arbiter.workspace.SourceResolver;

/**
 * Integration checks for listing, registering and unregistering a project's items (#25), and for generating,
 * listing and deleting its splits (#28).
 */
class CorpusServiceTest {
    @TempDir
    Path temporary;

    private TestWorkspace workspace;
    private int sourceCount;

    @BeforeEach
    void createWorkspace() {
        workspace = TestWorkspace.create(temporary.resolve("workspace"));
    }

    @Test
    void list_projectWithoutItems_emptyList() {
        long projectId = newProject("Tweets");

        assertEquals(List.of(), ownerService().list(projectId));
    }

    @Test
    void list_unknownProject_emptyList() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos").seed(workspace);

        assertEquals(List.of(), ownerService().list(flow.projectId() + 1_000));
    }

    @Test
    void list_itemsInSeveralProjects_onlyThatProjectsItemsInRegistrationOrder() {
        long first = newProject("First");
        long second = newProject("Second");
        CorpusService service = ownerService();
        Path zebra = source("reviews/zebra.txt", "Zebra review");
        Path apple = source("reviews/apple.txt", "Apple review");
        Path mango = source("other/mango.txt", "Mango review");
        Path kiwi = source("reviews/kiwi.txt", "Kiwi review");

        service.register(first, List.of(zebra, apple));
        service.register(second, List.of(mango));
        service.register(first, List.of(kiwi));

        assertEquals(List.of("media/reviews/zebra.txt", "media/reviews/apple.txt", "media/reviews/kiwi.txt"),
                storedPaths(service.list(first)));
        assertEquals(List.of("media/other/mango.txt"), storedPaths(service.list(second)));
    }

    @Test
    void register_nestedFiles_storedWithMediaPathsHashesAndOneImportTimeAcrossReopen() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        Path deep = source("reviews/2026/september/first.txt", "The first review");
        Path shallow = source("second.txt", "The second review");
        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        List<Item> returned = service.register(projectId, List.of(deep, shallow));

        Instant after = Instant.now();
        assertEquals(List.of("media/reviews/2026/september/first.txt", "media/second.txt"), storedPaths(returned));
        assertEquals(hash("The first review"), returned.get(0).getContentHash());
        assertEquals(hash("The second review"), returned.get(1).getContentHash());
        Instant importedAt = returned.get(0).getImportedAt();
        assertEquals(importedAt, returned.get(1).getImportedAt());
        assertFalse(importedAt.isBefore(before));
        assertFalse(importedAt.isAfter(after));
        for (Item item : returned) {
            assertEquals(projectId, item.getProjectId());
        }
        JsonStore reopened = JsonStore.open(workspace.paths());
        List<Item> stored = serviceOn(reopened).list(projectId);
        assertEquals(describe(returned), describe(stored));
        SourceResolver resolver = new SourceResolver(workspace.paths());
        assertEquals("The first review", resolver.resolve(stored.get(0).getPath(),
                stored.get(0).getContentHash()).text());
        assertEquals("The second review", resolver.resolve(stored.get(1).getPath(),
                stored.get(1).getContentHash()).text());
    }

    @Test
    void register_emptySelection_emptyListAndNothingStored() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        byte[] before = workspace.dataFileBytes();

        assertEquals(List.of(), service.register(projectId, List.of()));

        assertArrayEquals(before, workspace.dataFileBytes());
        assertEquals(List.of(), service.list(projectId));
    }

    @Test
    void register_projectWithNeverAssignedSplit_accepted() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos").items(2).annotator("spare").seed(workspace);
        CorpusService service = ownerService();

        Item added = service.register(flow.projectId(), List.of(source("added.txt", "An added review"))).getFirst();

        assertEquals(List.of(flow.itemId(0), flow.itemId(1), added.getId()), ids(service.list(flow.projectId())));
    }

    @Test
    void register_sameFileInSecondProject_accepted() {
        long first = newProject("First");
        long second = newProject("Second");
        CorpusService service = ownerService();
        Path shared = source("shared.txt", "A shared review");

        Item firstItem = service.register(first, List.of(shared)).getFirst();
        Item secondItem = service.register(second, List.of(shared)).getFirst();

        assertNotEquals(firstItem.getId(), secondItem.getId());
        assertEquals(second, secondItem.getProjectId());
        assertEquals("media/shared.txt", secondItem.getPath());
        assertEquals(firstItem.getContentHash(), secondItem.getContentHash());
        assertEquals(List.of(firstItem.getId()), ids(service.list(first)));
        assertEquals(List.of(secondItem.getId()), ids(service.list(second)));
    }

    @Test
    void register_pathChangedSinceAnotherProjectRegisteredIt_acceptedWithNewHash() throws IOException {
        long first = newProject("First");
        long second = newProject("Second");
        CorpusService service = ownerService();
        Path shared = source("shared.txt", "The original review");
        service.register(first, List.of(shared));
        Files.writeString(shared, "The edited review");

        Item item = service.register(second, List.of(shared)).getFirst();

        assertEquals("media/shared.txt", item.getPath());
        assertEquals(hash("The edited review"), item.getContentHash());
    }

    @Test
    void register_fileUnregisteredEarlier_accepted() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        Path review = source("review.txt", "A review");
        long removed = service.register(projectId, List.of(review)).getFirst().getId();
        service.unregister(removed);

        Item again = service.register(projectId, List.of(review)).getFirst();

        assertNotEquals(removed, again.getId());
        assertEquals(List.of(again.getId()), ids(service.list(projectId)));
    }

    @Test
    void register_nameDifferingOnlyInCaseFromRegisteredOne_acceptedAndEachReadsItsOwnFile() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        Path lower = source("reviews/test.txt", "The lower-case review");
        assumeTrue(!Files.exists(lower.resolveSibling("TEST.txt")), "case-insensitive file system");
        Path upper = source("reviews/TEST.txt", "The upper-case review");
        service.register(projectId, List.of(lower));

        service.register(projectId, List.of(upper));

        List<Item> stored = service.list(projectId);
        assertEquals(List.of("media/reviews/test.txt", "media/reviews/TEST.txt"), storedPaths(stored));
        SourceResolver resolver = new SourceResolver(workspace.paths());
        assertEquals("The lower-case review", resolver.resolve(stored.get(0).getPath(),
                stored.get(0).getContentHash()).text());
        assertEquals("The upper-case review", resolver.resolve(stored.get(1).getPath(),
                stored.get(1).getContentHash()).text());
    }

    @Test
    void register_lastFileOutsideWorkspace_sourceExceptionAndNothingStored() {
        Path outside = file(temporary.resolve("elsewhere/outside.txt"), text("An outside review"));

        assertLastFileFailsSet(outside, SourceFailure.OUTSIDE_MEDIA, "outside.txt");
    }

    @Test
    void register_lastFileTraversesOutOfMedia_sourceExceptionAndNothingStored() {
        file(workspace.paths().root().resolve("outside.txt"), text("A review beside media"));
        Path traversal = workspace.paths().mediaDirectory().resolve("..").resolve("outside.txt");

        assertLastFileFailsSet(traversal, SourceFailure.OUTSIDE_MEDIA, "outside.txt");
    }

    @Test
    void register_lastFileThroughLinkLeavingMedia_sourceExceptionAndNothingStored() {
        file(workspace.paths().root().resolve("outside/linked.txt"), text("A linked review"));
        Path link = workspace.paths().mediaDirectory().resolve("joined");
        assumeTrue(createDirectoryLink(link, workspace.paths().root().resolve("outside")), "links unavailable");

        assertLastFileFailsSet(link.resolve("linked.txt"), SourceFailure.OUTSIDE_MEDIA, "media/joined/linked.txt");
    }

    @Test
    void register_lastFileMissing_sourceExceptionAndNothingStored() {
        Path missing = workspace.paths().mediaDirectory().resolve("missing.txt");

        assertLastFileFailsSet(missing, SourceFailure.MISSING, "media/missing.txt");
    }

    @Test
    void register_lastFileNotTxt_sourceExceptionAndNothingStored() {
        Path pdf = file(workspace.paths().mediaDirectory().resolve("review.pdf"), text("Not plain text"));

        assertLastFileFailsSet(pdf, SourceFailure.NOT_TEXT, "media/review.pdf");
    }

    @Test
    void register_lastFileInvalidUtf8_sourceExceptionAndNothingStored() {
        Path broken = file(workspace.paths().mediaDirectory().resolve("broken.txt"), new byte[] {(byte) 0x80, 'a'});

        assertLastFileFailsSet(broken, SourceFailure.INVALID_TEXT, "media/broken.txt");
    }

    @Test
    void register_registeredPathWithChangedBytes_rejectedAndNothingStored() throws IOException {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        Path review = source("review.txt", "The original review");
        service.register(projectId, List.of(review));
        Files.writeString(review, "The edited review");
        Path fresh = source("fresh.txt", "A fresh review");
        byte[] before = workspace.dataFileBytes();

        ProjectException rejection = assertThrows(ProjectException.class, () ->
                service.register(projectId, List.of(fresh, review)));

        assertMessageNames(rejection, "media/review.txt", "changed");
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void register_sameFileAgain_rejectedAndNothingStored() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        Path review = source("review.txt", "A review");
        service.register(projectId, List.of(review));
        Path fresh = source("fresh.txt", "A fresh review");
        byte[] before = workspace.dataFileBytes();

        ProjectException rejection = assertThrows(ProjectException.class, () ->
                service.register(projectId, List.of(fresh, review)));

        assertMessageNames(rejection, "media/review.txt", "already registered");
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void register_copyOfRegisteredFileAtAnotherPath_rejectedAndNothingStored() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        service.register(projectId, List.of(source("original.txt", "A review")));
        Path fresh = source("fresh.txt", "A fresh review");
        Path copy = source("copies/copy.txt", "A review");
        byte[] before = workspace.dataFileBytes();

        ProjectException rejection = assertThrows(ProjectException.class, () ->
                service.register(projectId, List.of(fresh, copy)));

        assertMessageNames(rejection, "media/copies/copy.txt", "media/original.txt");
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void register_identicalFilesInOneSelection_rejectedAndNothingStored() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        Path first = source("first.txt", "A repeated review");
        Path fresh = source("fresh.txt", "A fresh review");
        Path second = source("second.txt", "A repeated review");
        byte[] before = workspace.dataFileBytes();

        ProjectException rejection = assertThrows(ProjectException.class, () ->
                service.register(projectId, List.of(first, fresh, second)));

        assertMessageNames(rejection, "media/second.txt", "media/first.txt");
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void register_sameFileTwiceInOneSelection_rejectedAndNothingStored() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        Path review = source("review.txt", "A review");
        byte[] before = workspace.dataFileBytes();

        ProjectException rejection = assertThrows(ProjectException.class, () ->
                service.register(projectId, List.of(review, review)));

        assertEquals("media/review.txt is selected more than once", rejection.getMessage());
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void register_identicalFilesWithNamesDifferingOnlyInCase_rejectedAndNothingStored() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        Path lower = source("test.txt", "A repeated review");
        assumeTrue(!Files.exists(lower.resolveSibling("TEST.txt")), "case-insensitive file system");
        Path upper = source("TEST.txt", "A repeated review");
        byte[] before = workspace.dataFileBytes();

        ProjectException rejection = assertThrows(ProjectException.class, () ->
                service.register(projectId, List.of(lower, upper)));

        assertEquals("media/TEST.txt has the same content as media/test.txt, which is also selected",
                rejection.getMessage());
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void register_twoFailingFiles_firstFailureReported() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        service.register(projectId, List.of(source("original.txt", "A review")));
        Path copy = source("copy.txt", "A review");
        Path missing = workspace.paths().mediaDirectory().resolve("missing.txt");
        byte[] before = workspace.dataFileBytes();

        ProjectException duplicate = assertThrows(ProjectException.class, () ->
                service.register(projectId, List.of(copy, missing)));
        SourceException absent = assertThrows(SourceException.class, () ->
                service.register(projectId, List.of(missing, copy)));

        assertMessageNames(duplicate, "media/copy.txt");
        assertEquals(SourceFailure.MISSING, absent.reason());
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void register_missingProject_rejectedAndNothingStored() {
        long deleted = newProject("Tweets");
        projectService().delete(deleted);
        CorpusService service = ownerService();
        Path review = source("review.txt", "A review");
        byte[] before = workspace.dataFileBytes();

        assertRejected(() -> service.register(deleted, List.of(review)));
        assertRejected(() -> service.register(deleted + 1_000, List.of(review)));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void unregister_itemInNeverAssignedSplit_itemAndMembershipRemovedRestKept() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos", "neg").items(3)
                .annotator("spare")
                .seed(workspace);
        ClassificationWorkflow other = ClassificationWorkflow.single("yes", "no").items(2).seed(workspace);
        CorpusService service = ownerService();
        long removed = flow.itemId(1);
        String removedPath = read(session -> session.items().findById(removed)).orElseThrow().getPath();
        List<String> keptItems = describe(service.list(flow.projectId()).stream()
                .filter(item -> item.getId() != removed).toList());
        List<String> keptMemberships = memberships(flow.splitId()).stream()
                .filter(membership -> !membership.startsWith(removed + "@")).toList();
        List<String> labels = labels(flow.projectId());
        List<String> otherItems = describe(service.list(other.projectId()));
        List<String> otherMemberships = memberships(other.splitId());
        List<String> otherLabels = labels(other.projectId());
        Map<String, String> mediaBefore = mediaFiles();

        service.unregister(removed);

        JsonStore reopened = JsonStore.open(workspace.paths());
        assertTrue(reopened.<Boolean>read(session -> session.items().findById(removed).isEmpty()));
        assertTrue(reopened.<Boolean>read(session -> session.splitItems().findByItem(removed).isEmpty()));
        assertTrue(reopened.<Boolean>read(session -> session.splits().findById(flow.splitId()).isPresent()));
        assertEquals(keptItems, describe(serviceOn(reopened).list(flow.projectId())));
        assertEquals(keptMemberships, memberships(flow.splitId()));
        assertEquals(labels, labels(flow.projectId()));
        assertEquals(otherItems, describe(service.list(other.projectId())));
        assertEquals(otherMemberships, memberships(other.splitId()));
        assertEquals(otherLabels, labels(other.projectId()));
        assertEquals(mediaBefore, mediaFiles());
        assertTrue(Files.isRegularFile(workspace.paths().root().resolve(removedPath)));
    }

    @Test
    void unregister_itemAlreadyRemovedOrUnknown_rejectedAndNothingChanged() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos").items(2).seed(workspace);
        CorpusService service = ownerService();
        service.unregister(flow.itemId(0));
        byte[] before = workspace.dataFileBytes();

        assertRejected(() -> service.unregister(flow.itemId(0)));
        assertRejected(() -> service.unregister(flow.itemId(1) + 1_000));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void corpusWrites_registerThenUnregister_mediaFilesUnchanged() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        Path first = source("reviews/first.txt", "The first review");
        Path second = source("reviews/nested/second.txt", "The second review");
        Path third = source("third.txt", "The third review");
        Map<String, String> mediaBefore = mediaFiles();

        List<Item> items = service.register(projectId, List.of(first, second, third));
        assertEquals(mediaBefore, mediaFiles());
        service.unregister(items.get(1).getId());

        assertEquals(mediaBefore, mediaFiles());
        assertEquals(List.of("media/reviews/first.txt", "media/third.txt"), storedPaths(service.list(projectId)));
    }

    @Test
    void corpusWrites_notStartedAssignment_rejectedAndNothingChanged() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos", "neg").items(2).assign("annotator")
                .seed(workspace);
        CorpusService service = ownerService();
        Path added = source("added.txt", "An added review");
        byte[] before = workspace.dataFileBytes();

        assertRejected(() -> service.register(flow.projectId(), List.of(added)));
        assertRejected(() -> service.unregister(flow.itemId(0)));
        assertRejected(() -> service.deleteSplit(flow.splitId()));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void corpusWrites_assignedAnnotatorDisabled_rejectedAndNothingChanged() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos", "neg").items(2)
                .assign("annotator", "pos")
                .disabled("annotator")
                .seed(workspace);
        CorpusService service = ownerService();
        Path added = source("added.txt", "An added review");
        byte[] before = workspace.dataFileBytes();

        assertRejected(() -> service.register(flow.projectId(), List.of(added)));
        assertRejected(() -> service.unregister(flow.itemId(1)));
        assertRejected(() -> service.deleteSplit(flow.splitId()));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void corpusWrites_assignedProjectAfterRestart_rejectedAndNothingChanged() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos", "neg").items(2).assign("annotator")
                .seed(workspace);
        Path added = source("added.txt", "An added review");
        byte[] before = workspace.dataFileBytes();
        CorpusService service = serviceOn(JsonStore.open(workspace.paths()));

        assertRejected(() -> service.register(flow.projectId(), List.of(added)));
        assertRejected(() -> service.unregister(flow.itemId(0)));
        assertRejected(() -> service.deleteSplit(flow.splitId()));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void corpusWrites_assignmentAfterListRead_rejectedAndNothingChanged() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos").items(2)
                .annotator("annotator")
                .seed(workspace);
        CorpusService service = ownerService();
        List<Item> stale = service.list(flow.projectId());
        assertEquals(2, stale.size());
        workspace.assignNewSplit(flow.projectId(), flow.annotatorId("annotator"));
        Path added = source("added.txt", "An added review");
        byte[] before = workspace.dataFileBytes();

        assertRejected(() -> service.register(flow.projectId(), List.of(added)));
        assertRejected(() -> service.unregister(stale.getFirst().getId()));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void corpusWrites_otherProjectAssigned_acceptedAndOtherProjectUnchanged() {
        ClassificationWorkflow assigned = ClassificationWorkflow.single("pos", "neg").items(2).assign("annotator")
                .seed(workspace);
        ClassificationWorkflow open = ClassificationWorkflow.single("pos").items(2).seed(workspace);
        CorpusService service = ownerService();
        List<String> assignedItems = describe(service.list(assigned.projectId()));
        List<String> assignedMemberships = memberships(assigned.splitId());
        Path added = source("added.txt", "An added review");

        Item item = service.register(open.projectId(), List.of(added)).getFirst();
        service.unregister(open.itemId(0));

        assertEquals(List.of(open.itemId(1), item.getId()), ids(service.list(open.projectId())));
        assertEquals(assignedItems, describe(service.list(assigned.projectId())));
        assertEquals(assignedMemberships, memberships(assigned.splitId()));
    }

    @Test
    void allocate_sameIdsCountAndSeed_sameShuffledSplits() {
        List<Long> ids = LongStream.rangeClosed(1, 20).boxed().toList();

        List<List<Long>> splits = CorpusService.allocate(ids, 6, 42);

        assertEquals(splits, CorpusService.allocate(ids, 6, 42));
        assertEquals(List.of(6, 6, 6, 2), splits.stream().map(List::size).toList());
        List<Long> order = splits.stream().flatMap(List::stream).toList();
        assertEquals(ids, order.stream().sorted().toList());
        assertNotEquals(ids, order);
        assertNotEquals(splits, CorpusService.allocate(ids, 6, 43));
    }

    @Test
    void previewSplits_validCounts_sizesInOrderAndNothingStored() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        registerItems(service, projectId, 5);
        Map<String, List<Integer>> expected = Map.of(
                "1", List.of(1, 1, 1, 1, 1),
                "2", List.of(2, 2, 1),
                "4", List.of(4, 1),
                "5", List.of(5),
                " 3 ", List.of(3, 2),
                "03", List.of(3, 2),
                "000000000000000000003", List.of(3, 2));
        byte[] before = workspace.dataFileBytes();

        expected.forEach((count, sizes) -> assertEquals(sizes, service.previewSplits(projectId, count), count));

        assertArrayEquals(before, workspace.dataFileBytes());
        assertEquals(List.of(), service.listSplits(projectId));
    }

    @Test
    void previewAndGenerate_invalidCount_rejectedAndNothingStored() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        registerItems(service, projectId, 3);
        // U+0663 is the Arabic-Indic digit three.
        List<String> counts = Arrays.asList(null, "", "   ", "0", "-1", "2.5", "abc", "٣");
        byte[] before = workspace.dataFileBytes();

        for (String count : counts) {
            assertMessageNames(assertRejected(() -> service.previewSplits(projectId, count)),
                    "positive whole number");
            assertMessageNames(assertRejected(() -> service.generateSplits(projectId, count, List.of(3))),
                    "positive whole number");
        }

        assertArrayEquals(before, workspace.dataFileBytes());
        assertEquals(List.of(), service.listSplits(projectId));
    }

    @Test
    void previewAndGenerate_countAboveAvailableItems_rejectedAndNothingStored() {
        long projectId = newProject("Tweets");
        long singleId = newProject("Single");
        CorpusService service = ownerService();
        registerItems(service, projectId, 2);
        generate(service, projectId, "2");
        registerItems(service, projectId, 3);
        registerItems(service, singleId, 1);
        byte[] before = workspace.dataFileBytes();

        // 4 is more than the 3 available items but not the 5 registered ones.
        for (String count : List.of("4", "2147483647", "2147483648", "99999999999999999999")) {
            assertMessageNames(assertRejected(() -> service.previewSplits(projectId, count)),
                    "Only 3 files are available");
            assertMessageNames(assertRejected(() -> service.generateSplits(projectId, count, List.of(3))),
                    "Only 3 files are available");
        }
        assertMessageNames(assertRejected(() -> service.previewSplits(singleId, "2")), "Only 1 file is available");
        assertMessageNames(assertRejected(() -> service.generateSplits(singleId, "2", List.of(1))),
                "Only 1 file is available");

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void previewAndGenerate_missingProjectOrNoAvailableItems_rejectedAndNothingStored() {
        long deleted = newProject("Deleted");
        projectService().delete(deleted);
        long empty = newProject("Empty");
        ClassificationWorkflow allInSplit = ClassificationWorkflow.single("pos").items(2).seed(workspace);
        CorpusService service = ownerService();
        Map<Long, String> reasons = Map.of(
                deleted, "no longer exists",
                deleted + 1_000, "no longer exists",
                empty, "no registered files outside a split",
                allInSplit.projectId(), "no registered files outside a split");
        byte[] before = workspace.dataFileBytes();

        reasons.forEach((projectId, reason) -> {
            assertMessageNames(assertRejected(() -> service.previewSplits(projectId, "1")), reason);
            assertMessageNames(assertRejected(() -> service.generateSplits(projectId, "1", List.of())), reason);
        });

        assertArrayEquals(before, workspace.dataFileBytes());
        assertEquals(List.of(), service.listSplits(empty));
    }

    @Test
    void generateSplits_count50On120Items_splitsOf50And50And20HoldingEachItemOnce() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        List<Long> itemIds = registerItems(service, projectId, 120);
        List<Integer> preview = service.previewSplits(projectId, "50");
        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        List<Split> generated = service.generateSplits(projectId, "50", preview);

        Instant after = Instant.now();
        assertEquals(List.of(50, 50, 20), preview);
        List<SplitSummary> splits = service.listSplits(projectId);
        assertEquals(splitIds(generated), splits.stream().map(SplitSummary::id).toList());
        assertEquals(List.of("Split 1", "Split 2", "Split 3"), splits.stream().map(SplitSummary::name).toList());
        assertEquals(preview, splits.stream().map(split -> split.itemIds().size()).toList());
        assertEquals(itemIds, splits.stream().flatMap(split -> split.itemIds().stream()).sorted().toList());
        for (SplitSummary split : splits) {
            assertEquals(IntStream.rangeClosed(1, split.itemIds().size()).boxed().toList(), sequences(split.id()));
            assertFalse(split.assigned());
        }
        Split first = generated.getFirst();
        assertFalse(first.getCreatedAt().isBefore(before));
        assertFalse(first.getCreatedAt().isAfter(after));
        for (Split stored : read(session -> session.splits().listByProject(projectId))) {
            assertEquals(first.getSeed(), stored.getSeed());
            assertEquals(50, stored.getRequestedBatchSize());
            assertNull(stored.getAnnotationsPerItem());
            assertEquals(first.getCreatedAt(), stored.getCreatedAt());
        }
    }

    @Test
    void generateSplits_laterGeneration_onlyAvailableItemsAsRecordedSeedAllocates() {
        long projectId = newProject("Tweets");
        long otherId = newProject("Other");
        CorpusService service = ownerService();
        registerItems(service, otherId, 2);
        generate(service, otherId, "1");
        registerItems(service, projectId, 10);
        generate(service, projectId, "1");
        List<SplitSummary> earlier = service.listSplits(projectId);
        List<SplitSummary> other = service.listSplits(otherId);
        List<Long> added = registerItems(service, projectId, 3);

        List<Split> generated = generate(service, projectId, "2");

        assertEquals("Split 1", earlier.getFirst().name());
        List<SplitSummary> splits = service.listSplits(projectId);
        assertEquals(earlier, splits.subList(0, 10));
        List<SplitSummary> later = splits.subList(10, splits.size());
        assertEquals(splitIds(generated), later.stream().map(SplitSummary::id).toList());
        assertEquals(List.of("Split 11", "Split 12"), later.stream().map(SplitSummary::name).toList());
        assertEquals(CorpusService.allocate(added, 2, generated.getFirst().getSeed()),
                later.stream().map(SplitSummary::itemIds).toList());
        assertEquals(other, service.listSplits(otherId));
    }

    @Test
    void generateSplits_itemRegisteredOrUnregisteredSincePreview_rejectedAndNothingStored() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        long firstItem = registerItems(service, projectId, 4).getFirst();
        List<Integer> preview = service.previewSplits(projectId, "2");
        long added = registerItems(service, projectId, 1).getFirst();
        byte[] withAdded = workspace.dataFileBytes();

        assertMessageNames(assertRejected(() -> service.generateSplits(projectId, "2", preview)),
                "changed since the preview");

        assertArrayEquals(withAdded, workspace.dataFileBytes());
        service.unregister(added);
        service.unregister(firstItem);
        byte[] withRemoved = workspace.dataFileBytes();

        assertMessageNames(assertRejected(() -> service.generateSplits(projectId, "2", preview)),
                "changed since the preview");

        assertArrayEquals(withRemoved, workspace.dataFileBytes());
        assertEquals(List.of(), service.listSplits(projectId));
    }

    @Test
    void listSplits_afterAssignmentAndRestart_savedMembershipAndOrder() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        registerItems(service, projectId, 7);
        generate(service, projectId, "3");
        List<SplitSummary> generated = service.listSplits(projectId);
        long assignedId = generated.get(1).id();

        workspace.assignSplit(assignedId, "annotator");

        List<SplitSummary> expected = generated.stream()
                .map(split -> new SplitSummary(split.id(), split.name(), split.itemIds(),
                        split.id() == assignedId ? 1 : 0, split.id() == assignedId ? 1 : null))
                .toList();
        assertEquals(expected, service.listSplits(projectId));
        assertEquals(expected, serviceOn(JsonStore.open(workspace.paths())).listSplits(projectId));
    }

    @Test
    void deleteSplit_neverAssigned_splitAndMembershipsRemovedAndItemsRegenerated() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        List<Long> itemIds = registerItems(service, projectId, 5);
        generate(service, projectId, "2");
        List<SplitSummary> splits = service.listSplits(projectId);
        SplitSummary deleted = splits.getLast();

        service.deleteSplit(deleted.id());

        assertEquals(splits.subList(0, 2), service.listSplits(projectId));
        assertTrue(this.<Boolean>read(session -> session.splits().findById(deleted.id()).isEmpty()));
        assertEquals(List.of(), memberships(deleted.id()));
        assertEquals(itemIds, ids(service.list(projectId)));
        assertEquals(List.of(1), service.previewSplits(projectId, "1"));
        Split regenerated = generate(service, projectId, "1").getFirst();
        assertEquals("Split 3", regenerated.getName());
        assertEquals(deleted.itemIds(), service.listSplits(projectId).getLast().itemIds());
    }

    @Test
    void deleteSplit_neverAssignedAfterAnotherSplitsAssignment_deletedAndItemsRegenerated() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        registerItems(service, projectId, 5);
        generate(service, projectId, "2");
        List<SplitSummary> splits = service.listSplits(projectId);
        SplitSummary assigned = splits.get(0);
        SplitSummary deleted = splits.get(1);
        workspace.assignSplit(assigned.id(), "annotator");

        service.deleteSplit(deleted.id());
        generate(service, projectId, "2");

        List<SplitSummary> after = service.listSplits(projectId);
        assertEquals(3, after.size());
        assertEquals(new SplitSummary(assigned.id(), assigned.name(), assigned.itemIds(), 1, 1), after.get(0));
        assertEquals(splits.get(2), after.get(1));
        assertEquals("Split 4", after.get(2).name());
        assertEquals(deleted.itemIds().stream().sorted().toList(), after.get(2).itemIds().stream().sorted().toList());
    }

    @Test
    void deleteSplit_assignmentAfterListRead_rejectedAndNothingChanged() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        registerItems(service, projectId, 3);
        generate(service, projectId, "2");
        List<SplitSummary> stale = service.listSplits(projectId);
        assertFalse(stale.getFirst().assigned());
        workspace.assignSplit(stale.getFirst().id(), "annotator");
        byte[] before = workspace.dataFileBytes();

        assertRejected(() -> service.deleteSplit(stale.getFirst().id()));
        assertRejected(() -> service.unregister(stale.getFirst().itemIds().getFirst()));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void deleteSplit_alreadyDeletedOrUnknown_rejectedAndNothingChanged() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        registerItems(service, projectId, 1);
        long splitId = generate(service, projectId, "1").getFirst().getId();
        service.deleteSplit(splitId);
        byte[] before = workspace.dataFileBytes();

        assertRejected(() -> service.deleteSplit(splitId));
        assertRejected(() -> service.deleteSplit(splitId + 1_000));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void unregister_itemsOfGeneratedSplits_membershipsRemovedAndEmptiedSplitDeleted() {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        registerItems(service, projectId, 3);
        generate(service, projectId, "2");
        List<SplitSummary> splits = service.listSplits(projectId);
        SplitSummary pair = splits.get(0);
        SplitSummary single = splits.get(1);

        service.unregister(pair.itemIds().getFirst());
        service.unregister(single.itemIds().getFirst());

        List<SplitSummary> expected = List.of(
                new SplitSummary(pair.id(), pair.name(), pair.itemIds().subList(1, 2), 0, null));
        assertEquals(expected, service.listSplits(projectId));
        assertEquals(List.of(), memberships(single.id()));
        JsonStore reopened = JsonStore.open(workspace.paths());
        assertTrue(reopened.<Boolean>read(session -> session.splits().findById(single.id()).isEmpty()));
        assertEquals(expected, serviceOn(reopened).listSplits(projectId));
    }

    @Test
    void allMethods_signedOutAfterUse_authExceptionAndNothingChanged() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos").seed(workspace);
        AuthService auth = workspace.signIn(TestWorkspace.OWNER);
        CorpusService service = new CorpusService(workspace.store(), auth, workspace.paths());
        service.list(flow.projectId());
        auth.logout();
        Path added = source("added.txt", "An added review");
        Path missing = workspace.paths().mediaDirectory().resolve("missing.txt");
        byte[] before = workspace.dataFileBytes();

        assertThrows(AuthException.class, () -> service.list(flow.projectId()));
        assertThrows(AuthException.class, () -> service.register(flow.projectId(), List.of(added)));
        assertThrows(AuthException.class, () -> service.register(flow.projectId(), List.of(missing)));
        assertThrows(AuthException.class, () -> service.register(flow.projectId() + 1_000, List.of(added)));
        assertThrows(AuthException.class, () -> service.unregister(flow.itemId(0)));
        assertThrows(AuthException.class, () -> service.unregister(flow.itemId(0) + 1_000));
        assertThrows(AuthException.class, () -> service.listSplits(flow.projectId()));
        assertThrows(AuthException.class, () -> service.previewSplits(flow.projectId(), "1"));
        assertThrows(AuthException.class, () -> service.previewSplits(flow.projectId(), "0"));
        assertThrows(AuthException.class, () -> service.generateSplits(flow.projectId(), "1", List.of(1)));
        assertThrows(AuthException.class, () -> service.generateSplits(flow.projectId(), "0", List.of(1)));
        assertThrows(AuthException.class, () -> service.deleteSplit(flow.splitId()));
        assertThrows(AuthException.class, () -> service.deleteSplit(flow.splitId() + 1_000));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void allMethods_annotator_authExceptionAndNothingChanged() {
        ClassificationWorkflow flow = ClassificationWorkflow.single("pos").annotator("annotator").seed(workspace);
        CorpusService service = new CorpusService(workspace.store(), workspace.signIn("annotator"),
                workspace.paths());
        Path added = source("added.txt", "An added review");
        byte[] before = workspace.dataFileBytes();

        assertThrows(AuthException.class, () -> service.list(flow.projectId()));
        assertThrows(AuthException.class, () -> service.register(flow.projectId(), List.of(added)));
        assertThrows(AuthException.class, () -> service.unregister(flow.itemId(0)));
        assertThrows(AuthException.class, () -> service.listSplits(flow.projectId()));
        assertThrows(AuthException.class, () -> service.previewSplits(flow.projectId(), "1"));
        assertThrows(AuthException.class, () -> service.generateSplits(flow.projectId(), "1", List.of(1)));
        assertThrows(AuthException.class, () -> service.deleteSplit(flow.splitId()));

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    /**
     * Registers two valid files followed by {@code last} in a new project, and checks that the whole set
     * fails for this reason, names the file and leaves {@code arbiter.json} unchanged byte for byte.
     */
    private void assertLastFileFailsSet(Path last, SourceFailure reason, String named) {
        long projectId = newProject("Tweets");
        CorpusService service = ownerService();
        List<Path> files = List.of(source("valid/first.txt", "The first valid review"),
                source("valid/second.txt", "The second valid review"), last);
        byte[] before = workspace.dataFileBytes();

        SourceException failure = assertThrows(SourceException.class, () -> service.register(projectId, files));

        assertEquals(reason, failure.reason());
        assertTrue(failure.getMessage().contains(named), failure.getMessage());
        assertArrayEquals(before, workspace.dataFileBytes());
        assertEquals(List.of(), service.list(projectId));
    }

    private static void assertMessageNames(ProjectException rejection, String... parts) {
        for (String part : parts) {
            assertTrue(rejection.getMessage().contains(part), rejection.getMessage());
        }
    }

    /** Returns a service for the owner. */
    private CorpusService ownerService() {
        return new CorpusService(workspace.store(), workspace.signInOwner(), workspace.paths());
    }

    /** Returns a service for the owner on this store, as after a restart. */
    private CorpusService serviceOn(JsonStore store) {
        AuthService auth = new AuthService(store);
        auth.login(TestWorkspace.OWNER, TestWorkspace.PASSWORD);
        return new CorpusService(store, auth, workspace.paths());
    }

    private ProjectService projectService() {
        return new ProjectService(workspace.store(), workspace.signInOwner());
    }

    /** Creates an empty project through the service the adjudicator uses. */
    private long newProject(String name) {
        return projectService().create(name, null, TaxonomyKind.SINGLE, OutputFormat.CSV).getId();
    }

    /** Registers this many new text files in a project and returns their identifiers in registration order. */
    private List<Long> registerItems(CorpusService service, long projectId, int count) {
        List<Path> files = new ArrayList<>();
        for (int file = 0; file < count; file++) {
            sourceCount++;
            files.add(source("items/item-" + sourceCount + ".txt", "Item " + sourceCount));
        }
        return ids(service.register(projectId, files));
    }

    /** Generates splits of this many items with the sizes its preview shows, as confirming the preview does. */
    private static List<Split> generate(CorpusService service, long projectId, String itemsPerSplit) {
        return service.generateSplits(projectId, itemsPerSplit, service.previewSplits(projectId, itemsPerSplit));
    }

    /** Writes a UTF-8 text file below {@code media/} and returns it as the chooser would. */
    private Path source(String belowMedia, String text) {
        return workspace.paths().root().resolve(workspace.writeSource(belowMedia, text).storedPath());
    }

    private static Path file(Path file, byte[] bytes) {
        try {
            Files.createDirectories(file.getParent());
            return Files.write(file, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] text(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String hash(String text) {
        return SourceResolver.hash(text(text));
    }

    private static List<String> storedPaths(List<Item> items) {
        return items.stream().map(Item::getPath).toList();
    }

    private static List<Long> ids(List<Item> items) {
        return items.stream().map(Item::getId).toList();
    }

    private static List<String> describe(List<Item> items) {
        return items.stream().map(item -> item.getId() + " " + item.getProjectId() + " " + item.getPath() + " "
                + item.getContentHash() + " " + item.getImportedAt()).toList();
    }

    private <T> T read(Function<RepositorySession, T> action) {
        return workspace.store().read(action);
    }

    /** Returns a split's memberships as {@code itemId@sequence}, in stored order. */
    private List<String> memberships(long splitId) {
        return read(session -> session.splitItems().listBySplit(splitId).stream()
                .map(membership -> membership.getItemId() + "@" + membership.getSequence())
                .toList());
    }

    /** Returns the positions of a split's memberships, in stored order. */
    private List<Integer> sequences(long splitId) {
        return read(session -> session.splitItems().listBySplit(splitId).stream()
                .map(SplitItem::getSequence)
                .toList());
    }

    private static List<Long> splitIds(List<Split> splits) {
        return splits.stream().map(Split::getId).toList();
    }

    private List<String> labels(long projectId) {
        return read(session -> session.labels().listByProject(projectId).stream()
                .map(label -> label.getId() + " " + label.getKey() + " " + label.getSequence() + " "
                        + label.getDescription())
                .toList());
    }

    /** Returns every file under {@code media/} with the hash of its bytes. */
    private Map<String, String> mediaFiles() {
        try (Stream<Path> files = Files.walk(workspace.paths().mediaDirectory())) {
            Map<String, String> contents = new TreeMap<>();
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                contents.put(file.toString(), SourceResolver.hash(Files.readAllBytes(file)));
            }
            return contents;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
