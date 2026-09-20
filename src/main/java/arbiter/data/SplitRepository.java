package arbiter.data;

import java.util.List;
import java.util.Optional;

import arbiter.model.Split;

/** Stores splits, the batches of items that get assigned. */
public interface SplitRepository {
    /** Inserts or updates a split and returns the stored copy. */
    Split save(Split split);

    /** Returns the split with this identifier, if any. */
    Optional<Split> findById(long id);

    /** Returns a project's splits. */
    List<Split> findByProject(long projectId);

    /** Removes a split that has not been assigned. */
    void deleteById(long id);
}
