package arbiter.workspace;

import java.time.Instant;

/**
 * The contents of {@code workspace.json}.
 *
 * <p>The two versions are deliberately separate. {@code workspaceVersion} covers the folder layout
 * and this file; {@code schemaVersion} covers the database and is written by whoever owns the schema.
 */
public final class WorkspaceMetadata {
    /** The layout version this build writes. */
    public static final int CURRENT_WORKSPACE_VERSION = 1;

    /** The database schema version this build expects. */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private final int workspaceVersion;
    private final int schemaVersion;
    private final Instant created;

    /**
     * Creates metadata.
     *
     * @param workspaceVersion the layout version
     * @param schemaVersion the database schema version
     * @param created when the workspace was created
     */
    public WorkspaceMetadata(int workspaceVersion, int schemaVersion, Instant created) {
        this.workspaceVersion = workspaceVersion;
        this.schemaVersion = schemaVersion;
        this.created = created;
    }

    /** Returns metadata for a workspace created now. */
    public static WorkspaceMetadata createNow() {
        return new WorkspaceMetadata(CURRENT_WORKSPACE_VERSION, CURRENT_SCHEMA_VERSION, Instant.now());
    }

    /** Returns the layout version. */
    public int getWorkspaceVersion() {
        return workspaceVersion;
    }

    /** Returns the database schema version. */
    public int getSchemaVersion() {
        return schemaVersion;
    }

    /** Returns when the workspace was created. */
    public Instant getCreated() {
        return created;
    }
}
