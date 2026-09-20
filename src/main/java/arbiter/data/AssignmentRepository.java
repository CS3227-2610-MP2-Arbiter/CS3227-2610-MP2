package arbiter.data;

import java.util.List;
import java.util.Optional;

import arbiter.model.Assignment;

/** Stores the link between one split and one annotator. */
public interface AssignmentRepository {
    /** Inserts or updates an assignment and returns the stored copy. */
    Assignment save(Assignment assignment);

    /** Returns the assignment with this identifier, if any. */
    Optional<Assignment> findById(long id);

    /** Returns one annotator's own assignments and nobody else's, the read path blindness depends on. */
    List<Assignment> findByAnnotator(long annotatorId);

    /** Returns everyone assigned to a split. */
    List<Assignment> findBySplit(long splitId);

    /** Removes an assignment that has no submitted work. */
    void deleteById(long id);
}
