package arbiter.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.Item;
import arbiter.model.project.Split;
import arbiter.model.project.SplitItem;
import arbiter.model.project.TaxonomySettings;
import arbiter.workspace.SourceException;
import arbiter.workspace.SourceResolver;
import arbiter.workspace.WorkspacePaths;

/**
 * The annotator's own work: their assignments, their queue and, as later issues add them, their submissions.
 *
 * <p>Every {@code forCurrentUser} method requires a signed-in annotator through
 * {@link AuthService#requireAnnotator} and reads only that annotator's assignments and answers, never another
 * annotator's work or a resolution (rule 1). They are the only annotation reads annotator screens may make,
 * which the blindness test relies on.
 */
public final class AnnotationService {
    private static final Comparator<Assignment> HOME_ORDER = Comparator
            .comparing((Assignment assignment) -> assignment.getStatus() == AssignmentStatus.SUBMITTED)
            .thenComparing(Assignment::getAssignedAt)
            .thenComparing(Assignment::getId);

    private static final String NOT_ASSIGNED = "This split is not assigned to you";

    private final JsonStore store;
    private final AuthService auth;
    private final SourceResolver sources;

    /** Serves one workspace's annotator, whoever is signed in to {@code auth}. */
    public AnnotationService(JsonStore store, AuthService auth, WorkspacePaths paths) {
        this.store = Objects.requireNonNull(store, "store");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.sources = new SourceResolver(Objects.requireNonNull(paths, "paths"));
    }

    /**
     * Returns the signed-in annotator's assignments, read from one snapshot: unfinished ones first, then
     * submitted ones, each in the order they were assigned.
     *
     * @throws AuthException if the caller is not a signed-in annotator
     */
    public List<AssignmentProgress> forCurrentUser() {
        long annotatorId = auth.requireAnnotator().id();
        return store.read(session -> session.assignments().listByAnnotator(annotatorId).stream()
                .sorted(HOME_ORDER)
                .map(assignment -> progress(session, assignment,
                        position(session, assignment.getSplitId(), annotatorId)))
                .toList());
    }

    /**
     * Returns where one of the signed-in annotator's assignments stands (#13): its progress and, unless it is
     * finished, the first file in saved split order without their answer, with that file's text.
     *
     * <p>The position is worked out from stored answers each time, so a restart resumes at the first unanswered
     * file (rule 18). A file whose source is missing, unreadable or changed is returned with the reason instead
     * of its text (rule 21), so the queue still shows where the annotator is.
     *
     * @throws AuthException if the caller is not a signed-in annotator
     * @throws ProjectException if the assignment does not exist or is another annotator's; both are refused
     *     alike, so the refusal reveals nothing about other annotators
     * @throws IllegalStateException if the assignment is not finished but every file has an answer, which
     *     submission (#17) never leaves behind
     */
    public QueueView forCurrentUser(long assignmentId) {
        long annotatorId = auth.requireAnnotator().id();
        Snapshot snapshot = store.read(session -> {
            Assignment assignment = session.assignments().findById(assignmentId)
                    .filter(found -> found.getAnnotatorId() == annotatorId)
                    .orElseThrow(() -> new ProjectException(NOT_ASSIGNED));
            Position position = position(session, assignment.getSplitId(), annotatorId);
            AssignmentProgress progress = progress(session, assignment, position);
            Taxonomy taxonomy = taxonomy(session, assignment.getSplitId());
            if (progress.finished()) {
                return new Snapshot(progress, null, taxonomy);
            }
            if (position.nextItemId() == null) {
                throw new IllegalStateException("Assignment " + assignmentId
                        + " has an answer for every file but is not submitted");
            }
            return new Snapshot(progress, session.items().findById(position.nextItemId()).orElseThrow(), taxonomy);
        });
        AssignmentProgress progress = snapshot.progress();
        Taxonomy taxonomy = snapshot.taxonomy();
        Item item = snapshot.next();
        if (item == null) {
            return new QueueView(progress, null, taxonomy);
        }
        // The file is read outside the store's action, which holds the workspace's data, not its media.
        try {
            String text = sources.resolve(item.getPath(), item.getContentHash()).text();
            return new QueueView(progress, new QueueItem(item.getId(), text, null), taxonomy);
        } catch (SourceException e) {
            return new QueueView(progress, new QueueItem(item.getId(), null, e.reason()), taxonomy);
        }
    }

    /** Returns the taxonomy of a split's project, with its labels in their saved order. */
    private static Taxonomy taxonomy(RepositorySession session, long splitId) {
        long projectId = session.splits().findById(splitId).orElseThrow().getProjectId();
        TaxonomySettings settings = session.taxonomySettings().findByProject(projectId).orElseThrow();
        List<LabelOption> labels = session.labels().listByProject(projectId).stream()
                .map(label -> new LabelOption(label.getId(), label.getKey(), label.getDescription()))
                .toList();
        return new Taxonomy(settings.getKind(), labels, settings.getScaleMin(), settings.getScaleMax());
    }

    private static AssignmentProgress progress(RepositorySession session, Assignment assignment, Position position) {
        Split split = session.splits().findById(assignment.getSplitId()).orElseThrow();
        String projectName = session.projects().findById(split.getProjectId()).orElseThrow().getName();
        return new AssignmentProgress(assignment.getId(), projectName, split.getName(), assignment.getStatus(),
                position.submitted(), position.total());
    }

    /**
     * Works out where an annotator stands in a split from its saved order and their own answers: the one place
     * the next file is decided, for the home screen, the queue and submission alike.
     */
    private static Position position(RepositorySession session, long splitId, long annotatorId) {
        List<SplitItem> members = session.splitItems().listBySplit(splitId);
        int submitted = 0;
        Long nextItemId = null;
        for (SplitItem member : members) {
            if (session.annotations().findByItemAndAnnotator(member.getItemId(), annotatorId).isPresent()) {
                submitted++;
            } else if (nextItemId == null) {
                nextItemId = member.getItemId();
            }
        }
        return new Position(submitted, members.size(), nextItemId);
    }

    /**
     * Where an annotator stands in a split.
     *
     * @param submitted the split's files they have answered
     * @param total the split's files
     * @param nextItemId the first file in saved order they have not answered, or null if none is left
     */
    private record Position(int submitted, int total, Long nextItemId) {
    }

    /** An assignment's progress, its next file's record and its project's taxonomy, read from one snapshot. */
    private record Snapshot(AssignmentProgress progress, Item next, Taxonomy taxonomy) {
    }
}
