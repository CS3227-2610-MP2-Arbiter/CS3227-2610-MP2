package arbiter.service;

/**
 * Where one of the signed-in annotator's assignments stands in its queue (#13).
 *
 * @param assignment the assignment's progress, as the annotator's home shows it
 * @param current the first file in saved split order without the annotator's answer, or null once the
 *     assignment is finished or every file is answered
 */
public record QueueView(AssignmentProgress assignment, QueueItem current) {
    /** Returns whether the queue has no file to show, so it shows completion. */
    public boolean finished() {
        return current == null;
    }
}
