package arbiter.data.project;

import java.util.List;
import java.util.Optional;

import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;

/** Stores the link between one split and one annotator. */
public interface AssignmentRepository {
    /** Inserts or updates an assignment and returns the stored copy. */
    Assignment save(Assignment assignment);

    /** Returns the assignment with this identifier, if any. */
    Optional<Assignment> findById(long id);

    /** Returns one annotator's own assignments and nobody else's, the read path blindness depends on. */
    List<Assignment> listByAnnotator(long annotatorId);

    /** Returns everyone assigned to a split. */
    List<Assignment> listBySplit(long splitId);

    /** Returns assignments with the given status. */
    List<Assignment> listByStatus(AssignmentStatus status);

    /** Removes an assignment that has no submitted work. */
    void deleteById(long id);
}
