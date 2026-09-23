package arbiter.model.project;

import java.time.Instant;

/**
 * One text-classification job and the settings that shape everything under it.
 *
 * <p>{@code outputFormat} is fixed at creation (rule 4).
 */
public class Project {
    /** Persistent identifier. */
    private Long id;

    /** Project name. */
    private String name;

    /** What the project is for. */
    private String description;

    /** Format the finished dataset is written in. */
    private OutputFormat outputFormat;

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

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
