package arbiter.service;

import java.util.List;

/**
 * What a split's assignment form on the adjudicator's project page shows (#32).
 *
 * @param annotationsPerItem the split's k, saved by its first assignment, or null before then
 * @param activeAnnotators the workspace's active annotators, the most k can be before it is saved
 * @param assignees the accounts of the split's assignees in assignment order, disabled ones included
 * @param offered the active annotators not assigned to the split, in account creation order, never the owner
 */
public record AssignmentOptions(Integer annotationsPerItem, long activeAnnotators, List<AccountSummary> assignees,
        List<AnnotatorLoad> offered) {
    /**
     * Returns the k the form shows: the saved one, or before the first assignment
     * {@link AssignmentService#DEFAULT_ANNOTATIONS_PER_ITEM}, lowered to the number of active annotators but
     * never below 1.
     */
    public int prefilledAnnotationsPerItem() {
        if (annotationsPerItem != null) {
            return annotationsPerItem;
        }
        return Math.clamp(activeAnnotators, 1, AssignmentService.DEFAULT_ANNOTATIONS_PER_ITEM);
    }
}
