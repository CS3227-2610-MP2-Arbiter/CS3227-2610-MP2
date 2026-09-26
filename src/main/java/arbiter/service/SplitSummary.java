package arbiter.service;

import java.util.List;

/**
 * One split as the adjudicator's project page shows it (#28, #32).
 *
 * @param id the split's identifier
 * @param name the split's name
 * @param itemIds the identifiers of the split's items in saved order
 * @param assignmentCount the split's assignments, in every status and whatever their annotator's account status,
 *     each of which takes one of its places (rule 19)
 * @param annotationsPerItem the split's k, saved by its first assignment, or null before then
 */
public record SplitSummary(long id, String name, List<Long> itemIds, int assignmentCount,
        Integer annotationsPerItem) {
    /** Returns whether the split has had its first assignment, which locks it (rule 14). */
    public boolean assigned() {
        return assignmentCount > 0;
    }

    /** Returns whether every one of the split's k places is taken, so no annotator can be added (rule 19). */
    public boolean full() {
        return annotationsPerItem != null && assignmentCount >= annotationsPerItem;
    }
}
