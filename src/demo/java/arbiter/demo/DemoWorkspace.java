package arbiter.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import arbiter.data.json.JsonStore;
import arbiter.model.project.Item;
import arbiter.model.project.OutputFormat;
import arbiter.model.project.Split;
import arbiter.model.project.TaxonomyKind;
import arbiter.service.AnnotationService;
import arbiter.service.Answer;
import arbiter.service.AssignmentProgress;
import arbiter.service.AssignmentService;
import arbiter.service.AuthService;
import arbiter.service.CorpusService;
import arbiter.service.ProjectService;
import arbiter.service.QueueView;
import arbiter.workspace.WorkspaceException;
import arbiter.workspace.WorkspaceLock;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;

/**
 * Seeds a new demo workspace for the smoke checklist (#41), in the state that checklist's Part B describes.
 *
 * <p>Every change goes through the services a user's clicks reach, signed in as the account that would make it, so
 * the seeded workspace holds only states the app can reach and automatic resolution (#27) runs as it does in the
 * app. Each planned answer is keyed by file, so every file's outcome is fixed even though split order is shuffled.
 * The accounts, the password and the expected outcomes are listed in {@code docs/SmokeChecklist.md}.
 */
public final class DemoWorkspace {
    /** The password of every demo account. */
    public static final String PASSWORD = "demo1234";

    /** The owner's username. */
    public static final String OWNER = "owner";

    /** The annotators' usernames, in the order they are created. */
    public static final List<String> ANNOTATORS = List.of("alice", "bob", "carol");

    /** The single-label project's name. */
    public static final String REVIEWS = "Product reviews";

    /** The scale project's name. */
    public static final String HELPFULNESS = "Answer helpfulness";

    /** How many of his queue's files bob rates in the scale project, leaving the rest to answer by hand. */
    public static final int BOB_RATED = 4;

    private static final String CORPUS = "/arbiter/demo/corpus/";

    /** Each review's label from alice, bob and carol. */
    private static final Map<String, List<String>> REVIEW_LABELS = Map.of(
            "reviews/review-01.txt", List.of("positive", "positive", "positive"),
            "reviews/review-02.txt", List.of("negative", "negative", "negative"),
            "reviews/review-03.txt", List.of("positive", "positive", "mixed"),
            "reviews/review-04.txt", List.of("positive", "negative", "mixed"),
            "reviews/review-05.txt", List.of("negative", "mixed", "negative"),
            "reviews/review-06.txt", List.of("mixed", "positive", "negative"));

    /** Each answer's helpfulness rating from alice and bob. */
    private static final Map<String, List<Integer>> ANSWER_RATINGS = Map.of(
            "answers/answer-01.txt", List.of(5, 5),
            "answers/answer-02.txt", List.of(1, 2),
            "answers/answer-03.txt", List.of(5, 4),
            "answers/answer-04.txt", List.of(2, 2),
            "answers/answer-05.txt", List.of(4, 5),
            "answers/answer-06.txt", List.of(2, 3));

    /** The single-label project's labels in taxonomy order, each with its description. */
    private static final List<Map.Entry<String, String>> LABELS = List.of(
            Map.entry("positive", "The reviewer is satisfied overall."),
            Map.entry("negative", "The reviewer is dissatisfied overall."),
            Map.entry("mixed", "Clear praise and clear complaints, neither winning."));

    private DemoWorkspace() {
    }

    /**
     * Seeds a demo workspace in the folder named by the first argument.
     *
     * @param args the folder to create the workspace in
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Name the folder to create the demo workspace in");
        }
        seed(Path.of(args[0]));
    }

    /**
     * Creates a workspace in {@code folder} and seeds the demo into it. If seeding fails, everything it created is
     * deleted, so it never leaves a half-built workspace.
     *
     * @param folder a folder that does not exist yet or is empty
     * @return the new workspace's paths
     * @throws WorkspaceException if the folder exists and is not empty, so existing files are never changed
     */
    public static WorkspacePaths seed(Path folder) {
        boolean folderIsNew = requireNewOrEmpty(folder);
        try {
            WorkspacePaths paths = new WorkspaceService().create(folder);
            copyCorpus(paths);
            try (WorkspaceLock lock = WorkspaceLock.acquire(paths)) {
                JsonStore store = JsonStore.initializeNew(lock);
                AuthService auth = new AuthService(store);
                Map<String, Answer> answers = new HashMap<>();
                Map<Long, String> storedPaths = setUp(store, auth, paths, answers);
                annotate(store, auth, paths, storedPaths, answers);
            }
            return paths;
        } catch (RuntimeException e) {
            discard(folder, folderIsNew, e);
            throw e;
        }
    }

    /** Refuses a folder that holds anything, and returns whether the folder does not exist yet. */
    private static boolean requireNewOrEmpty(Path folder) {
        if (Files.notExists(folder)) {
            return true;
        }
        if (Files.isDirectory(folder)) {
            try (Stream<Path> entries = Files.list(folder)) {
                if (entries.findAny().isEmpty()) {
                    return false;
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + folder, e);
            }
        }
        throw new WorkspaceException("The demo can only be seeded into a new or empty folder, and " + folder
                + " is neither. Delete it or choose another folder.");
    }

    /** Deletes everything a failed seed put in {@code folder}, and the folder itself if the seed made it. */
    private static void discard(Path folder, boolean folderIsNew, RuntimeException failure) {
        if (Files.notExists(folder)) {
            return;
        }
        try (Stream<Path> entries = Files.walk(folder)) {
            for (Path entry : entries.sorted(Comparator.reverseOrder()).toList()) {
                if (folderIsNew || !entry.equals(folder)) {
                    Files.delete(entry);
                }
            }
        } catch (IOException e) {
            failure.addSuppressed(e);
        }
    }

    private static void copyCorpus(WorkspacePaths paths) {
        for (String file : corpusFiles()) {
            Path target = paths.mediaDirectory().resolve(file);
            try (InputStream source = DemoWorkspace.class.getResourceAsStream(CORPUS + file)) {
                if (source == null) {
                    throw new IllegalStateException("The demo corpus is missing " + file);
                }
                Files.createDirectories(target.getParent());
                Files.copy(source, target);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not copy " + file + " into the demo workspace", e);
            }
        }
    }

    /**
     * Sets up the accounts and both projects as the owner, and returns each item's stored path by its identifier.
     * Fills {@code answers} with each annotator's planned answer, keyed by {@link #key}.
     */
    private static Map<Long, String> setUp(JsonStore store, AuthService auth, WorkspacePaths paths,
            Map<String, Answer> answers) {
        auth.bootstrapOwner(OWNER, PASSWORD);
        auth.login(OWNER, PASSWORD);
        List<Long> annotatorIds = new ArrayList<>();
        for (String username : ANNOTATORS) {
            annotatorIds.add(auth.createAnnotator(username, PASSWORD).id());
        }
        ProjectService projects = new ProjectService(store, auth);
        CorpusService corpus = new CorpusService(store, auth, paths);
        AssignmentService assignments = new AssignmentService(store, auth);
        Map<Long, String> storedPaths = new HashMap<>();

        long reviews = projects.create(REVIEWS, "Is each product review positive, negative or mixed?",
                TaxonomyKind.SINGLE, OutputFormat.CSV).getId();
        Map<String, Long> labelIds = new HashMap<>();
        for (Map.Entry<String, String> label : LABELS) {
            labelIds.put(label.getKey(), corpus.addLabel(reviews, label.getKey(), label.getValue()).getId());
        }
        register(corpus, paths, reviews, REVIEW_LABELS.keySet(), storedPaths);
        REVIEW_LABELS.forEach((file, labels) -> {
            for (int index = 0; index < labels.size(); index++) {
                answers.put(key(ANNOTATORS.get(index), storedPath(file)),
                        new Answer.LabelChoice(labelIds.get(labels.get(index))));
            }
        });
        assignments.assign(onlySplit(corpus, reviews), "3", annotatorIds);

        long helpfulness = projects.create(HELPFULNESS, "How helpful is each answer to a support question?",
                TaxonomyKind.SCALE, OutputFormat.JSON).getId();
        corpus.saveRange(helpfulness, "1", "5");
        register(corpus, paths, helpfulness, ANSWER_RATINGS.keySet(), storedPaths);
        ANSWER_RATINGS.forEach((file, ratings) -> {
            for (int index = 0; index < ratings.size(); index++) {
                answers.put(key(ANNOTATORS.get(index), storedPath(file)), new Answer.Rating(ratings.get(index)));
            }
        });
        assignments.assign(onlySplit(corpus, helpfulness), "2", annotatorIds.subList(0, 2));
        auth.logout();
        return storedPaths;
    }

    private static void register(CorpusService corpus, WorkspacePaths paths, long projectId,
            Iterable<String> corpusFiles, Map<Long, String> storedPaths) {
        List<Path> chosen = new ArrayList<>();
        for (String file : corpusFiles) {
            chosen.add(paths.mediaDirectory().resolve(file));
        }
        // Map.of's order changes from run to run, so sort to register the files in the same order every time.
        chosen.sort(null);
        for (Item item : corpus.register(projectId, chosen)) {
            storedPaths.put(item.getId(), item.getPath());
        }
    }

    /** Returns the path a corpus file is stored with once it is registered from {@code media/} (#25). */
    private static String storedPath(String file) {
        return WorkspacePaths.MEDIA_DIRECTORY + "/" + file;
    }

    /** Generates one split holding every file of the project and returns its identifier. */
    private static long onlySplit(CorpusService corpus, long projectId) {
        String all = String.valueOf(corpus.list(projectId).size());
        List<Split> splits = corpus.generateSplits(projectId, all, corpus.previewSplits(projectId, all));
        return splits.getFirst().getId();
    }

    /** Submits each annotator's planned answers in queue order, signed in as that annotator. */
    private static void annotate(JsonStore store, AuthService auth, WorkspacePaths paths,
            Map<Long, String> storedPaths, Map<String, Answer> answers) {
        AnnotationService annotations = new AnnotationService(store, auth, paths);
        for (String username : ANNOTATORS) {
            auth.login(username, PASSWORD);
            for (AssignmentProgress assignment : annotations.forCurrentUser()) {
                QueueView queue = annotations.forCurrentUser(assignment.assignmentId());
                boolean scale = queue.taxonomy().kind() == TaxonomyKind.SCALE;
                int limit = scale && username.equals("bob") ? BOB_RATED : assignment.total();
                for (int done = 0; done < limit; done++) {
                    long itemId = queue.current().itemId();
                    Answer answer = answers.get(key(username, storedPaths.get(itemId)));
                    queue = annotations.submit(assignment.assignmentId(), itemId, answer);
                }
            }
            auth.logout();
        }
    }

    private static String key(String username, String file) {
        return username + ":" + file;
    }

    private static List<String> corpusFiles() {
        List<String> all = new ArrayList<>(REVIEW_LABELS.keySet());
        all.addAll(ANSWER_RATINGS.keySet());
        return all;
    }
}
