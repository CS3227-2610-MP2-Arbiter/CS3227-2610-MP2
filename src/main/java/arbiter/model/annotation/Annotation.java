package arbiter.model.annotation;

import java.time.Instant;

/**
 * One annotator's submitted answer for one item.
 *
 * <p>An answer carries at most one of {@code labelId} or {@code scaleValue}. The setters switch
 * between them, and the getters reject a record loaded with both.
 */
public class Annotation {
    /** Persistent identifier. */
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

    /** When the annotation was submitted. */
    private Instant submittedAt;

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

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

}
