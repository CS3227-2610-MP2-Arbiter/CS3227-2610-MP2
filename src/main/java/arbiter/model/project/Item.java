package arbiter.model.project;

import java.time.Instant;

/** One annotatable unit: an image, or a plain-text document. */
public class Item {
    /** Database identifier. */
    private Long id;

    /** Project this item belongs to. */
    private Long projectId;

    /** Workspace-relative location of the source file under {@code media/}, such as {@code media/corpus/cat.jpg}. */
    private String path;

    /** Hash of the bytes registered at import, used to reject duplicates and detect a changed source file. */
    private String contentHash;

    /** True once the item is excluded, which drops it from work and the export while keeping its records. */
    private boolean retired;

    /** When the item was imported. */
    private Instant importedAt;

    /** Creates an empty Item. */
    public Item() {
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(Instant importedAt) {
        this.importedAt = importedAt;
    }
}
