package arbiter.model.project;

import java.time.Instant;

/**
 * The link between one split and one annotator.
 *
 * <p>A model class holds data only. Defaults and the rules about when a value may change live in
 * {@code arbiter.service}.
 */
public class Assignment {
    /** Database identifier. */
    private Long id;

    /** Split being worked. */
    private Long splitId;

    /** Annotator working it. */
    private Long annotatorId;

    /** Where the assignment has got to. */
    private AssignmentStatus status;

    /** When the assignment was made. */
    private Instant assignedAt;

    /** When an adjudicator last returned it for rework, null if never returned. */
    private Instant returnedAt;

    /** Adjudicator who last returned it, null if never returned. */
    private Long returnedByUserId;

    /** Why it was returned, so the annotator knows what to fix. */
    private String returnReason;

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

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(Instant returnedAt) {
        this.returnedAt = returnedAt;
    }

    public Long getReturnedByUserId() {
        return returnedByUserId;
    }

    public void setReturnedByUserId(Long returnedByUserId) {
        this.returnedByUserId = returnedByUserId;
    }

    public String getReturnReason() {
        return returnReason;
    }

    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }
}
