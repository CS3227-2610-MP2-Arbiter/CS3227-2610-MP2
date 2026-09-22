package arbiter.model.annotation;

import java.time.Instant;

/**
 * One annotator's answer or report for one item.
 *
 * <p>An annotation is a draft until {@code submittedAt} is set. A submitted annotation is either a
 * valid answer or, when {@code reportOnly} is set, a report-only outcome that holds no label, scale
 * value or boxes and carries the flag explaining why. Missing fields or an empty box list never mean
 * report-only.
 *
 * <p>An answer carries at most one of {@code labelId} or {@code scaleValue}. The setters switch
 * between them, and the getters reject a record loaded with both.
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

    /** Whole-number answer, for a taxonomy whose kind is SCALE. */
    private Integer scaleValue;

    /** The annotator's optional explanation. */
    private String rationale;

    /** When the draft was first saved. */
    private Instant createdAt;

    /** When the draft was last saved. */
    private Instant updatedAt;

    /** When the annotation was submitted, or null while it is a draft. */
    private Instant submittedAt;

    /** True for a report-only outcome. */
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

    /** Sets the chosen label and clears any scale value. */
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

    /** Sets the numeric answer and clears any chosen label. */
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
