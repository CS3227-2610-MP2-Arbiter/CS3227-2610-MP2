package arbiter.data.project;

import java.util.List;
import java.util.Optional;

import arbiter.model.project.Split;

/** Stores splits, the batches of items that get assigned. */
public interface SplitRepository {
    /** Inserts or updates a split and returns the stored copy. */
    Split save(Split split);

    /** Returns the split with this identifier, if any. */
    Optional<Split> findById(long id);

    /** Returns a project's splits. */
    List<Split> listByProject(long projectId);

    /** Removes a split and its memberships before that split's first assignment (rule 14). */
    void deleteById(long id);
}
