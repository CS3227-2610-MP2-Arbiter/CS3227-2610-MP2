package arbiter.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.model.annotation.Annotation;
import arbiter.model.project.OutputFormat;
import arbiter.model.project.Project;
import arbiter.model.project.Split;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.resolution.Resolution;
import arbiter.testing.ClassificationWorkflow;
import arbiter.testing.Records;
import arbiter.testing.TestWorkspace;
import arbiter.workspace.ResolvedSource;
import arbiter.workspace.SourceException;
import arbiter.workspace.SourceFailure;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadFeature;

/** Integration checks of the adjudicator's CSV and JSON dataset export (#37). */
class ExportServiceTest {
    /** {@link Records#NOW}, when the fixture stamps every answer and decision, as ISO-8601 in UTC. */
    private static final String NOW = Records.NOW.toString();
    /** A label key that CSV must quote. */
    private static final String UNSURE = "unsure, \"maybe\"";
    private static final List<String> ITEM_COLUMNS = List.of("path", "status", "answer", "method", "decided_by",
            "decided_at");
    /** The CSV header when the most submissions any item has is three. */
    private static final List<String> THREE_GROUP_HEADER = Stream.concat(ITEM_COLUMNS.stream(), Stream.of(
            "annotator_1", "answer_1", "submitted_at_1", "annotator_2", "answer_2", "submitted_at_2",
            "annotator_3", "answer_3", "submitted_at_3")).toList();
    private static final CsvMapper CSV = CsvMapper.builder().enable(CsvReadFeature.WRAP_AS_ARRAY).build();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @TempDir
    Path temporary;

    private TestWorkspace workspace;

    @BeforeEach
    void createWorkspace() {
        workspace = TestWorkspace.create(temporary.resolve("workspace"));
    }

    @Test
    void export_csvSingleProject_everyItemInRegistrationOrderWithCurrentDecisionProvenance() throws IOException {
        // k = 3, and carol is disabled. Item 0 is MAJORITY with carol disagreeing, 1 ADJUDICATED, 2 an undecided
        // dispute, 3 a majority from before #27 with no resolution, and 4 has two answers.
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative", UNSURE).items(5)
                .assign("alice", "positive", "positive", "positive", "positive", "positive")
                .assign("bob", "positive", "negative", "negative", "positive", "positive")
                .assign("carol", "negative", UNSURE, UNSURE, "positive")
                .disabled("carol")
                .majority(0, "positive")
                .adjudicated(1, UNSURE)
                .seed(workspace);
        setFormat(flow.projectId(), OutputFormat.CSV);
        String outsideSplit = "Café, naïve/résumé.txt";
        registerOutsideSplit(flow.projectId(), outsideSplit);

        ExportSummary summary = service().export(flow.projectId());

        assertEquals(new ExportSummary(exportFile(flow.projectId(), "csv"), 2, 4), summary);
        int columns = THREE_GROUP_HEADER.size();
        assertEquals(List.of(THREE_GROUP_HEADER,
                List.of("media/corpus-1/item-1.txt", "RESOLVED", "positive", "MAJORITY", "", NOW,
                        "alice", "positive", NOW, "bob", "positive", NOW, "carol", "negative", NOW),
                List.of("media/corpus-1/item-2.txt", "RESOLVED", UNSURE, "ADJUDICATED", TestWorkspace.OWNER, NOW,
                        "alice", "positive", NOW, "bob", "negative", NOW, "carol", UNSURE, NOW),
                unresolved("media/corpus-1/item-3.txt", columns),
                unresolved("media/corpus-1/item-4.txt", columns),
                unresolved("media/corpus-1/item-5.txt", columns),
                unresolved("media/" + outsideSplit, columns)),
                readCsv(summary.file()));
    }

    @Test
    void export_jsonScaleProject_meansAndRatingsWithoutLoss() throws IOException {
        // k = 6. Item 0's ratings average 2.5 and item 1's 7/3, and item 2 has one rating.
        ClassificationWorkflow flow = ClassificationWorkflow.scale(1, 5).items(3)
                .assign("alice", 1, 1, 5).assign("bob", 2, 2).assign("carol", 2, 2)
                .assign("dave", 3, 3).assign("erin", 3, 3).assign("frank", 4, 3)
                .mean(0, 2.5).mean(1, 7.0 / 3)
                .seed(workspace);

        ExportSummary summary = service().export(flow.projectId());

        assertEquals(new ExportSummary(exportFile(flow.projectId(), "json"), 2, 1), summary);
        JsonNode written = readJson(summary.file());
        assertEquals(7.0 / 3, written.at("/items/1/answer").doubleValue());
        assertEquals(JSON.readTree("""
                {"project": "Synthetic project", "taxonomyKind": "SCALE", "items": [
                  {"path": "media/corpus-1/item-1.txt", "status": "RESOLVED", "answer": 2.5,
                   "method": "AUTO_SCALE", "decidedBy": null, "decidedAt": "NOW", "submissions": [
                     {"annotator": "alice", "answer": 1, "submittedAt": "NOW"},
                     {"annotator": "bob", "answer": 2, "submittedAt": "NOW"},
                     {"annotator": "carol", "answer": 2, "submittedAt": "NOW"},
                     {"annotator": "dave", "answer": 3, "submittedAt": "NOW"},
                     {"annotator": "erin", "answer": 3, "submittedAt": "NOW"},
                     {"annotator": "frank", "answer": 4, "submittedAt": "NOW"}]},
                  {"path": "media/corpus-1/item-2.txt", "status": "RESOLVED", "answer": 2.3333333333333335,
                   "method": "AUTO_SCALE", "decidedBy": null, "decidedAt": "NOW", "submissions": [
                     {"annotator": "alice", "answer": 1, "submittedAt": "NOW"},
                     {"annotator": "bob", "answer": 2, "submittedAt": "NOW"},
                     {"annotator": "carol", "answer": 2, "submittedAt": "NOW"},
                     {"annotator": "dave", "answer": 3, "submittedAt": "NOW"},
                     {"annotator": "erin", "answer": 3, "submittedAt": "NOW"},
                     {"annotator": "frank", "answer": 3, "submittedAt": "NOW"}]},
                  {"path": "media/corpus-1/item-3.txt", "status": "UNRESOLVED", "answer": null, "method": null,
                   "decidedBy": null, "decidedAt": null, "submissions": []}]}
                """.replace("NOW", NOW)), written);
    }

    @Test
    void export_csvScaleProjectWithSplitsOfDifferentK_meansWithoutLossAndGroupsPaddedToLargestK() throws IOException {
        // k = 3. Item 0's ratings average 7/3 and item 1's exactly 3, and item 2 has one rating.
        ClassificationWorkflow flow = ClassificationWorkflow.scale(1, 5).items(3)
                .assign("alice", 1, 3, 5).assign("bob", 3, 3).assign("carol", 3, 3)
                .mean(0, 7.0 / 3).mean(1, 3.0)
                .seed(workspace);
        setFormat(flow.projectId(), OutputFormat.CSV);
        addSplitWithOneRating(flow.projectId(), flow.annotatorId("alice"), 4);

        ExportSummary summary = service().export(flow.projectId());

        assertEquals(new ExportSummary(exportFile(flow.projectId(), "csv"), 3, 1), summary);
        List<List<String>> rows = readCsv(summary.file());
        assertEquals(7.0 / 3, Double.parseDouble(rows.get(1).get(2)));
        assertEquals(List.of(THREE_GROUP_HEADER,
                List.of("media/corpus-1/item-1.txt", "RESOLVED", "2.3333333333333335", "AUTO_SCALE", "", NOW,
                        "alice", "1", NOW, "bob", "3", NOW, "carol", "3", NOW),
                List.of("media/corpus-1/item-2.txt", "RESOLVED", "3.0", "AUTO_SCALE", "", NOW,
                        "alice", "3", NOW, "bob", "3", NOW, "carol", "3", NOW),
                unresolved("media/corpus-1/item-3.txt", THREE_GROUP_HEADER.size()),
                List.of("media/batch-2/item-1.txt", "RESOLVED", "4.0", "AUTO_SCALE", "", NOW,
                        "alice", "4", NOW, "", "", "", "", "", "")),
                rows);
    }

    @Test
    void previewAndExport_resolvedAndUnresolvedItems_sameLocationAndCountsWithRecordsUnchanged() throws IOException {
        // k = 2. Item 0 is MAJORITY, 1 an undecided dispute, and 2 has no answers.
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(3)
                .assign("alice", "positive", "positive").assign("bob", "positive", "negative")
                .majority(0, "positive").seed(workspace);
        ExportService service = service();
        ExportSummary expected = new ExportSummary(exportFile(flow.projectId(), "json"), 1, 2);
        byte[] before = workspace.dataFileBytes();

        assertEquals(expected, service.preview(flow.projectId()));
        assertEquals(List.of(), exportNames());
        assertEquals(expected, service.export(flow.projectId()));

        assertEquals(List.of(expected.file().getFileName().toString()), exportNames());
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void export_noItemsOrNothingResolved_validFileWithoutSubmissions() throws IOException {
        long empty = new ProjectService(workspace.store(), workspace.signInOwner())
                .create("Empty project", null, TaxonomyKind.SINGLE, OutputFormat.JSON).getId();
        // k = 2. Item 0 is an undecided dispute, and item 1 has no answers.
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(2)
                .assign("alice", "positive").assign("bob", "negative").seed(workspace);
        setFormat(flow.projectId(), OutputFormat.CSV);
        ExportService service = service();

        ExportSummary noItems = service.export(empty);
        ExportSummary nothingResolved = service.export(flow.projectId());

        assertEquals(new ExportSummary(exportFile(empty, "json"), 0, 0), noItems);
        assertEquals(JSON.readTree("""
                {"project": "Empty project", "taxonomyKind": "SINGLE", "items": []}
                """), readJson(noItems.file()));
        assertEquals(new ExportSummary(exportFile(flow.projectId(), "csv"), 0, 2), nothingResolved);
        assertEquals(List.of(ITEM_COLUMNS,
                unresolved("media/corpus-1/item-1.txt", ITEM_COLUMNS.size()),
                unresolved("media/corpus-1/item-2.txt", ITEM_COLUMNS.size())),
                readCsv(nothingResolved.file()));
    }

    @Test
    void export_disputeDecidedAfterEarlierExport_replacedWithDecisionAndSubmissionsInTimeOrder() throws IOException {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob").seed(workspace);
        // Bob's answer is stored after alice's but stamped a minute earlier, so it is listed first.
        Annotation bobs = Records.answer(flow.itemId(0), flow.assignmentId("bob"), flow.annotatorId("bob"),
                flow.labelId("negative"));
        bobs.setSubmittedAt(Records.NOW.minusSeconds(60));
        workspace.store().write(session -> session.annotations().insert(bobs));
        ExportService service = service();
        Path file = service.export(flow.projectId()).file();
        assertEquals("UNRESOLVED", readJson(file).at("/items/0/status").stringValue());
        new ResolutionService(workspace.store(), workspace.signInOwner(), workspace.paths())
                .adjudicate(flow.itemId(0), flow.labelId("negative"));

        ExportSummary replaced = service.export(flow.projectId());

        assertEquals(new ExportSummary(file, 1, 0), replaced);
        JsonNode items = readJson(file).get("items");
        assertEquals(1, items.size());
        JsonNode item = items.get(0);
        assertEquals("RESOLVED", item.get("status").stringValue());
        assertEquals("negative", item.get("answer").stringValue());
        assertEquals("ADJUDICATED", item.get("method").stringValue());
        assertEquals(TestWorkspace.OWNER, item.get("decidedBy").stringValue());
        assertEquals(JSON.readTree("""
                [{"annotator": "bob", "answer": "negative", "submittedAt": "EARLIER"},
                 {"annotator": "alice", "answer": "positive", "submittedAt": "NOW"}]
                """.replace("EARLIER", Records.NOW.minusSeconds(60).toString()).replace("NOW", NOW)),
                item.get("submissions"));
        assertEquals(List.of(file.getFileName().toString()), exportNames());
    }

    @Test
    void export_nonEmptyFolderAtExportPath_projectExceptionAndNothingElseWritten() throws IOException {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").seed(workspace);
        ExportService service = service();
        Path file = exportFile(flow.projectId(), "json");
        Files.createDirectories(file);
        Files.writeString(file.resolve("kept.txt"), "Not an export");
        byte[] before = workspace.dataFileBytes();

        ProjectException rejection = ServiceAssertions.assertRejected(() -> service.export(flow.projectId()));

        assertEquals("The export could not be written to " + file
                + ". If another program has it open, close it and try again.", rejection.getMessage());
        assertEquals(List.of(file.getFileName().toString()), exportNames());
        try (Stream<Path> kept = Files.list(file)) {
            assertEquals(List.of(file.resolve("kept.txt")), kept.toList());
        }
        assertEquals("Not an export", Files.readString(file.resolve("kept.txt")));
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void export_unresolvedItemSourceDeletedOrEdited_sourceExceptionAndEarlierExportKept() throws IOException {
        // k = 2. Item 0 is MAJORITY, 1 a majority from before #27 with no resolution, and 2 an undecided dispute.
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative").items(3)
                .assign("alice", "positive", "positive", "positive")
                .assign("bob", "positive", "positive", "negative")
                .majority(0, "positive").seed(workspace);
        ExportService service = service();
        Path file = service.export(flow.projectId()).file();
        byte[] earlier = Files.readAllBytes(file);
        // Deciding the dispute changes the dataset, so an export published despite a failure would show.
        new ResolutionService(workspace.store(), workspace.signInOwner(), workspace.paths())
                .adjudicate(flow.itemId(2), flow.labelId("negative"));
        byte[] data = workspace.dataFileBytes();
        String storedPath = "media/corpus-1/item-2.txt";
        Path source = workspace.paths().root().resolve(storedPath);

        Files.delete(source);
        SourceException missing = assertThrows(SourceException.class, () -> service.export(flow.projectId()));
        Files.writeString(source, "Edited after import");
        SourceException changed = assertThrows(SourceException.class, () -> service.export(flow.projectId()));

        assertEquals(SourceFailure.MISSING, missing.reason());
        assertEquals(SourceFailure.HASH_MISMATCH, changed.reason());
        for (SourceException failure : List.of(missing, changed)) {
            assertEquals(storedPath, failure.storedPath());
            assertTrue(failure.getMessage().contains(storedPath), failure.getMessage());
        }
        assertArrayEquals(earlier, Files.readAllBytes(file));
        assertEquals(List.of(file.getFileName().toString()), exportNames());
        assertArrayEquals(data, workspace.dataFileBytes());
        assertEquals("Edited after import", Files.readString(source));
    }

    @Test
    void export_resolutionWithAnotherProjectsLabel_failsAndNothingWritten() throws IOException {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").majority(0, "positive").seed(workspace);
        long otherLabel = ClassificationWorkflow.single("yes", "no").seed(workspace).labelId("yes");
        // Corrupt data the store accepts, since it checks only that a resolution's label exists.
        workspace.store().write(session -> {
            Resolution resolution = session.resolutions().findByItem(flow.itemId(0)).orElseThrow();
            resolution.setLabelId(otherLabel);
            return session.resolutions().save(resolution);
        });
        ExportService service = service();
        byte[] before = workspace.dataFileBytes();

        assertThrows(NoSuchElementException.class, () -> service.export(flow.projectId()));

        assertEquals(List.of(), exportNames());
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void previewAndExport_signedOutOrAnnotator_authExceptionAndNothingWritten() throws IOException {
        ClassificationWorkflow flow = ClassificationWorkflow.single("positive", "negative")
                .assign("alice", "positive").assign("bob", "positive").majority(0, "positive").seed(workspace);
        AuthService owner = workspace.signInOwner();
        ExportService signedOut = new ExportService(workspace.store(), owner, workspace.paths());
        signedOut.preview(flow.projectId());
        owner.logout();
        ExportService annotator = new ExportService(workspace.store(), workspace.signIn("alice"), workspace.paths());
        byte[] before = workspace.dataFileBytes();

        for (ExportService service : List.of(signedOut, annotator)) {
            assertThrows(AuthException.class, () -> service.preview(flow.projectId()));
            assertThrows(AuthException.class, () -> service.export(flow.projectId()));
            assertThrows(AuthException.class, () -> service.preview(999_999L));
            assertThrows(AuthException.class, () -> service.export(999_999L));
        }

        assertEquals(List.of(), exportNames());
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void previewAndExport_unknownProject_projectExceptionAndNothingWritten() throws IOException {
        ExportService service = service();
        byte[] before = workspace.dataFileBytes();

        ServiceAssertions.assertRejected(() -> service.preview(999_999L));
        ServiceAssertions.assertRejected(() -> service.export(999_999L));

        assertEquals(List.of(), exportNames());
        assertArrayEquals(before, workspace.dataFileBytes());
    }

    /** Returns a service for the owner. */
    private ExportService service() {
        return new ExportService(workspace.store(), workspace.signInOwner(), workspace.paths());
    }

    /** Sets a project's output format, which the fixture seeds as JSON. */
    private void setFormat(long projectId, OutputFormat format) {
        workspace.store().write(session -> {
            Project project = session.projects().findById(projectId).orElseThrow();
            project.setOutputFormat(format);
            return session.projects().save(project);
        });
    }

    /** Registers a new source below {@code media/} in a project without adding it to a split. */
    private void registerOutsideSplit(long projectId, String relativePath) {
        ResolvedSource source = workspace.writeSource(relativePath, "Synthetic item outside any split");
        workspace.store().write(session -> session.items().save(Records.item(projectId, source)));
    }

    /**
     * Adds a second split with k = 1 to a scale project, holding one new item that this annotator rated, resolved
     * to that rating.
     */
    private void addSplitWithOneRating(long projectId, long annotatorId, int rating) {
        ResolvedSource source = workspace.writeSource("batch-2/item-1.txt", "Synthetic item in a second split");
        workspace.store().write(session -> {
            long itemId = session.items().save(Records.item(projectId, source)).getId();
            Split split = Records.split(projectId, "Batch 2");
            split.setAnnotationsPerItem(1);
            long splitId = session.splits().save(split).getId();
            session.splitItems().save(Records.membership(splitId, itemId));
            long assignmentId = session.assignments().save(Records.assignment(splitId, annotatorId)).getId();
            session.annotations().insert(Records.scaleAnswer(itemId, assignmentId, annotatorId, rating));
            return session.resolutions().save(Records.scaleResolution(itemId, rating));
        });
    }

    /** Returns the file a project's export is written to, {@code exports/project-<id>.<extension>} (#37). */
    private Path exportFile(long projectId, String extension) {
        return workspace.paths().root().resolve("exports").resolve("project-" + projectId + "." + extension);
    }

    /** Returns the names in the workspace's {@code exports/} folder, sorted. */
    private List<String> exportNames() throws IOException {
        try (Stream<Path> files = Files.list(workspace.paths().root().resolve("exports"))) {
            return files.map(file -> file.getFileName().toString()).sorted().toList();
        }
    }

    /** Returns an export's CSV rows, header first, as the cells a CSV reader parses. */
    private static List<List<String>> readCsv(Path file) throws IOException {
        MappingIterator<String[]> rows = CSV.readerFor(String[].class).readValues(readText(file));
        return rows.readAll().stream().map(Arrays::asList).toList();
    }

    private static JsonNode readJson(Path file) throws IOException {
        return JSON.readTree(readText(file));
    }

    /** Returns an export's text, which must be valid UTF-8 without a byte order mark. */
    private static String readText(Path file) throws IOException {
        String text = Files.readString(file);
        assertFalse(text.startsWith("\uFEFF"), file + " starts with a byte order mark");
        return text;
    }

    /** Returns an unresolved item's CSV row: its path and status, and every other cell blank. */
    private static List<String> unresolved(String path, int columns) {
        List<String> row = new ArrayList<>(List.of(path, "UNRESOLVED"));
        row.addAll(Collections.nCopies(columns - row.size(), ""));
        return row;
    }
}
