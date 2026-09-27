package arbiter.service;

/**
 * One item in a project's dispute list (#34), a dispute or an item the adjudicator has decided, as the list shows
 * it. It names no annotator and no time.
 *
 * @param itemId the item's identifier
 * @param path the item's stored path
 * @param splitName the name of the item's split
 * @param position the item's position in its split, counting from 1
 * @param decisionKey the key of the label the adjudicator decided on, or null if it is undecided
 */
public record DisputeSummary(long itemId, String path, String splitName, int position, String decisionKey) {
    /** Returns whether the adjudicator has decided the item. */
    public boolean decided() {
        return decisionKey != null;
    }
}
