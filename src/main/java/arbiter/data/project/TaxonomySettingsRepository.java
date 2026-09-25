package arbiter.data.project;

import java.util.Optional;

import arbiter.model.project.TaxonomySettings;

/**
 * Stores a project's taxonomy settings.
 *
 * <p>Every project has exactly one row, saved with the project and removed only by
 * {@link ProjectRepository#deleteById}; a commit that breaks this is rejected.
 */
public interface TaxonomySettingsRepository {
    /** Inserts or updates the settings and returns the stored copy. */
    TaxonomySettings save(TaxonomySettings settings);

    /** Returns the settings with this identifier, if any. */
    Optional<TaxonomySettings> findById(long id);

    /** Returns a project's settings, or empty if there is no such project. */
    Optional<TaxonomySettings> findByProject(long projectId);
}
