package arbiter.service;

/**
 * Where one of the signed-in annotator's assignments stands in its queue (#13).
 *
 * @param assignment the assignment's progress, as the annotator's home shows it
 * @param current the first file in saved split order without the annotator's answer, or null once every file is
 *     answered
 */
public record QueueView(AssignmentProgress assignment, QueueItem current) {
    /** Returns whether every file has the annotator's answer, so the queue shows completion. */
    public boolean finished() {
        return current == null;
    }
}
