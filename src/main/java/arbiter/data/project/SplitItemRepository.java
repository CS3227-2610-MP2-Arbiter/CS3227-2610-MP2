package arbiter.data.project;

import java.util.List;

import arbiter.model.project.SplitItem;

/** Stores which items belong to which split. */
public interface SplitItemRepository {
    /** Inserts or updates a membership and returns the stored copy. */
    SplitItem save(SplitItem splitItem);

    /** Returns a split's memberships in sequence order. */
    List<SplitItem> listBySplit(long splitId);

    /** Returns the splits an item belongs to. */
    List<SplitItem> listByItem(long itemId);

    /** Removes one item from a split. */
    void deleteBySplitAndItem(long splitId, long itemId);

    /** Removes every membership of a split that is being deleted. */
    void deleteBySplit(long splitId);

    /** Counts a split's items. */
    long countBySplit(long splitId);
}
