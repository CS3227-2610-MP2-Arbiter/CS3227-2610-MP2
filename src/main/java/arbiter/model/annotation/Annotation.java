package arbiter.model.annotation;

import java.time.Instant;

/**
 * One annotator's answer for one item, stored in canonical form.
 *
 * <p>An answer carries exactly one of {@code labelId} or {@code scaleValue}, depending on the
 * project's taxonomy kind. The setters enforce that, so the two can never both be set and the
 * choice cannot be read two ways.
 */
public class Annotation {
    /** Database identifier. */
    private Long id;

    /** Item being annotated. */
    private Long itemId;

    /** Assignment this answer belongs to. */
    private Long assignmentId;

    /** Annotator who made it. */
    private Long annotatorId;

    /** Chosen label, for a taxonomy whose kind is SINGLE. */
    private Long labelId;

    /**
     * Numeric answer, for a taxonomy whose kind is SCALE. An annotator picks a whole number, so
     * this is an int; only the value agreed at resolution may be an average.
     */
    private Integer scaleValue;

    /** The annotator's explanation. */
    private String rationale;

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

    /** Sets the chosen label and clears any scale value, so only one answer shape is ever set. */
    public void setLabelId(Long labelId) {
        this.labelId = labelId;
        if (labelId != null) {
            this.scaleValue = null;
        }
    }

    public Integer getScaleValue() {
        return scaleValue;
    }

    /** Sets the numeric answer and clears any chosen label, so only one answer shape is ever set. */
    public void setScaleValue(Integer scaleValue) {
        this.scaleValue = scaleValue;
        if (scaleValue != null) {
            this.labelId = null;
        }
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
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
