package arbiter.service;

import arbiter.data.json.RepositorySession;

/**
 * The one check of whether a project, or one of its splits, has had its first assignment. A project's
 * freezes its corpus and taxonomy (rule 3) and ends the chance to delete it (rule 5), and a split's locks
 * that split's definition (rule 14).
 */
final class FirstAssignment {
    private FirstAssignment() {
    }

    /**
     * Requires the project to exist and none of its splits to have had its first assignment. Call it inside
     * the write action it guards.
     *
     * @param frozenMessage the message to show if the project has had its first assignment
     * @throws ProjectException if no project has this identifier, or it has had its first assignment
     */
    static void requireNotReached(RepositorySession session, long projectId, String frozenMessage) {
        if (session.projects().findById(projectId).isEmpty()) {
            throw new ProjectException("This project no longer exists");
        }
        if (reachedProject(session, projectId)) {
            throw new ProjectException(frozenMessage);
        }
    }

    /**
     * Returns whether any of the project's splits has had its first assignment, as {@link #reachedSplit} decides.
     * When it guards a write, call it inside that write action.
     */
    static boolean reachedProject(RepositorySession session, long projectId) {
        return session.splits().listByProject(projectId).stream()
                .anyMatch(split -> reachedSplit(session, split.getId()));
    }

    /**
     * Returns whether this split has had its first assignment, whatever that assignment's status or its
     * annotator's account status. When it guards a write, call it inside that write action.
     */
    static boolean reachedSplit(RepositorySession session, long splitId) {
        return !session.assignments().listBySplit(splitId).isEmpty();
    }
}
