package arbiter.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class WorkspaceMetadataTest {
    @Test
    void createNow() {
        Instant before = Instant.now();
        WorkspaceMetadata metadata = WorkspaceMetadata.createNow();
        Instant after = Instant.now();

        assertEquals(1, WorkspaceMetadata.CURRENT_WORKSPACE_VERSION);
        assertEquals(WorkspaceMetadata.CURRENT_WORKSPACE_VERSION, metadata.workspaceVersion());
        assertFalse(metadata.created().isBefore(before), "created before the call");
        assertFalse(metadata.created().isAfter(after), "created after the call");
    }
}
