package arbiter.model.project;

import java.time.Instant;

/**
 * A batch of items cut from the corpus, which is what gets assigned.
 *
 * <p>Its items and their order are the {@code SplitItem} records that name them (rule 7).
 */
public class Split {
    /** Persistent identifier. */
    private Long id;

    /** Project this split belongs to. */
    private Long projectId;

    /** Split name. */
    private String name;

    /**
     * How many annotators see each item (k). It belongs to the split rather than the assignment
     * because every annotator on a split sees the same items.
     */
    private Integer annotationsPerItem;

    /** Seed the allocation's shuffle actually used, including one the app generated. */
    private Long seed;

    /**
     * Items per batch the adjudicator asked for, which the membership cannot show because the last
     * batch of a generation can be smaller.
     */
    private Integer requestedBatchSize;

    /** True once the split has been assigned, which locks it (rule 14). */
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
