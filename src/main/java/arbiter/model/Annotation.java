package arbiter.model;

import java.time.Instant;

/** One annotator's answer for one item, stored in canonical form. */
public class Annotation {
    /** Database identifier. */
    private Long id;

    /** Item being annotated. */
    private Long itemId;

    /** Assignment this answer belongs to. */
    private Long assignmentId;

    /** Annotator who made it. */
    private Long annotatorId;

    /** Chosen label, null for a detection or scale answer. */
    private Long labelId;

    /** Numeric answer for a SCALE taxonomy, null otherwise. */
    private Integer scaleValue;

    /** The annotator's explanation. */
    private String rationale;

    /** How long the annotator spent on the item. */
    private Long timeSpentMillis;

    /** When the answer was first saved. */
    private Instant createdAt;

    /** When the answer was last changed. */
    private Instant updatedAt;

    /** True once the answer has been submitted. */
    private boolean submitted;

    /** Creates an empty Annotation. */
    public Annotation() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Long getAnnotatorId() {
        return annotatorId;
    }

    public void setAnnotatorId(Long annotatorId) {
        this.annotatorId = annotatorId;
    }

    public Long getLabelId() {
        return labelId;
    }

    public void setLabelId(Long labelId) {
        this.labelId = labelId;
    }

    public Integer getScaleValue() {
        return scaleValue;
    }

    public void setScaleValue(Integer scaleValue) {
        this.scaleValue = scaleValue;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public Long getTimeSpentMillis() {
        return timeSpentMillis;
    }

    public void setTimeSpentMillis(Long timeSpentMillis) {
        this.timeSpentMillis = timeSpentMillis;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }
}
