package arbiter.model.annotation;

import java.time.Instant;

/**
 * An annotator's report that an item's source material is unusable.
 *
 * <p>The flag is the annotator's report. What an adjudicator does about it is a separate decision,
 * recorded as a {@code FlagDisposition} so the flagged queue empties once every flag has been dealt
 * with (#35) and the disposition can be shown per item (#36). Excluding a flag retires its item, so
 * {@code Item.retired} remains the one place that decides whether an item is in the dataset.
 *
 * <p>A flag is submitted with its annotation. Until then it is an editable draft that stays out of
 * review; afterwards its reason and comment are permanent.
 */
public class Flag {
    /** Database identifier. */
    private Long id;

    /** Annotation the flag belongs to, whose submission also submits the flag. */
    private Long annotationId;

    /** Item that was flagged. */
    private Long itemId;

    /** Annotator who raised the flag. */
    private Long annotatorId;

    /** Why the item was flagged. */
    private FlagReason reason;

    /** The annotator's free-text explanation. */
    private String comment;

    /** What an adjudicator decided about this flag, PENDING until someone reviews it. */
    private FlagDisposition disposition;

    /** When an adjudicator reviewed the flag, null while it is still pending. */
    private Instant reviewedAt;

    /** When the flag was first saved; its submission time is the annotation's. */
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

    public FlagDisposition getDisposition() {
        return disposition;
    }

    public void setDisposition(FlagDisposition disposition) {
        this.disposition = disposition;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
