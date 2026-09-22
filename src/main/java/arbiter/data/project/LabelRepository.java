package arbiter.data.project;

import java.util.List;
import java.util.Optional;

import arbiter.model.project.Label;

/** Stores a project's labels. */
public interface LabelRepository {
    /** Inserts or updates a label and returns the stored copy. */
    Label save(Label label);

    /** Returns the label with this identifier, if any. */
    Optional<Label> findById(long id);

    /** Returns a project's labels in sequence order. */
    List<Label> listByProject(long projectId);

    /**
     * Removes a label before the project's first assignment. A label that any answer or resolution
     * references is rejected, never cascaded (rule 5).
     */
    void deleteById(long id);
}
