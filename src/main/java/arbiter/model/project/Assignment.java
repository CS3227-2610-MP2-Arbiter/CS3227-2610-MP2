package arbiter.model.project;

import java.time.Instant;

/** The link between one split and one annotator. */
public class Assignment {
    /** Persistent identifier. */
    private Long id;

    /** Split being worked. */
    private Long splitId;

    /** Annotator working it. */
    private Long annotatorId;

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
