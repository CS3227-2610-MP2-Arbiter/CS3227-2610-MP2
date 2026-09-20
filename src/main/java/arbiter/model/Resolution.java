package arbiter.model;

import java.time.Instant;

/** The final answer for an item after adjudication. */
public class Resolution {
    /** Database identifier. */
    private Long id;

    /** Item being resolved. */
    private Long itemId;

    /** Winning label, null when unresolved or resolved to a scale value. */
    private Long labelId;

    /** Winning scale value, null otherwise. */
    private Integer scaleValue;

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
