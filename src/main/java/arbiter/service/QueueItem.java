package arbiter.service;

import arbiter.workspace.SourceFailure;

/**
 * The file an annotator's queue is on (#13): its text, or why its source cannot be read (rule 21).
 *
 * <p>It carries no path, because a file's name or folder can hint at a label, and an annotator never needs it.
 *
 * @param itemId the item's identifier
 * @param text the file's text, or null if its source cannot be read
 * @param failure why the source cannot be read, or null if its text was read
 */
public record QueueItem(long itemId, String text, SourceFailure failure) {
    /** Returns whether the file's text was read, so it can be answered. */
    public boolean readable() {
        return failure == null;
    }
}
