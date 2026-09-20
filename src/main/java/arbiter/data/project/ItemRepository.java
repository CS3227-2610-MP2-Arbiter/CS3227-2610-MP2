package arbiter.data.project;

import java.util.List;
import java.util.Optional;

import arbiter.model.project.Item;

/** Stores imported items. */
public interface ItemRepository {
    /** Inserts or updates an item and returns the stored copy. */
    Item save(Item item);

    /** Returns the item with this identifier, if any. */
    Optional<Item> findById(long id);

    /** Returns a project's items. */
    List<Item> listByProject(long projectId);

    /** Finds an item by content hash, so a re-import does not duplicate it. */
    Optional<Item> findByProjectAndHash(long projectId, String contentHash);

    /** Counts a project's items. */
    long countByProject(long projectId);
}
