package arbiter.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.Split;
import arbiter.model.project.SplitItem;

/**
 * The annotator's own work: their assignments and, as later issues add them, their queue and submissions.
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

    private final JsonStore store;
    private final AuthService auth;

    /** Serves one workspace's annotator, whoever is signed in to {@code auth}. */
    public AnnotationService(JsonStore store, AuthService auth) {
        this.store = Objects.requireNonNull(store, "store");
        this.auth = Objects.requireNonNull(auth, "auth");
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
                .map(assignment -> progress(session, assignment, annotatorId))
                .toList());
    }

    private static AssignmentProgress progress(RepositorySession session, Assignment assignment, long annotatorId) {
        Split split = session.splits().findById(assignment.getSplitId()).orElseThrow();
        String projectName = session.projects().findById(split.getProjectId()).orElseThrow().getName();
        List<SplitItem> members = session.splitItems().listBySplit(split.getId());
        int submitted = 0;
        Long nextItemId = null;
        for (SplitItem member : members) {
            if (session.annotations().findByItemAndAnnotator(member.getItemId(), annotatorId).isPresent()) {
                submitted++;
            } else if (nextItemId == null) {
                nextItemId = member.getItemId();
            }
        }
        return new AssignmentProgress(assignment.getId(), projectName, split.getName(), assignment.getStatus(),
                submitted, members.size(), nextItemId);
    }
}
