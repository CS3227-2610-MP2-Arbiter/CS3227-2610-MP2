package arbiter.service;

import java.util.List;

/**
 * One split as the adjudicator's project page shows it (#28).
 *
 * @param id the split's identifier
 * @param name the split's name
 * @param itemIds the identifiers of the split's items in saved order
 * @param assigned whether the split has an assignment, in any status, which locks it (rule 14)
 */
public record SplitSummary(long id, String name, List<Long> itemIds, boolean assigned) {
}
