package arbiter.model.resolution;

import java.time.Instant;

/**
 * How an item was settled: its final answer and how it was reached.
 *
 * <p>A resolution carries at most one result: a {@code labelId} or {@code scaleValue} for
 * classification, or a {@code selectedAnnotationId} for detection. The setters switch between them,
 * and the getters reject a record loaded with more than one. A scale value is a {@code Double}
 * because it may be the average of several annotators' answers.
 *
 * <p>Contributors are derived, not stored. A label or scale result comes from the item's k valid
 * answers, one per annotator, including when an adjudicator supplied the label; a detection result's
 * only contributor is the selected annotation. This relies on each item being in at most one retained
 * split (#28), at most k assignments per split to distinct annotators (#32), one immutable submission
 * per assignment and item (#17), and no decision before k valid answers (#27, #34). Relaxing any of
 * these requires storing contributors instead.
 */
public class Resolution {
    /** Database identifier. */
    private Long id;

    /** Item being resolved. */
    private Long itemId;

    /** Winning label, for a taxonomy whose kind is SINGLE. */
    private Long labelId;

    /** Winning numeric answer, for a taxonomy whose kind is SCALE. */
    private Double scaleValue;

    /**
     * Valid submitted annotation of this item whose complete box set is the final answer, for a
     * DETECTION task.
     */
    private Long selectedAnnotationId;

    /** How the decision was reached. */
    private ResolutionMethod method;

    /** Adjudicator who decided, null for an automatic resolution. */
    private Long decidedByUserId;

    /** When the item was resolved. */
    private Instant decidedAt;

    /** True while the item is still a dispute. */
    private boolean unresolved;

    /** Creates an empty Resolution. */
    public Resolution() {
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

    /**
     * Returns the winning label, or null when no label is set.
     *
     * @throws IllegalStateException if more than one result field is populated
     */
    public Long getLabelId() {
        assertResultShapeIsValid();
        return labelId;
    }

    /** Sets the winning label and clears any other result. */
    public void setLabelId(Long labelId) {
        this.labelId = labelId;
        if (labelId != null) {
            this.scaleValue = null;
            this.selectedAnnotationId = null;
        }
    }

    /**
     * Returns the winning numeric answer, or null when no scale value is set.
     *
     * @throws IllegalStateException if more than one result field is populated
     */
    public Double getScaleValue() {
        assertResultShapeIsValid();
        return scaleValue;
    }

    /** Sets the winning numeric answer and clears any other result. */
    public void setScaleValue(Double scaleValue) {
        this.scaleValue = scaleValue;
        if (scaleValue != null) {
            this.labelId = null;
            this.selectedAnnotationId = null;
        }
    }

    /**
     * Returns the selected detection annotation, or null when none is selected.
     *
     * @throws IllegalStateException if more than one result field is populated
     */
    public Long getSelectedAnnotationId() {
        assertResultShapeIsValid();
        return selectedAnnotationId;
    }

    /** Selects a detection annotation and clears any other result. */
    public void setSelectedAnnotationId(Long selectedAnnotationId) {
        this.selectedAnnotationId = selectedAnnotationId;
        if (selectedAnnotationId != null) {
            this.labelId = null;
            this.scaleValue = null;
        }
    }

    private void assertResultShapeIsValid() {
        int results = (labelId == null ? 0 : 1) + (scaleValue == null ? 0 : 1)
            + (selectedAnnotationId == null ? 0 : 1);
        if (results > 1) {
            throw new IllegalStateException("Resolution cannot contain more than one result");
        }
    }

    public ResolutionMethod getMethod() {
        return method;
    }

    public void setMethod(ResolutionMethod method) {
        this.method = method;
    }

    public Long getDecidedByUserId() {
        return decidedByUserId;
    }

    public void setDecidedByUserId(Long decidedByUserId) {
        this.decidedByUserId = decidedByUserId;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public boolean isUnresolved() {
        return unresolved;
    }

    public void setUnresolved(boolean unresolved) {
        this.unresolved = unresolved;
    }
}
