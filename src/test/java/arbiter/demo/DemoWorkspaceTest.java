package arbiter.demo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.data.json.JsonStore;
import arbiter.model.user.Role;
import arbiter.service.AuthService;
import arbiter.service.CorpusService;
import arbiter.service.DisputeSummary;
import arbiter.service.ExportService;
import arbiter.service.ExportSummary;
import arbiter.service.ProjectService;
import arbiter.service.ProjectSummary;
import arbiter.service.ResolutionService;
import arbiter.service.SplitSummary;
import arbiter.workspace.WorkspaceException;
import arbiter.workspace.WorkspaceLock;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadFeature;
import tools.jackson.dataformat.csv.CsvSchema;

class DemoWorkspaceTest {
    private static final Map<String, String> REVIEW_OUTCOMES = Map.of(
            "media/reviews/review-01.txt", "positive MAJORITY",
            "media/reviews/review-02.txt", "negative MAJORITY",
            "media/reviews/review-03.txt", "positive MAJORITY",
            "media/reviews/review-04.txt", "UNRESOLVED",
            "media/reviews/review-05.txt", "negative MAJORITY",
            "media/reviews/review-06.txt", "UNRESOLVED");

    @TempDir
    Path temp;

    @Test
    void seed_accounts_signInWithTheirRoles() {
        WorkspacePaths paths = DemoWorkspace.seed(temp.resolve("demo"));

        try (WorkspaceLock lock = WorkspaceLock.acquire(new WorkspaceService().open(paths.root()))) {
            AuthService auth = new AuthService(JsonStore.open(lock));
            assertEquals(Role.ADJUDICATOR, auth.login(DemoWorkspace.OWNER, DemoWorkspace.PASSWORD).role());
            for (String annotator : DemoWorkspace.ANNOTATORS) {
                assertEquals(Role.ANNOTATOR, auth.login(annotator, DemoWorkspace.PASSWORD).role());
            }
        }
    }

    @Test
    void seed_singleProject_majoritiesAndDisputesWithKOfThree() throws IOException {
        WorkspacePaths paths = DemoWorkspace.seed(temp.resolve("demo"));

        try (Owner owner = Owner.signIn(paths)) {
            long reviews = owner.project(DemoWorkspace.REVIEWS);
            assertEquals(List.of(3), owner.annotationsPerItem(reviews));
            assertEquals(List.of("media/reviews/review-04.txt", "media/reviews/review-06.txt"),
                    owner.resolutions.disputes(reviews).stream().map(DisputeSummary::path).sorted().toList());
            assertEquals(REVIEW_OUTCOMES, owner.csvOutcomes(reviews));
        }
    }

    @Test
    void seed_scaleProject_meansWithKOfTwoAndTwoFilesLeft() throws IOException {
        WorkspacePaths paths = DemoWorkspace.seed(temp.resolve("demo"));

        try (Owner owner = Owner.signIn(paths)) {
            long helpfulness = owner.project(DemoWorkspace.HELPFULNESS);
            assertEquals(List.of(2), owner.annotationsPerItem(helpfulness));
            Map<String, String> outcomes = owner.jsonOutcomes(helpfulness);
            assertEquals(6, outcomes.size());
            // Which four files bob rated depends on the shuffled split order, but each rated file's mean does not.
            Map<String, String> means = Map.of("answer-01.txt", "5.0", "answer-02.txt", "1.5",
                    "answer-03.txt", "4.5", "answer-04.txt", "2.0", "answer-05.txt", "4.5", "answer-06.txt", "2.5");
            int resolved = 0;
            for (Map.Entry<String, String> outcome : outcomes.entrySet()) {
                String name = Path.of(outcome.getKey()).getFileName().toString();
                if (!outcome.getValue().equals("UNRESOLVED")) {
                    assertEquals(means.get(name) + " AUTO_SCALE", outcome.getValue(), name);
                    resolved++;
                }
            }
            assertEquals(DemoWorkspace.BOB_RATED, resolved);
        }
    }

    @Test
    void seed_twice_sameOutcomes() throws IOException {
        WorkspacePaths first = DemoWorkspace.seed(temp.resolve("first"));
        WorkspacePaths second = DemoWorkspace.seed(temp.resolve("second"));

        try (Owner one = Owner.signIn(first)) {
            try (Owner two = Owner.signIn(second)) {
                assertEquals(one.csvOutcomes(one.project(DemoWorkspace.REVIEWS)),
                        two.csvOutcomes(two.project(DemoWorkspace.REVIEWS)));
                assertEquals(one.projects.list().size(), two.projects.list().size());
            }
        }
    }

    @Test
    void seed_existingWorkspace_refusedAndUnchanged() throws IOException {
        WorkspacePaths paths = DemoWorkspace.seed(temp.resolve("demo"));
        byte[] before = Files.readAllBytes(paths.dataFile());

        assertThrows(WorkspaceException.class, () -> DemoWorkspace.seed(paths.root()));

        assertArrayEquals(before, Files.readAllBytes(paths.dataFile()));
    }

    /** The demo owner, signed in to a seeded workspace that it holds the lock of. */
    private static final class Owner implements AutoCloseable {
        private final WorkspaceLock lock;
        private final ProjectService projects;
        private final CorpusService corpus;
        private final ResolutionService resolutions;
        private final ExportService exports;

        private Owner(WorkspaceLock lock) {
            this.lock = lock;
            JsonStore store = JsonStore.open(lock);
            AuthService auth = new AuthService(store);
            auth.login(DemoWorkspace.OWNER, DemoWorkspace.PASSWORD);
            projects = new ProjectService(store, auth);
            corpus = new CorpusService(store, auth, lock.paths());
            resolutions = new ResolutionService(store, auth, lock.paths());
            exports = new ExportService(store, auth, lock.paths());
        }

        static Owner signIn(WorkspacePaths paths) {
            return new Owner(WorkspaceLock.acquire(new WorkspaceService().open(paths.root())));
        }

        long project(String name) {
            return projects.list().stream().filter(project -> project.name().equals(name))
                    .mapToLong(ProjectSummary::id).findFirst().orElseThrow();
        }

        List<Integer> annotationsPerItem(long projectId) {
            return corpus.listSplits(projectId).stream().map(SplitSummary::annotationsPerItem).toList();
        }

        /** Exports a CSV project and returns each file's answer and method, or its unresolved status. */
        Map<String, String> csvOutcomes(long projectId) throws IOException {
            ExportSummary summary = exports.export(projectId);
            List<List<String>> rows = new CsvMapper().readerFor(List.class)
                    .with(CsvSchema.emptySchema().withSkipFirstDataRow(true))
                    .with(CsvReadFeature.WRAP_AS_ARRAY)
                    .<List<String>>readValues(Files.readAllBytes(summary.file())).readAll();
            Map<String, String> outcomes = new TreeMap<>();
            for (List<String> row : rows) {
                String outcome = row.get(1).equals("UNRESOLVED") ? "UNRESOLVED" : row.get(2) + " " + row.get(3);
                outcomes.put(row.get(0), outcome);
            }
            return outcomes;
        }

        /** Exports a JSON project and returns each file's answer and method, or its unresolved status. */
        Map<String, String> jsonOutcomes(long projectId) throws IOException {
            ExportSummary summary = exports.export(projectId);
            JsonNode dataset = JsonMapper.builder().build().readTree(Files.readAllBytes(summary.file()));
            Map<String, String> outcomes = new TreeMap<>();
            for (JsonNode item : dataset.get("items")) {
                String status = item.get("status").asString();
                outcomes.put(item.get("path").asString(), status.equals("UNRESOLVED") ? "UNRESOLVED"
                        : item.get("answer").asString() + " " + item.get("method").asString());
            }
            return outcomes;
        }

        @Override
        public void close() {
            lock.close();
        }
    }
}
