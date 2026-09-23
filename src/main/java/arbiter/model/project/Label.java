package arbiter.model.project;

/** One answer an annotator can choose. A project's labels are its taxonomy. */
public class Label {
    /** Persistent identifier. */
    private Long id;

    /** Project this label belongs to. */
    private Long projectId;

    /** Short stable key shown to annotators and used in exports. */
    private String key;

    /** What the label means and when to use it. */
    private String description;

    /** Position in the label set, so order is stable. */
    private Integer sequence;

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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

}
