package arbiter.model.project;

import java.time.Instant;

/**
 * A batch of items cut from the corpus, which is what gets assigned.
 *
 * <p>A split names a set of items rather than copying them: {@code SplitItem} joins this to the
 * items. That saved membership and order is the split's work; reopening or assigning it never reruns
 * the allocation (rule 7).
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

    /**
     * Seed the allocation's shuffle actually used, including one the app generated because none was
     * supplied. The same input items, requested batch size and seed give the same membership, but a
     * seed does not rebuild an earlier corpus (rule 7).
     */
    private Long seed;

    /**
     * Items per batch the adjudicator asked for. The last batch of a generation can be smaller, so
     * this cannot be read back from the membership: 120 items at 50 per batch end with a batch of 20.
     */
    private Integer requestedBatchSize;

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

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public Integer getRequestedBatchSize() {
        return requestedBatchSize;
    }

    public void setRequestedBatchSize(Integer requestedBatchSize) {
        this.requestedBatchSize = requestedBatchSize;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
