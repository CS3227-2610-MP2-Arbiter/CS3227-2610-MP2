package arbiter.service;

import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Pattern;

import arbiter.model.project.TaxonomyKind;

/**
 * A project's frozen taxonomy as an annotator answers against it, and the rules for a valid answer (#14). The
 * screen and submission (#17) both apply these rules, so what the screen allows is what is stored.
 *
 * @param kind whether an answer is one label or one rating
 * @param labels a SINGLE project's labels in their saved order; empty for SCALE
 * @param scaleMin the lowest rating of a SCALE project, or null if its range is not set
 * @param scaleMax the highest rating of a SCALE project, or null if its range is not set
 */
public record Taxonomy(TaxonomyKind kind, List<LabelOption> labels, Integer scaleMin, Integer scaleMax) {
    private static final Pattern RATING = Pattern.compile("-?[0-9]+");

    /** Keeps an unchangeable copy of the labels. */
    public Taxonomy {
        labels = List.copyOf(labels);
    }

    /** Returns whether an answer can be given at all: SINGLE needs labels, and SCALE needs its range. */
    public boolean answerable() {
        return kind == TaxonomyKind.SINGLE ? !labels.isEmpty() : scaleMin != null && scaleMax != null;
    }

    /** Returns whether this label is one of a SINGLE project's labels. */
    public boolean accepts(long labelId) {
        return kind == TaxonomyKind.SINGLE && labels.stream().anyMatch(label -> label.id() == labelId);
    }

    /**
     * Returns the rating this text gives for a SCALE project: ASCII digits with an optional leading minus sign,
     * once surrounding whitespace is stripped, within the inclusive range.
     *
     * @return the rating, or empty if the text is not a valid rating, including for a SINGLE project
     */
    public OptionalInt parseRating(String text) {
        if (kind != TaxonomyKind.SCALE || !answerable() || text == null) {
            return OptionalInt.empty();
        }
        String stripped = text.strip();
        if (!RATING.matcher(stripped).matches()) {
            return OptionalInt.empty();
        }
        try {
            int rating = Integer.parseInt(stripped);
            return rating >= scaleMin && rating <= scaleMax ? OptionalInt.of(rating) : OptionalInt.empty();
        } catch (NumberFormatException e) {
            // The pattern leaves only a number too large for an int, which is outside every range.
            return OptionalInt.empty();
        }
    }
}
