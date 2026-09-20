package arbiter.model.project;

import java.time.Instant;

/**
 * A batch of items cut from the corpus, which is what gets assigned.
 *
 * <p>A split names a set of items rather than copying them: {@code SplitItem} joins this to the
 * items, so an item can be moved or removed without rewriting the split.
 */
public class Split {
    /** Database identifier. */
    private Long id;

    /** Project this split belongs to. */
    private Long projectId;

    /** Split name. */
    private String name;

    /**
     * How many annotators see each item, defaulting to 2. This is a property of the work rather than
     * of one annotator's link to it: every annotator on the split sees the same items, so *k* belongs
     * here, not on the assignment.
     */
    private Integer annotationsPerItem;

    /** Allocation strategy the split was cut with, null when it was cut by hand. */
    private SplitStrategy strategy;

    /**
     * Seed the allocation used, null when none was given. Rule 7 requires a seeded split to be
     * reproducible, so the seed is stored rather than asked for again.
     */
    private Long seed;

    /** Amount paid for each item, fixed once the split is assigned. */
    private Double itemReward;

    /** True once assigned, after which nothing about the split changes. */
    private boolean assigned;

    /** When the split was created. */
    private Instant createdAt;

    /** Creates an empty Split. */
    public Split() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Integer getAnnotationsPerItem() {
        return annotationsPerItem;
    }

    public void setAnnotationsPerItem(Integer annotationsPerItem) {
        this.annotationsPerItem = annotationsPerItem;
    }

    public SplitStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(SplitStrategy strategy) {
        this.strategy = strategy;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getItemReward() {
        return itemReward;
    }

    public void setItemReward(Double itemReward) {
        this.itemReward = itemReward;
    }

    public boolean isAssigned() {
        return assigned;
    }

    public void setAssigned(boolean assigned) {
        this.assigned = assigned;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
