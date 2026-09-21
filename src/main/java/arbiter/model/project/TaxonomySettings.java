package arbiter.model.project;

/**
 * The settings a taxonomy needs, kept out of Project so a project is not a bag of optional numbers.
 *
 * <p>This is its own entity rather than a value object embedded in {@code Project}, because ORMLite
 * has no equivalent of JPA's {@code @Embedded}. It has its own identity because ORMLite's update, delete
 * and find-by-id operations need one.
 *
 * <p>{@code projectId} is the only link between the two, in the same direction as {@code Label},
 * {@code Item} and {@code Split}. {@code Project} deliberately holds no reference back, so the
 * relationship cannot be recorded twice and disagree with itself.
 */
public class TaxonomySettings {
    /** Database identifier. */
    private Long id;

    /** Project these settings belong to. */
    private Long projectId;

    /** Whether the label set is a pick-one list or a numeric scale. */
    private TaxonomyKind kind;

    /** Lowest value of a SCALE taxonomy, null otherwise. */
    private Integer scaleMin;

    /** Highest value of a SCALE taxonomy, null otherwise. */
    private Integer scaleMax;

    /** Creates an empty TaxonomySettings. */
    public TaxonomySettings() {
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

    public TaxonomyKind getKind() {
        return kind;
    }

    public void setKind(TaxonomyKind kind) {
        this.kind = kind;
    }

    public Integer getScaleMin() {
        return scaleMin;
    }

    public void setScaleMin(Integer scaleMin) {
        this.scaleMin = scaleMin;
    }

    public Integer getScaleMax() {
        return scaleMax;
    }

    public void setScaleMax(Integer scaleMax) {
        this.scaleMax = scaleMax;
    }
}
