package arbiter.model;

import java.time.Instant;

/** A batch of items cut from the corpus, which is what gets assigned. */
public class Split {
    /** Database identifier. */
    private Long id;

    /** Project this split belongs to. */
    private Long projectId;

    /** Split name. */
    private String name;

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
