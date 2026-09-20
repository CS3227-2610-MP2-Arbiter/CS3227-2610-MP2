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

    /** Removes a label and the annotations that used it. */
    void deleteById(long id);
}
