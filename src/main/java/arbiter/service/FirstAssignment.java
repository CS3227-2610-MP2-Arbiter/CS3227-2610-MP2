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
     * Requires the project to exist and none of its splits to have an assignment, whatever its status
     * or its annotator's account status. Call it inside the write action it guards.
     *
     * @param frozenMessage the message to show if the project has had its first assignment
     * @throws ProjectException if no project has this identifier, or it has had its first assignment
     */
    static void requireNotReached(RepositorySession session, long projectId, String frozenMessage) {
        if (session.projects().findById(projectId).isEmpty()) {
            throw new ProjectException("This project no longer exists");
        }
        boolean reached = session.splits().listByProject(projectId).stream()
                .anyMatch(split -> !session.assignments().listBySplit(split.getId()).isEmpty());
        if (reached) {
            throw new ProjectException(frozenMessage);
        }
    }
}
