package arbiter.model.annotation;

import java.time.Instant;

/**
 * An annotator's report that an item's source material is unusable.
 *
 * <p>A flag records the report only. Deciding that a flagged item leaves the dataset is a separate
 * act, done by retiring the item, so there is no second exclusion state here to fall out of step
 * with {@code Item.retired}.
 */
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
