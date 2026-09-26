package arbiter.data.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.service.AuthException;
import arbiter.service.AuthService;
import arbiter.testing.ClassificationWorkflow;
import arbiter.testing.TestWorkspace;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;

/** Checks owner setup and password reset when publishing the JSON snapshot fails. */
class AuthPublicationTest {
    @TempDir
    Path temporary;

    @Test
    void bootstrapOwner_publicationFails_reopenAllowsOneSuccessfulRetry() {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        JsonStore.initializeNew(paths);
        AuthService failing = new AuthService(failingStore(paths));

        assertThrows(JsonStoreException.class, () -> failing.bootstrapOwner("owner", "password8"));

        AuthService retried = new AuthService(JsonStore.open(paths));
        assertTrue(retried.needsBootstrap());
        retried.bootstrapOwner("owner", "password8");
        assertEquals(1, JsonStore.open(paths).<Integer>read(session -> session.users().listAll().size()));
    }

    @Test
    void resetAnnotatorPassword_publicationFails_oldPasswordWorksAfterReopen() {
        TestWorkspace workspace = TestWorkspace.create(temporary.resolve("workspace"));
        long amyId = ClassificationWorkflow.single("yes").annotator("amy").seed(workspace).annotatorId("amy");
        AuthService failing = new AuthService(failingStore(workspace.paths()));
        failing.login(TestWorkspace.OWNER, TestWorkspace.PASSWORD);

        assertThrows(JsonStoreException.class, () -> failing.resetAnnotatorPassword(amyId, "newpass8"));

        AuthService reopened = new AuthService(JsonStore.open(workspace.paths()));
        assertEquals(amyId, reopened.login("amy", TestWorkspace.PASSWORD).id());
        assertEquals("Invalid username or password",
                assertThrows(AuthException.class, () -> reopened.login("amy", "newpass8")).getMessage());
    }

    private static JsonStore failingStore(WorkspacePaths paths) {
        return JsonStore.open(paths, (target, bytes) -> {
            throw new IOException("injected publication failure");
        });
    }
}
