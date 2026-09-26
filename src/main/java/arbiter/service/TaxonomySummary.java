package arbiter.service;

import java.util.List;
import java.util.OptionalInt;

import arbiter.model.project.Label;
import arbiter.model.project.TaxonomyKind;

/**
 * A project's taxonomy as the adjudicator's taxonomy view (#26) and the annotator's queue (#14) show it, with the
 * rules for a valid answer, which the annotator's screen and submission (#17) both apply.
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
    /** Returns whether an answer can be given at all: SINGLE needs labels, and SCALE needs its range. */
    public boolean answerable() {
        return kind == TaxonomyKind.SINGLE ? !labels.isEmpty() : scaleMin != null && scaleMax != null;
    }

    /** Returns whether an answer follows the taxonomy: a project label for SINGLE, a rating in range for SCALE. */
    public boolean accepts(Answer answer) {
        return switch (answer) {
        case Answer.LabelChoice choice -> kind == TaxonomyKind.SINGLE
                && labels.stream().anyMatch(label -> label.getId() == choice.labelId());
        case Answer.Rating rating -> kind == TaxonomyKind.SCALE && answerable()
                && rating.value() >= scaleMin && rating.value() <= scaleMax;
        };
    }

    /**
     * Returns the rating this text gives, parsed as {@link WholeNumbers#parse} does, if it is within a SCALE
     * project's range.
     *
     * @return the rating, or empty if the text is not a valid rating for this taxonomy
     */
    public OptionalInt parseRating(String text) {
        OptionalInt value = WholeNumbers.parse(text);
        return value.isPresent() && accepts(new Answer.Rating(value.getAsInt())) ? value : OptionalInt.empty();
    }
}
