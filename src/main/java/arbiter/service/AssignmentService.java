package arbiter.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.Split;
import arbiter.model.user.AccountStatus;
import arbiter.model.user.Role;
import arbiter.model.user.User;

/**
 * Assigns annotators to a split's places (#32), saving the split's k with its first assignment, which freezes
 * its project (rule 3) and locks the split (rule 14) through {@link FirstAssignment}.
 *
 * <p>Every method first requires the signed-in adjudicator through {@link AuthService#requireAdjudicator},
 * so anyone else gets its {@link AuthException}. The results cover every annotator's assignments, so only
 * adjudicator screens may call this (rule 1). Nothing here removes an assignment or changes its split or
 * annotator (rule 19).
 */
public final class AssignmentService {
    /** The default k, from which {@link AssignmentOptions#prefilledAnnotationsPerItem} derives the form's k. */
    public static final int DEFAULT_ANNOTATIONS_PER_ITEM = 2;

    private static final String ANNOTATIONS_PER_ITEM_RULE = "Annotators per file must be a positive whole number";

    private final JsonStore store;
    private final AuthService auth;

    /** Manages one workspace's assignments on behalf of whoever is signed in to {@code auth}. */
    public AssignmentService(JsonStore store, AuthService auth) {
        this.store = Objects.requireNonNull(store, "store");
        this.auth = Objects.requireNonNull(auth, "auth");
    }

    /**
     * Returns what a split's assignment form shows, read from one snapshot.
     *
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if no split has this identifier
     */
    public AssignmentOptions options(long splitId) {
        auth.requireAdjudicator();
        return store.read(session -> {
            Split split = requireSplit(session, splitId);
            List<Assignment> assigned = assignments(session, splitId);
            Set<Long> assigneeIds = new HashSet<>();
            List<AccountSummary> assignees = new ArrayList<>();
            for (Assignment assignment : assigned) {
                assigneeIds.add(assignment.getAnnotatorId());
                assignees.add(AuthService.summarize(session.users().findById(assignment.getAnnotatorId())
                        .orElseThrow()));
            }
            List<AnnotatorLoad> offered = session.users().listByRole(Role.ANNOTATOR).stream()
                    .filter(user -> user.getAccountStatus() == AccountStatus.ACTIVE)
                    .filter(user -> !assigneeIds.contains(user.getId()))
                    .sorted(Comparator.comparing(User::getId))
                    .map(user -> load(session, user))
                    .toList();
            return new AssignmentOptions(split.getAnnotationsPerItem(),
                    session.users().countActiveByRole(Role.ANNOTATOR), assignees, offered);
        });
    }

    /**
     * Checks an assignment request as {@link #assign} would, without storing anything, and returns the k to
     * confirm.
     *
     * <p>Before the project's first assignment ({@link FirstAssignment}), its taxonomy must be ready for it
     * ({@link CorpusService#requireTaxonomyReady}). Before the split's first assignment, k is the typed text,
     * parsed by {@link CorpusService#parseCount}, and at most the number of active annotators. Afterwards the text
     * is ignored and the split's saved k applies. The split's free places are k less its assignments, whatever
     * their status or annotator's account status (rule 19).
     *
     * @param annotationsPerItemText the split's k as the adjudicator typed it
     * @param annotatorIds the account identifiers of the annotators to assign
     * @return the k the split has or would be given
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if no split has this identifier, the project's taxonomy is not ready, the k text
     *     breaks the rule above, no annotator is chosen, a chosen account is missing, the owner or disabled, is
     *     chosen more than once or is already assigned to the split, or more annotators are chosen than the split
     *     has free places
     */
    public int check(long splitId, String annotationsPerItemText, List<Long> annotatorIds) {
        auth.requireAdjudicator();
        return store.read(session -> requireValid(session, requireSplit(session, splitId), annotationsPerItemText,
                annotatorIds));
    }

    /**
     * Assigns the chosen annotators to a split in one committed action, each as a new {@code NOT_STARTED}
     * assignment stamped with one assigned time. If the split has had no assignment yet ({@link FirstAssignment}),
     * the action also saves its k, which then never changes.
     *
     * <p>Every check of {@link #check} runs inside that action, so a stale project page cannot bypass them, and
     * either every chosen annotator is assigned or none is.
     *
     * @param annotationsPerItemText the split's k, as {@link #check} takes it
     * @param annotatorIds the account identifiers of the annotators to assign
     * @return the stored assignments, in the order of {@code annotatorIds}
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if {@link #check} would reject this request; nothing is stored
     */
    public List<Assignment> assign(long splitId, String annotationsPerItemText, List<Long> annotatorIds) {
        auth.requireAdjudicator();
        return store.write(session -> {
            Split split = requireSplit(session, splitId);
            int annotationsPerItem = requireValid(session, split, annotationsPerItemText, annotatorIds);
            if (!FirstAssignment.reachedSplit(session, splitId)) {
                split.setAnnotationsPerItem(annotationsPerItem);
                session.splits().save(split);
            }
            Instant assignedAt = Instant.now();
            List<Assignment> stored = new ArrayList<>();
            for (long annotatorId : annotatorIds) {
                Assignment assignment = new Assignment();
                assignment.setSplitId(splitId);
                assignment.setAnnotatorId(annotatorId);
                assignment.setStatus(AssignmentStatus.NOT_STARTED);
                assignment.setAssignedAt(assignedAt);
                stored.add(session.assignments().save(assignment));
            }
            return stored;
        });
    }

    private static Split requireSplit(RepositorySession session, long splitId) {
        return session.splits().findById(splitId)
                .orElseThrow(() -> new ProjectException("This split no longer exists"));
    }

    /** Applies {@link #check}'s rules to a request, returning the k to use. */
    private static int requireValid(RepositorySession session, Split split, String annotationsPerItemText,
            List<Long> annotatorIds) {
        if (!FirstAssignment.reachedProject(session, split.getProjectId())) {
            CorpusService.requireTaxonomyReady(session, split.getProjectId());
        }
        int annotationsPerItem;
        if (FirstAssignment.reachedSplit(session, split.getId())) {
            annotationsPerItem = split.getAnnotationsPerItem();
        } else {
            annotationsPerItem = CorpusService.parseCount(annotationsPerItemText, ANNOTATIONS_PER_ITEM_RULE);
            long active = session.users().countActiveByRole(Role.ANNOTATOR);
            if (active == 0) {
                throw new ProjectException("There are no active annotators to assign");
            }
            if (annotationsPerItem > active) {
                throw new ProjectException("Annotators per file can be at most " + active
                        + ", the number of active annotators");
            }
        }
        if (annotatorIds.isEmpty()) {
            throw new ProjectException("Choose at least one annotator");
        }
        List<Assignment> existing = assignments(session, split.getId());
        Set<Long> assigned = new HashSet<>();
        for (Assignment assignment : existing) {
            assigned.add(assignment.getAnnotatorId());
        }
        Set<Long> chosen = new HashSet<>();
        for (long annotatorId : annotatorIds) {
            User account = session.users().findById(annotatorId)
                    .orElseThrow(() -> new ProjectException("A chosen account no longer exists"));
            if (account.getRole() != Role.ANNOTATOR) {
                throw new ProjectException("The workspace owner cannot be assigned");
            }
            if (account.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new ProjectException(account.getUsername() + "'s account is disabled");
            }
            if (!chosen.add(annotatorId)) {
                throw new ProjectException(account.getUsername() + " is chosen more than once");
            }
            if (assigned.contains(annotatorId)) {
                throw new ProjectException(account.getUsername() + " is already assigned to " + split.getName());
            }
        }
        int free = annotationsPerItem - existing.size();
        if (annotatorIds.size() > free) {
            throw new ProjectException(free <= 0 ? split.getName() + " has no free places"
                    : free == 1 ? "Only 1 place is free on " + split.getName()
                    : "Only " + free + " places are free on " + split.getName());
        }
        return annotationsPerItem;
    }

    /** Returns a split's assignments in assignment order. */
    private static List<Assignment> assignments(RepositorySession session, long splitId) {
        return session.assignments().listBySplit(splitId).stream()
                .sorted(Comparator.comparing(Assignment::getId))
                .toList();
    }

    private static AnnotatorLoad load(RepositorySession session, User annotator) {
        List<Assignment> unfinished = session.assignments().listByAnnotator(annotator.getId()).stream()
                .filter(assignment -> assignment.getStatus() != AssignmentStatus.SUBMITTED)
                .toList();
        long files = unfinished.stream()
                .mapToLong(assignment -> session.splitItems().countBySplit(assignment.getSplitId()))
                .sum();
        return new AnnotatorLoad(annotator.getId(), annotator.getUsername(), unfinished.size(), files);
    }
}
