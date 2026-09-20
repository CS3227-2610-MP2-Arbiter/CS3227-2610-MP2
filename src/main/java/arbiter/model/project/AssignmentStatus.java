package arbiter.model.project;

/**
 * Where an assignment has got to.
 *
 * <p>{@code SUBMITTED} is not the end of the road: an adjudicator may send a split back for rework,
 * which sets {@code RETURNED} and reopens it for the annotator. Without that state a corrected
 * resubmission could not be told apart from the original.
 */
public enum AssignmentStatus {
    NOT_STARTED,
    IN_PROGRESS,
    SUBMITTED,
    RETURNED
}
