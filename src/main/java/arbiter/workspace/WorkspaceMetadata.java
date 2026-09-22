package arbiter.workspace;

import java.time.Instant;

/**
 * The contents of {@code workspace.json}.
 *
 * <p>The two versions are deliberately separate. {@code workspaceVersion} covers the folder layout
 * and this file; {@code schemaVersion} covers the database and is written by whoever owns the schema.
 *
 * @param workspaceVersion the layout version
 * @param schemaVersion the database schema version
 * @param created when the workspace was created
 */
public record WorkspaceMetadata(int workspaceVersion, int schemaVersion, Instant created) {
    /** The layout version this build writes. */
    public static final int CURRENT_WORKSPACE_VERSION = 1;

    /** The database schema version this build expects. */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    /** Returns metadata for a workspace created now. */
    public static WorkspaceMetadata createNow() {
        return new WorkspaceMetadata(CURRENT_WORKSPACE_VERSION, CURRENT_SCHEMA_VERSION, Instant.now());
    }
}
