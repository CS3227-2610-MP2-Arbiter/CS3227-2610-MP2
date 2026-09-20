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
     * Removes a label and every annotation that used it, in one transaction, returning those
     * items to unannotated. Labels are the one exception to the soft-delete rule (rule 5): a
     * deleted label is genuinely gone rather than retired.
     */
    void deleteById(long id);
}
