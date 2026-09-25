package arbiter.data.json;

import static arbiter.testing.Records.NOW;
import static arbiter.testing.Records.project;
import static arbiter.testing.Records.settings;
import static arbiter.testing.Records.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.model.annotation.Annotation;
import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.Item;
import arbiter.model.project.Project;
import arbiter.model.project.Split;
import arbiter.model.resolution.Resolution;
import arbiter.model.resolution.ResolutionMethod;
import arbiter.model.user.User;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;

/** Integration checks for the shared JSON commit boundary. */
class JsonStoreTransactionTest {
    @TempDir
    Path temporary;

    @Test
    void write_relatedRepositories_allPersistAcrossReopen() {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        JsonStore store = JsonStore.initializeNew(paths);

        long[] ids = store.write(session -> {
            User user = session.users().save(user("alice"));
            Project project = saveProject(session, "Review");
            return new long[] {user.getId(), project.getId()};
        });

        JsonStore reopened = JsonStore.open(paths);
        assertEquals("alice", reopened.<String>read(session -> session.users().findById(ids[0])
                .orElseThrow().getUsername()));
        assertEquals("Review", reopened.<String>read(session -> session.projects().findById(ids[1])
                .orElseThrow().getName()));
    }

    @Test
    void write_actionThrows_allRepositoryChangesRolledBack() throws IOException {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        JsonStore store = JsonStore.initializeNew(paths);
        long existing = store.write(session -> session.users().save(user("before")).getId());
        byte[] before = Files.readAllBytes(paths.root().resolve("arbiter.json"));

        assertThrows(IllegalStateException.class, () -> store.write(session -> {
            session.users().save(user("after"));
            session.projects().save(project("Uncommitted"));
            throw new IllegalStateException("injected failure");
        }));

        JsonStore reopened = JsonStore.open(paths);
        assertEquals("before", reopened.<String>read(session -> session.users().findById(existing)
                .orElseThrow().getUsername()));
        assertFalse(reopened.<Boolean>read(session -> session.users().findByUsername("after").isPresent()));
        assertTrue(reopened.<Boolean>read(session -> session.projects().listAll().isEmpty()));
        assertEquals(0, java.util.Arrays.compare(before, Files.readAllBytes(paths.root().resolve("arbiter.json"))));
    }

    @Test
    void read_returnedObjectMutated_committedSnapshotUnchanged() {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        JsonStore store = JsonStore.initializeNew(paths);
        long id = store.write(session -> session.users().save(user("alice")).getId());

        User detached = store.read(session -> session.users().findById(id).orElseThrow());
        detached.setUsername("tampered");

        assertEquals("alice", JsonStore.open(paths).<String>read(
                session -> session.users().findById(id).orElseThrow().getUsername()));
    }

    @Test
    void write_afterFailedAction_nextIdRemainsAvailable() {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        JsonStore store = JsonStore.initializeNew(paths);
        long first = store.write(session -> session.users().save(user("first")).getId());

        assertThrows(IllegalStateException.class, () -> store.write(session -> {
            session.users().save(user("discarded"));
            throw new IllegalStateException("injected failure");
        }));

        long next = store.write(session -> session.users().save(user("next")).getId());
        assertEquals(first + 1, next);
    }

    @Test
    void write_publicationFails_committedSnapshotSurvivesReopen() throws IOException {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        JsonStore.initializeNew(paths).write(session -> session.users().save(user("before")));
        byte[] before = Files.readAllBytes(paths.dataFile());
        JsonStore failing = JsonStore.open(paths, (target, bytes) -> {
            throw new IOException("injected before replacement");
        });

        assertThrows(JsonStoreException.class, () -> failing.write(session ->
                saveProject(session, "Uncommitted")));
        assertThrows(JsonStoreException.class, () -> failing.write(session ->
                saveProject(session, "Retry before reopen")));

        assertEquals(0, java.util.Arrays.compare(before, Files.readAllBytes(paths.dataFile())));
        JsonStore reopened = JsonStore.open(paths);
        assertEquals("before", reopened.<String>read(session ->
                session.users().listAll().getFirst().getUsername()));
        assertTrue(reopened.<Boolean>read(session -> session.projects().listAll().isEmpty()));
    }

    @Test
    void read_mutationAttempt_rejectedAndSnapshotUnchanged() {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        JsonStore store = JsonStore.initializeNew(paths);

        assertThrows(IllegalStateException.class, () -> store.read(session ->
                session.users().save(user("forbidden"))));

        assertTrue(JsonStore.open(paths).<Boolean>read(session -> session.users().listAll().isEmpty()));
    }

    @Test
    void write_submissionAssignmentAndResolutionFailure_completeBeforeStateRestored() {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        JsonStore store = JsonStore.initializeNew(paths);
        long[] ids = store.write(session -> {
            long userId = session.users().save(user("alice")).getId();
            long projectId = saveProject(session, "Review").getId();
            Item item = new Item();
            item.setProjectId(projectId);
            item.setPath("media/item.txt");
            item.setContentHash("hash");
            item.setImportedAt(NOW);
            long itemId = session.items().save(item).getId();
            Split split = new Split();
            split.setProjectId(projectId);
            split.setName("Batch");
            split.setSeed(42L);
            split.setRequestedBatchSize(1);
            split.setCreatedAt(NOW);
            long splitId = session.splits().save(split).getId();
            return new long[] {userId, itemId, splitId};
        });

        assertThrows(IllegalStateException.class, () -> store.write(session -> {
            Split split = session.splits().findById(ids[2]).orElseThrow();
            split.setAssigned(true);
            session.splits().save(split);
            Assignment assignment = new Assignment();
            assignment.setSplitId(ids[2]);
            assignment.setAnnotatorId(ids[0]);
            assignment.setStatus(AssignmentStatus.IN_PROGRESS);
            assignment.setAssignedAt(NOW);
            long assignmentId = session.assignments().save(assignment).getId();
            Annotation answer = new Annotation();
            answer.setItemId(ids[1]);
            answer.setAssignmentId(assignmentId);
            answer.setAnnotatorId(ids[0]);
            answer.setScaleValue(4);
            answer.setSubmittedAt(NOW);
            session.annotations().insert(answer);
            Resolution resolution = new Resolution();
            resolution.setItemId(ids[1]);
            resolution.setScaleValue(4.0);
            resolution.setMethod(ResolutionMethod.AUTO_SCALE);
            resolution.setDecidedAt(NOW);
            session.resolutions().save(resolution);
            throw new IllegalStateException("injected failure");
        }));

        JsonStore reopened = JsonStore.open(paths);
        reopened.read(session -> {
            assertFalse(session.splits().findById(ids[2]).orElseThrow().isAssigned());
            assertTrue(session.assignments().listBySplit(ids[2]).isEmpty());
            assertTrue(session.annotations().listByItem(ids[1]).isEmpty());
            assertTrue(session.resolutions().findByItem(ids[1]).isEmpty());
            return null;
        });
    }

    /** Saves a project with the taxonomy settings every stored project needs. */
    private static Project saveProject(RepositorySession session, String name) {
        Project project = session.projects().save(project(name));
        session.taxonomySettings().save(settings(project.getId()));
        return project;
    }
}
