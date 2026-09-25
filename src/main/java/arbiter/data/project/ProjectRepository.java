package arbiter.data.project;

import java.util.List;
import java.util.Optional;

import arbiter.model.project.Project;

/** Stores projects. */
public interface ProjectRepository {
    /** Inserts or updates a project and returns the stored copy. */
    Project save(Project project);

    /** Returns the project with this identifier, if any. */
    Optional<Project> findById(long id);

    /** Returns every project. */
    List<Project> listAll();

    /**
     * Removes a project and every stored record it owns, assignments included. It does not check
     * whether deletion is allowed (rule 5), so callers must. Source files under {@code media/} are
     * never touched.
     */
    void deleteById(long id);
}
