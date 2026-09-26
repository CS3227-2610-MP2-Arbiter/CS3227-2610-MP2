package arbiter.service;

/**
 * Where one of the signed-in annotator's assignments stands in its queue (#13).
 *
 * @param assignment the assignment's progress, as the annotator's home shows it
 * @param current the first file in saved split order without the annotator's answer, or null exactly when the
 *     assignment is {@link AssignmentProgress#finished() finished}
 * @param taxonomy the project's taxonomy, which an answer to {@code current} must follow (#14)
 */
public record QueueView(AssignmentProgress assignment, QueueItem current, TaxonomySummary taxonomy) {
    /**
     * Returns the number of the file the queue is on, counting from 1: one past the files already answered. It
     * describes {@code current}, so it means nothing once the assignment is finished.
     */
    public int position() {
        return assignment.submitted() + 1;
    }
}
