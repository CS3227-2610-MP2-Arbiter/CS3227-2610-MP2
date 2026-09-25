package arbiter.service;

import arbiter.model.project.OutputFormat;
import arbiter.model.project.TaxonomyKind;

/**
 * One project as the adjudicator's project list shows it (#24).
 *
 * @param id the project's identifier
 * @param name the project's name
 * @param kind the project's taxonomy kind
 * @param outputFormat the project's output format
 * @param itemCount the items imported into the project
 * @param splitCount the project's splits
 * @param assignmentCount the assignments on the project's splits, in every status
 * @param unresolvedCount the project's items that have no {@code Resolution} record
 */
public record ProjectSummary(long id, String name, TaxonomyKind kind, OutputFormat outputFormat, long itemCount,
        long splitCount, long assignmentCount, long unresolvedCount) {
}
