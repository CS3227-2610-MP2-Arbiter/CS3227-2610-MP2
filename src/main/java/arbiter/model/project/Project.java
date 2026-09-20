package arbiter.model.project;

import java.time.Instant;

/** One labelling job and the settings that shape everything under it. */
public class Project {
    /** Database identifier. */
    private Long id;

    /** Project name. */
    private String name;

    /** What the project is for. */
    private String description;

    /** Classification or detection, fixed at creation. */
    private TaskType taskType;

    /** Image or text, fixed at creation. */
    private SourceType sourceType;

    /** Format the finished dataset is written in, fixed at creation. */
    private OutputFormat outputFormat;

    /** The label set's shape and, for a scale, its range. */
    private TaxonomySettings taxonomy;

    /** Whether an explanation is required with each label. */
    private boolean rationaleRequired;

    /** Whether the dataset is complete, which releases earnings. */
    private boolean complete;

    /** When the project was created. */
    private Instant createdAt;

    /** Creates an empty Project. */
    public Project() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    public TaxonomySettings getTaxonomy() {
        return taxonomy;
    }

    public void setTaxonomy(TaxonomySettings taxonomy) {
        this.taxonomy = taxonomy;
    }

    public boolean isRationaleRequired() {
        return rationaleRequired;
    }

    public void setRationaleRequired(boolean rationaleRequired) {
        this.rationaleRequired = rationaleRequired;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
