package arbiter.service;

/**
 * An annotator's current choice for one file: exactly one of a label or a rating (#14). It is only screen
 * state until submission (#17) stores it.
 *
 * @param labelId the chosen label, for a SINGLE project, or null
 * @param rating the chosen rating, for a SCALE project, or null
 */
public record Answer(Long labelId, Integer rating) {
    /** Requires exactly one of a label or a rating. */
    public Answer {
        if ((labelId == null) == (rating == null)) {
            throw new IllegalArgumentException("An answer is exactly one label or one rating");
        }
    }

    /** Returns an answer choosing this label. */
    public static Answer label(long labelId) {
        return new Answer(labelId, null);
    }

    /** Returns an answer giving this rating. */
    public static Answer rating(int rating) {
        return new Answer(null, rating);
    }
}
