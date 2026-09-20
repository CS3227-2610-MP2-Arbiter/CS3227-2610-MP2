package arbiter.data.project;

import java.util.Optional;

import arbiter.model.project.TaxonomySettings;

/**
 * Stores a project's taxonomy settings.
 *
 * <p>One row per project, so there is a single lookup rather than a list.
 */
public interface TaxonomySettingsRepository {
    /** Inserts or updates the settings and returns the stored copy. */
    TaxonomySettings save(TaxonomySettings settings);

    /** Returns the settings with this identifier, if any. */
    Optional<TaxonomySettings> findById(long id);

    /** Returns the settings for a project, if they have been set. */
    Optional<TaxonomySettings> findByProject(long projectId);

    /** Removes a project's settings, which happens when the project is deleted. */
    void deleteByProject(long projectId);
}
