package arbiter.model.project;

import java.time.Instant;

/**
 * The link between one split and one annotator.
 *
 * <p>A model class holds data only. Defaults and the rules about when a value may change - such
 * as a split locking once assigned - live in {@code arbiter.service}.
 */
public class Assignment {
    /** Database identifier. */
    private Long id;

    /** Split being worked. */
    private Long splitId;

    /** Annotator working it. */
    private Long annotatorId;

    /**
     * How many annotators see each item. The default of 2 is applied by {@code AssignmentService}
     * when an assignment is created, not here: this is a data holder, so it stores whatever k the
     * adjudicator chose rather than deciding it. Null means no value has been set yet.
     */
    private Integer annotationsPerItem;

    /** Where the assignment has got to. */
    private AssignmentStatus status;

    /** When the assignment was made. */
    private Instant assignedAt;

    /** Creates an empty Assignment. */
    public Assignment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSplitId() {
        return splitId;
    }

    public void setSplitId(Long splitId) {
        this.splitId = splitId;
    }

    public Long getAnnotatorId() {
        return annotatorId;
    }

    public void setAnnotatorId(Long annotatorId) {
        this.annotatorId = annotatorId;
    }

    public Integer getAnnotationsPerItem() {
        return annotationsPerItem;
    }

    public void setAnnotationsPerItem(Integer annotationsPerItem) {
        this.annotationsPerItem = annotationsPerItem;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }
}
