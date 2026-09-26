package arbiter.service;

/**
 * An annotator's current choice for one file (#14): one label or one rating, and nothing else can be built. It is
 * only screen state until submission (#17) stores it.
 */
public sealed interface Answer permits Answer.LabelChoice, Answer.Rating {
    /**
     * A chosen label, for a SINGLE project.
     *
     * @param labelId the label's identifier
     */
    record LabelChoice(long labelId) implements Answer {
    }

    /**
     * A rating, for a SCALE project.
     *
     * @param value the whole number chosen
     */
    record Rating(int value) implements Answer {
    }
}
