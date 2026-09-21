package arbiter.model.resolution;

import java.time.Instant;

/**
 * The final answer for an item after adjudication.
 *
 * <p>Like an annotation, a resolution carries at most one of {@code labelId} or {@code scaleValue}.
 * The setters switch between scalar result shapes, and the getters reject conflicting fields loaded
 * without the setters. A scale value is a {@code Double} because the agreed value may be the average
 * of several annotators' answers.
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

    /** How the decision was reached, recorded for provenance. */
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
     * @throws IllegalStateException if both scalar result fields are populated
     */
    public Long getLabelId() {
        assertScalarStateIsValid();
        return labelId;
    }

    /** Sets the winning label and clears any scale value, so only one answer shape is ever set. */
    public void setLabelId(Long labelId) {
        this.labelId = labelId;
        if (labelId != null) {
            this.scaleValue = null;
        }
    }

    /**
     * Returns the winning numeric answer, or null when no scale value is set.
     *
     * @throws IllegalStateException if both scalar result fields are populated
     */
    public Double getScaleValue() {
        assertScalarStateIsValid();
        return scaleValue;
    }

    /** Sets the winning numeric answer and clears any label, so only one answer shape is ever set. */
    public void setScaleValue(Double scaleValue) {
        this.scaleValue = scaleValue;
        if (scaleValue != null) {
            this.labelId = null;
        }
    }

    private void assertScalarStateIsValid() {
        if (labelId != null && scaleValue != null) {
            throw new IllegalStateException("Resolution cannot contain both a label and a scale value");
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
