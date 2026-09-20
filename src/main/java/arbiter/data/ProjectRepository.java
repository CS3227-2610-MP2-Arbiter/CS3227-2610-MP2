package arbiter.data;

import java.util.List;
import java.util.Optional;

import arbiter.model.Project;

/** Stores projects. */
public interface ProjectRepository {
    /** Inserts or updates a project and returns the stored copy. */
    Project save(Project project);

    /** Returns the project with this identifier, if any. */
    Optional<Project> findById(long id);

    /** Returns every project. */
    List<Project> findAll();

    /** Removes a project and everything recorded under it. */
    void deleteById(long id);
}
