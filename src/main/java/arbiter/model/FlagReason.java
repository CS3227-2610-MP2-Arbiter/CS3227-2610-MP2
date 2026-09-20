package arbiter.model;

/** Why an annotator flagged an item as unusable. */
public enum FlagReason {
    CORRUPT_OR_UNREADABLE,
    WRONG_CONTENT,
    DUPLICATE,
    OFF_TOPIC,
    TOXIC_OR_SENSITIVE,
    INSTRUCTIONS_UNCLEAR
}
