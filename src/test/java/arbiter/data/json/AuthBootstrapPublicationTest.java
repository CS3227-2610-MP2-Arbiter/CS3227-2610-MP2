package arbiter.data.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.service.AuthService;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;

/** Checks owner setup when publishing the JSON snapshot fails. */
class AuthBootstrapPublicationTest {
    @TempDir
    Path temporary;

    @Test
    void bootstrapOwner_publicationFails_reopenAllowsOneSuccessfulRetry() {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        JsonStore.initializeNew(paths);
        JsonStore failingStore = JsonStore.open(paths, (target, bytes) -> {
            throw new IOException("injected publication failure");
        });
        AuthService failing = new AuthService(failingStore);

        assertThrows(JsonStoreException.class, () -> failing.bootstrapOwner("owner", "password8"));

        AuthService retried = new AuthService(JsonStore.open(paths));
        assertTrue(retried.needsBootstrap());
        retried.bootstrapOwner("owner", "password8");
        assertEquals(1, JsonStore.open(paths).<Integer>read(session -> session.users().listAll().size()));
    }
}
