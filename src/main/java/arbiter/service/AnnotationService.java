package arbiter.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.annotation.Annotation;
import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.Item;
import arbiter.model.project.Split;
import arbiter.model.project.SplitItem;
import arbiter.workspace.SourceException;
import arbiter.workspace.SourceResolver;
import arbiter.workspace.WorkspacePaths;

/**
 * The annotator's own work: their assignments, their queue and their submissions.
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
        return view(store.read(session -> snapshot(session, requireOwn(session, assignmentId, annotatorId),
                annotatorId)));
    }

    /**
     * Submits the annotator's answer for the file their queue is on and moves the queue forward (#17), all in
     * one committed action: the answer is stored with its submission time and can never change, and the
     * assignment becomes {@code SUBMITTED} once every file has an answer and {@code IN_PROGRESS} before then
     * (rules 2, 18). Automatic resolution of an item's kth answer (#27) belongs in this same action.
     *
     * <p>Every check runs inside that action, and any refusal stores nothing, so a repeated or stale submission
     * cannot add a second answer or move the queue twice.
     *
     * @param itemId the file the annotator answered, which must be the next one in their queue
     * @return where the assignment stands afterwards, as {@link #forCurrentUser(long)} returns it, worked out inside
     *     the same action from what it just stored
     * @throws AuthException if the caller is not a signed-in annotator
     * @throws ProjectException if the assignment is missing or another annotator's, is finished, or the file is
     *     not their next one, already has their answer or cannot be read, or the answer does not follow the
     *     project's taxonomy
     */
    public QueueView submit(long assignmentId, long itemId, Answer answer) {
        Objects.requireNonNull(answer, "answer");
        long annotatorId = auth.requireAnnotator().id();
        Snapshot after = store.write(session -> {
            Assignment assignment = requireOwn(session, assignmentId, annotatorId);
            if (assignment.getStatus() == AssignmentStatus.SUBMITTED) {
                throw new ProjectException("You have already finished this split");
            }
            if (session.annotations().findByItemAndAnnotator(itemId, annotatorId).isPresent()) {
                throw new ProjectException("This file already has your answer, which cannot be changed");
            }
            Position position = position(session, assignment.getSplitId(), annotatorId);
            if (position.nextItemId() == null || position.nextItemId() != itemId) {
                throw new ProjectException("This file is not the next one in your queue");
            }
            if (!taxonomy(session, assignment.getSplitId()).accepts(answer)) {
                throw new ProjectException("Choose one of this project's labels, or a rating within its range");
            }
            requireReadable(session.items().findById(itemId).orElseThrow());
            Annotation stored = new Annotation();
            stored.setItemId(itemId);
            stored.setAssignmentId(assignmentId);
            stored.setAnnotatorId(annotatorId);
            switch (answer) {
            case Answer.LabelChoice choice -> stored.setLabelId(choice.labelId());
            case Answer.Rating rating -> stored.setScaleValue(rating.value());
            }
            stored.setSubmittedAt(Instant.now());
            session.annotations().insert(stored);
            boolean last = position.submitted() + 1 == position.total();
            assignment.setStatus(last ? AssignmentStatus.SUBMITTED : AssignmentStatus.IN_PROGRESS);
            session.assignments().save(assignment);
            return snapshot(session, assignment, annotatorId);
        });
        return view(after);
    }

    /**
     * Reads where an assignment stands from this session: its progress, its next file's record unless it is
     * finished, and its project's taxonomy.
     *
     * @throws IllegalStateException if the assignment is not finished but every file has an answer
     */
    private static Snapshot snapshot(RepositorySession session, Assignment assignment, long annotatorId) {
        Position position = position(session, assignment.getSplitId(), annotatorId);
        AssignmentProgress progress = progress(session, assignment, position);
        TaxonomySummary taxonomy = taxonomy(session, assignment.getSplitId());
        if (progress.finished()) {
            return new Snapshot(progress, null, taxonomy);
        }
        if (position.nextItemId() == null) {
            throw new IllegalStateException("Assignment " + assignment.getId()
                    + " has an answer for every file but is not submitted");
        }
        return new Snapshot(progress, session.items().findById(position.nextItemId()).orElseThrow(), taxonomy);
    }

    /** Turns a snapshot into the queue's view, reading its next file's text outside the store's action. */
    private QueueView view(Snapshot snapshot) {
        Item item = snapshot.next();
        if (item == null) {
            return new QueueView(snapshot.progress(), null, snapshot.taxonomy());
        }
        // The file is read outside the store's action, which holds the workspace's data, not its media.
        try {
            String text = sources.resolve(item.getPath(), item.getContentHash()).text();
            return new QueueView(snapshot.progress(), new QueueItem(item.getId(), text, null), snapshot.taxonomy());
        } catch (SourceException e) {
            return new QueueView(snapshot.progress(), new QueueItem(item.getId(), null, e.reason()),
                    snapshot.taxonomy());
        }
    }

    private static Assignment requireOwn(RepositorySession session, long assignmentId, long annotatorId) {
        return session.assignments().findById(assignmentId)
                .filter(found -> found.getAnnotatorId() == annotatorId)
                .orElseThrow(() -> new ProjectException(NOT_ASSIGNED));
    }

    /** Refuses a file whose source no longer matches, without naming it, since a name can hint at a label. */
    private void requireReadable(Item item) {
        try {
            sources.resolve(item.getPath(), item.getContentHash());
        } catch (SourceException e) {
            throw new ProjectException("This file cannot be read, so it cannot be answered", e);
        }
    }

    /** Returns the taxonomy of a split's project, with its labels in their saved order. */
    private static TaxonomySummary taxonomy(RepositorySession session, long splitId) {
        return CorpusService.taxonomyOf(session, session.splits().findById(splitId).orElseThrow().getProjectId());
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
    private record Snapshot(AssignmentProgress progress, Item next, TaxonomySummary taxonomy) {
    }
}
