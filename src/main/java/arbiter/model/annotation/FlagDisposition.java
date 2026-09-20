package arbiter.model.annotation;

/** What an adjudicator decided to do with a flagged item (#35). */
public enum FlagDisposition {
    /** Not yet reviewed, so the item still sits in the flagged queue. */
    PENDING,
    /** Dropped from the dataset: the item is retired, so it leaves the export and earnings. */
    EXCLUDED,
    /** Kept, with the flag acknowledged and the label corrected if needed. */
    REPAIRED,
    /** Kept as-is: the flag was raised but the adjudicator disagreed with it. */
    KEPT
}
