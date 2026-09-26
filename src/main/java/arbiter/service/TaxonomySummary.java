package arbiter.service;

import java.util.List;

import arbiter.model.project.Label;
import arbiter.model.project.TaxonomyKind;

/**
 * A project's taxonomy as the adjudicator's taxonomy view shows it (#26).
 *
 * @param kind the project's taxonomy kind
 * @param labels the project's stored labels in order, which {@link CorpusService} adds only to a SINGLE project
 * @param scaleMin the project's saved minimum, or null if it has no range, which {@link CorpusService} saves only
 *     for a SCALE project
 * @param scaleMax the project's saved maximum, or null if it has no range
 * @param frozen whether the project has had its first assignment, which freezes its taxonomy (rule 3)
 */
public record TaxonomySummary(TaxonomyKind kind, List<Label> labels, Integer scaleMin, Integer scaleMax,
        boolean frozen) {
}
