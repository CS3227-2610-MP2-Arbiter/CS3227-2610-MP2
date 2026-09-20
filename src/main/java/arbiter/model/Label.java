package arbiter.model;

/** One answer an annotator can choose. A project's labels are its taxonomy. */
public class Label {
    /** Database identifier. */
    private Long id;

    /** Project this label belongs to. */
    private Long projectId;

    /** Label text shown to the annotator. */
    private String name;

    /** Short stable key used in exports. */
    private String key;

    /** Colour used to draw the label. */
    private String colour;

    /** What the label means. */
    private String description;

    /** Guidance shown while annotating. */
    private String guideline;

    /** Position in the label set, so order is stable. */
    private Integer sequence;

    /** False once the label has been deleted. */
    private boolean active;

    /** Creates an empty Label. */
    public Label() {
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGuideline() {
        return guideline;
    }

    public void setGuideline(String guideline) {
        this.guideline = guideline;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
