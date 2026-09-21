package arbiter.model.project;

/**
 * Where an assignment has got to in its forward-only queue.
 *
 * <p>{@code SUBMITTED} is terminal and follows automatically once every non-retired required item
 * is handled.
 */
public enum AssignmentStatus {
    NOT_STARTED,
    IN_PROGRESS,
    SUBMITTED
}
