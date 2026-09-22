package arbiter.data.resolution;

import java.util.List;
import java.util.Optional;

import arbiter.model.resolution.Resolution;

/** Stores the final answer for each item. */
public interface ResolutionRepository {
    /** Inserts or updates a resolution and returns the stored copy. */
    Resolution save(Resolution resolution);

    /** Returns the resolution for an item, if it has one. */
    Optional<Resolution> findByItem(long itemId);

    /** Returns the resolutions for a batch of items. */
    List<Resolution> listByItems(List<Long> itemIds);
}
