package arbiter.model.project;

/** The settings a taxonomy needs, kept out of Project so a project is not a bag of optional numbers. */
public class TaxonomySettings {
    /** Whether the label set is a pick-one list or a numeric scale. */
    private TaxonomyKind kind;

    /** Lowest value of a SCALE taxonomy, null otherwise. */
    private Integer scaleMin;

    /** Highest value of a SCALE taxonomy, null otherwise. */
    private Integer scaleMax;

    /** Creates an empty TaxonomySettings. */
    public TaxonomySettings() {
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
