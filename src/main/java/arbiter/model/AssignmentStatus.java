package arbiter.model;

/** Where an assignment has got to. RETURNED means the adjudicator sent it back for rework. */
public enum AssignmentStatus {
    NOT_STARTED,
    IN_PROGRESS,
    SUBMITTED,
    RETURNED
}
