package arbiter.service;

/**
 * The file an annotator's queue is on (#13): its text, or why its source cannot be read (rule 21).
 *
 * @param itemId the item's identifier
 * @param storedPath the file's workspace-relative path
 * @param text the file's text, or null if its source cannot be read
 * @param sourceError the source resolver's message about the file, or null if its text was read
 */
public record QueueItem(long itemId, String storedPath, String text, String sourceError) {
    /** Returns whether the file's text was read, so it can be answered. */
    public boolean readable() {
        return sourceError == null;
    }
}
