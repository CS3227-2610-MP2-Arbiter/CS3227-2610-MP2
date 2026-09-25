package arbiter.service;

import arbiter.data.json.RepositorySession;

/**
 * The one check of whether a project has had its first assignment, which freezes its corpus and
 * taxonomy (rule 3) and ends the chance to delete it (rule 5).
 */
final class FirstAssignment {
    private FirstAssignment() {
    }

    /**
     * Returns whether any of the project's splits has an assignment, whatever its status or its
     * annotator's account status. Call it inside the write action it guards.
     */
    static boolean reached(RepositorySession session, long projectId) {
        return session.splits().listByProject(projectId).stream()
                .anyMatch(split -> !session.assignments().listBySplit(split.getId()).isEmpty());
    }
}
