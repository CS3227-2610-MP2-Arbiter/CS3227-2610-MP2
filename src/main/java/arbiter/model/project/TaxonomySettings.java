package arbiter.model.project;

/**
 * The settings a taxonomy needs, kept out of Project so a project is not a bag of optional numbers.
 *
 * <p>{@code projectId} links these settings to their project. {@code Project} holds no reference back.
 */
public class TaxonomySettings {
    /** The lowest value a SCALE range may include (#26). */
    public static final int LOWEST_SCALE_VALUE = -10;

    /** The highest value a SCALE range may include (#26). */
    public static final int HIGHEST_SCALE_VALUE = 10;

    /** Persistent identifier. */
    private Long id;

    /** Project these settings belong to. */
    private Long projectId;

    /** Whether the label set is a pick-one list or a numeric scale, fixed when the project is created (rule 4). */
    private TaxonomyKind kind;

    /** Lowest value of a SCALE project's saved range, null if no range is saved. */
    private Integer scaleMin;

    /** Highest value of a SCALE project's saved range, null if no range is saved. */
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
