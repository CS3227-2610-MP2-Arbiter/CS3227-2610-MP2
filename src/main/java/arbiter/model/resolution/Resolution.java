package arbiter.model.resolution;

import java.time.Instant;

/**
 * How an item was settled: its final answer and how it was reached.
 *
 * <p>A resolution exists only once its item is settled. An item with k valid answers and no resolution
 * awaits the adjudicator, because automatic resolution runs as the kth answer is recorded (#27).
 *
 * <p>A resolution carries at most one result: a {@code labelId} or {@code scaleValue}. The setters
 * switch between them, and the getters reject a record loaded with both. A scale value is a
 * {@code Double} because it may be the average of several annotators' answers.
 *
 * <p>Contributors are derived, not stored. A label or scale result has the item's k submitted answers
 * as contributors, one per annotator. For a disputed SINGLE item, an adjudicator may choose a label
 * that differs from every submission without changing those contributors. This relies on each item
 * being in at most one split (rule 7), at most k assignments per split to distinct annotators (#32),
 * one immutable submission per assignment and item (#17), and no decision before k answers (#27,
 * #34). Relaxing any of these requires storing contributors instead.
 */
public class Resolution {
    /** Persistent identifier. */
    private Long id;

    /** Item being resolved. */
    private Long itemId;

    /** Winning label, for a taxonomy whose kind is SINGLE. */
    private Long labelId;

    /** Winning numeric answer, for a taxonomy whose kind is SCALE. */
    private Double scaleValue;

    /** How the decision was reached. */
    private ResolutionMethod method;

    /** Adjudicator who decided, null for an automatic resolution. */
    private Long decidedByUserId;

    /** When the item was resolved. */
    private Instant decidedAt;

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
        }
    }

    private void assertResultShapeIsValid() {
        int results = (labelId == null ? 0 : 1) + (scaleValue == null ? 0 : 1);
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
}
