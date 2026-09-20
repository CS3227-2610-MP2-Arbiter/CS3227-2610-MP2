package arbiter.model;

/** How an item's final label was decided, recorded for provenance. */
public enum ResolutionMethod {
    MAJORITY,
    ADJUDICATED,
    GOLD,
    AUTO_SCALE
}
