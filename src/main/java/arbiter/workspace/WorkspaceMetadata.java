package arbiter.workspace;

import java.time.Instant;

/**
 * The contents of {@code workspace.json}.
 *
 * @param workspaceVersion the version of the folder layout and of this file
 * @param created when the workspace was created
 */
public record WorkspaceMetadata(int workspaceVersion, Instant created) {
    /** The layout version this build writes. */
    public static final int CURRENT_WORKSPACE_VERSION = 1;

    /** Returns metadata for a workspace created now. */
    public static WorkspaceMetadata createNow() {
        return new WorkspaceMetadata(CURRENT_WORKSPACE_VERSION, Instant.now());
    }
}
