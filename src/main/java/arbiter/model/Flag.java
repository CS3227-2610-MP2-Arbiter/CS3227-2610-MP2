package arbiter.model;

import java.time.Instant;

/** An annotator's report that an item's source material is unusable. */
public class Flag {
    /** Database identifier. */
    private Long id;

    /** Annotation the flag was raised against. */
    private Long annotationId;

    /** Item that was flagged. */
    private Long itemId;

    /** Annotator who raised the flag. */
    private Long annotatorId;

    /** Why the item was flagged. */
    private FlagReason reason;

    /** The annotator's free-text explanation. */
    private String comment;

    /** True once an adjudicator has dropped the item from the dataset. */
    private boolean excluded;

    /** When the flag was raised. */
    private Instant createdAt;

    /** Creates an empty Flag. */
    public Flag() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAnnotationId() {
        return annotationId;
    }

    public void setAnnotationId(Long annotationId) {
        this.annotationId = annotationId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getAnnotatorId() {
        return annotatorId;
    }

    public void setAnnotatorId(Long annotatorId) {
        this.annotatorId = annotatorId;
    }

    public FlagReason getReason() {
        return reason;
    }

    public void setReason(FlagReason reason) {
        this.reason = reason;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isExcluded() {
        return excluded;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
