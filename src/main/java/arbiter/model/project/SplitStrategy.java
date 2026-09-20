package arbiter.model.project;

/** How a split's items were chosen, kept so a seeded split can be reproduced. */
public enum SplitStrategy {
    BY_COUNT,
    BY_PROPORTION,
    MANUAL
}
