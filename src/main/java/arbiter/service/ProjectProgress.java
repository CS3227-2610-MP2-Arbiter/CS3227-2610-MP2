package arbiter.service;

import java.util.List;

import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.user.AccountStatus;

/** One adjudicator-only snapshot of a project's progress (#33). */
public record ProjectProgress(long projectId, String projectName, TaxonomyKind kind, long itemCount,
        long submitted, long total, long unresolvedCount, long unresolvedDisputeCount,
        List<SplitProgress> splits, List<AnnotatorProgress> annotators) {
    /** Keeps the snapshot's rows immutable. */
    public ProjectProgress {
        splits = List.copyOf(splits);
        annotators = List.copyOf(annotators);
    }

    /** Progress on one split, with its saved k or null before its first assignment. */
    public record SplitProgress(long splitId, String name, long itemCount, Integer annotationsPerItem,
            int assignedPlaces, long submitted, long total, List<AssignmentProgressRow> assignments) {
        /** Keeps assignment rows from being changed after the snapshot is returned. */
        public SplitProgress {
            assignments = List.copyOf(assignments);
        }

        /** Returns unfilled k places; an unset k has no known number of places yet. */
        public int vacantPlaces() {
            return annotationsPerItem == null ? 0 : annotationsPerItem - assignedPlaces;
        }
    }

    /** Aggregate progress across one annotator's assignments in this project. */
    public record AnnotatorProgress(long annotatorId, String username, AccountStatus accountStatus,
            long submitted, long total) {
    }

    /** One stored assignment's status and submitted answers, including a disabled annotator's work. */
    public record AssignmentProgressRow(long assignmentId, long splitId, String splitName, long annotatorId,
            String username, AccountStatus accountStatus, AssignmentStatus status, long submitted, long total) {
    }
}
