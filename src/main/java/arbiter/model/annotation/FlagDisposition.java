package arbiter.model.annotation;

/** What an adjudicator decided to do with a flagged item (#35). */
public enum FlagDisposition {
    /** Not yet reviewed, so the item still sits in the flagged queue. */
    PENDING,
    /** Dropped from active work and export by retiring the item while retaining its evidence. */
    EXCLUDED,
    /** Kept as-is: the flag was raised but the adjudicator disagreed with it. */
    KEPT
}
