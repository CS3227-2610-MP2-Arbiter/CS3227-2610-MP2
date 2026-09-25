package arbiter.data.project;

import java.util.List;
import java.util.Optional;

import arbiter.model.project.SplitItem;

/** Stores which items belong to which split. */
public interface SplitItemRepository {
    /** Inserts or updates a membership and returns the stored copy. */
    SplitItem save(SplitItem splitItem);

    /** Returns a split's memberships in sequence order. */
    List<SplitItem> listBySplit(long splitId);

    /** Returns an item's membership, if it is in a split (rule 7). */
    Optional<SplitItem> findByItem(long itemId);

    /** Counts a split's items. */
    long countBySplit(long splitId);
}
