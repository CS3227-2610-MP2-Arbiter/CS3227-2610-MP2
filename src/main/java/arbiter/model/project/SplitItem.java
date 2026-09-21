package arbiter.model.project;

/**
 * One item's membership of one split.
 *
 * <p>A split names its items through this join rather than by copying them. Count-based batching
 * writes every membership and its position once; nothing edits them afterwards. Changing a batch
 * means deleting it before its first assignment and generating a replacement, and retiring an item
 * keeps its membership (rule 14).
 */
public class SplitItem {
    /** Database identifier. */
    private Long id;

    /** Split the item belongs to. */
    private Long splitId;

    /** Item in the split. */
    private Long itemId;

    /** Position in the split, so the queue order is stable. */
    private Integer sequence;

    /** Creates an empty SplitItem. */
    public SplitItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSplitId() {
        return splitId;
    }

    public void setSplitId(Long splitId) {
        this.splitId = splitId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }
}
