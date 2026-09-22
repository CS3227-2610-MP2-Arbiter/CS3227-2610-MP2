package arbiter.model.annotation;

/** Why an annotator reported an item's source material as unusable. */
public enum FlagReason {
    CORRUPT_OR_UNREADABLE,
    WRONG_CONTENT,
    TOXIC_OR_SENSITIVE
}
