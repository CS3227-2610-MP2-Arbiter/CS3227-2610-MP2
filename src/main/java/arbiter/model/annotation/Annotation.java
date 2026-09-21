package arbiter.model.annotation;

import java.time.Instant;

/**
 * One annotator's answer or report for one item, stored in canonical form.
 *
 * <p>An annotation is a draft until {@code submittedAt} is set; a draft's fields, boxes and flag may
 * still change and are not evidence. Submission is permanent and has one of two outcomes. A valid
 * answer holds the complete answer and may carry a flag. A report-only outcome holds no label, scale
 * value or boxes, and carries the flag that explains why (#16). Only {@code reportOnly} tells them
 * apart: missing fields or an empty box list never mean report-only.
 *
 * <p>An answer carries at most one of {@code labelId} or {@code scaleValue}, depending on the
 * project's taxonomy kind. The setters switch between scalar answer shapes, and the getters reject
 * conflicting fields loaded without the setters.
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

    /** When the draft was first saved. */
    private Instant createdAt;

    /** When the draft was last saved. */
    private Instant updatedAt;

    /** When the annotation was submitted, or null while it is a draft. */
    private Instant submittedAt;

    /** True for a submitted report-only outcome, which holds no answer. */
    private boolean reportOnly;

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

    /**
     * Returns the chosen label, or null when no label is set.
     *
     * @throws IllegalStateException if both scalar answer fields are populated
     */
    public Long getLabelId() {
        assertScalarStateIsValid();
        return labelId;
    }

    /** Sets the chosen label and clears any scale value, so only one answer shape is ever set. */
    public void setLabelId(Long labelId) {
        this.labelId = labelId;
        if (labelId != null) {
            this.scaleValue = null;
        }
    }

    /**
     * Returns the numeric answer, or null when no scale value is set.
     *
     * @throws IllegalStateException if both scalar answer fields are populated
     */
    public Integer getScaleValue() {
        assertScalarStateIsValid();
        return scaleValue;
    }

    /** Sets the numeric answer and clears any chosen label, so only one answer shape is ever set. */
    public void setScaleValue(Integer scaleValue) {
        this.scaleValue = scaleValue;
        if (scaleValue != null) {
            this.labelId = null;
        }
    }

    private void assertScalarStateIsValid() {
        if (labelId != null && scaleValue != null) {
            throw new IllegalStateException("Annotation cannot contain both a label and a scale value");
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

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public boolean isReportOnly() {
        return reportOnly;
    }

    public void setReportOnly(boolean reportOnly) {
        this.reportOnly = reportOnly;
    }

    /** True once submitted, as a valid answer or report-only; either outcome is handled work. */
    public boolean isSubmitted() {
        return submittedAt != null;
    }

    /** True for a submitted answer, the only kind that counts toward k; drafts and reports do not. */
    public boolean isValidAnswer() {
        return isSubmitted() && !reportOnly;
    }
}
