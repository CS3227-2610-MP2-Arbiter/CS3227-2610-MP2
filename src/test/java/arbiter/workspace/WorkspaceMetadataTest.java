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

        assertEquals(WorkspaceMetadata.CURRENT_WORKSPACE_VERSION, metadata.workspaceVersion());
        assertEquals(WorkspaceMetadata.CURRENT_SCHEMA_VERSION, metadata.schemaVersion());
        assertFalse(metadata.created().isBefore(before), "created before the call");
        assertFalse(metadata.created().isAfter(after), "created after the call");
    }
}
