package arbiter.data.json;

import static arbiter.testing.Records.item;
import static arbiter.testing.Records.label;
import static arbiter.testing.Records.membership;
import static arbiter.testing.Records.project;
import static arbiter.testing.Records.settings;
import static arbiter.testing.Records.split;
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

import arbiter.model.project.Item;
import arbiter.model.user.User;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;

/** Integration checks for constraints enforced at the shared commit boundary. */
class JsonStoreIntegrityTest {
    @TempDir
    Path temporary;

    @Test
    void write_usernameDiffersOnlyByCase_rejectedWithoutPartialCommit() {
        JsonStore store = newStore();
        store.write(session -> session.users().save(user("Alice")));

        assertThrows(JsonStoreException.class, () -> store.write(session -> {
            session.projects().save(project("Uncommitted"));
            session.users().save(user("aLiCe"));
            return null;
        }));

        assertEquals(1, store.<Integer>read(session -> session.users().listAll().size()).intValue());
        assertTrue(store.<Boolean>read(session -> session.projects().listAll().isEmpty()));
    }

    @Test
    void write_requiredFieldMissing_rejectedWithoutCommit() {
        JsonStore store = newStore();
        User invalid = user("alice");
        invalid.setPasswordHash(null);

        assertThrows(JsonStoreException.class, () -> store.write(session -> session.users().save(invalid)));

        assertTrue(store.<Boolean>read(session -> session.users().listAll().isEmpty()));
    }

    @Test
    void write_referenceToMissingProject_rejectedWithoutCommit() {
        JsonStore store = newStore();
        Item invalid = item(100, "orphan");

        assertThrows(JsonStoreException.class, () -> store.write(session -> session.items().save(invalid)));

        assertEquals(0L, store.<Long>read(session -> session.items().countByProject(100)).longValue());
    }

    @Test
    void delete_itemBeforeAssignment_membershipRemoved() {
        JsonStore store = newStore();
        long[] ids = store.write(session -> {
            long project = session.projects().save(project("Project")).getId();
            long item = session.items().save(item(project, "one")).getId();
            long split = session.splits().save(split(project, "Batch")).getId();
            session.splitItems().save(membership(split, item));
            return new long[] {project, item, split};
        });

        store.write(session -> {
            session.items().deleteById(ids[1]);
            return null;
        });

        assertFalse(store.<Boolean>read(session -> session.items().findById(ids[1]).isPresent()));
        assertFalse(store.<Boolean>read(session -> session.splitItems().findByItem(ids[1]).isPresent()));
        assertEquals(0L, store.<Long>read(session -> session.splitItems().countBySplit(ids[2])).longValue());
        assertEquals(1, store.<Integer>read(session -> session.projects().listAll().size()).intValue());
    }

    @Test
    void delete_individualUnassignedRecords_recordsRemoved() {
        JsonStore store = newStore();
        long[] ids = store.write(session -> {
            long project = session.projects().save(project("Project")).getId();
            long taxonomy = session.taxonomySettings().save(settings(project)).getId();
            long label = session.labels().save(label(project)).getId();
            long split = session.splits().save(split(project, "Batch")).getId();
            return new long[] {project, taxonomy, label, split};
        });

        store.write(session -> {
            session.labels().deleteById(ids[2]);
            session.taxonomySettings().deleteByProject(ids[0]);
            session.splits().deleteById(ids[3]);
            return null;
        });

        assertTrue(store.<Boolean>read(session -> session.labels().findById(ids[2]).isEmpty()));
        assertTrue(store.<Boolean>read(session -> session.taxonomySettings().findById(ids[1]).isEmpty()));
        assertTrue(store.<Boolean>read(session -> session.splits().findById(ids[3]).isEmpty()));
        assertTrue(store.<Boolean>read(session -> session.projects().findById(ids[0]).isPresent()));
    }

    @Test
    void delete_projectWithOwnedRecords_recordsRemovedButMediaKept() throws IOException {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        JsonStore store = JsonStore.initializeNew(paths);
        Path media = Files.writeString(paths.mediaDirectory().resolve("one.txt"), "source");
        long[] ids = store.write(session -> {
            long project = session.projects().save(project("Project")).getId();
            session.taxonomySettings().save(settings(project));
            session.labels().save(label(project));
            long item = session.items().save(item(project, "one")).getId();
            long split = session.splits().save(split(project, "Batch")).getId();
            session.splitItems().save(membership(split, item));
            return new long[] {project, item, split};
        });

        store.write(session -> {
            session.projects().deleteById(ids[0]);
            return null;
        });

        JsonStore reopened = JsonStore.open(paths);
        assertTrue(reopened.<Boolean>read(session -> session.projects().listAll().isEmpty()));
        assertTrue(reopened.<Boolean>read(session -> session.taxonomySettings()
                .findByProject(ids[0]).isEmpty()));
        assertTrue(reopened.<Boolean>read(session -> session.labels().listByProject(ids[0]).isEmpty()));
        assertTrue(reopened.<Boolean>read(session -> session.items().findById(ids[1]).isEmpty()));
        assertTrue(reopened.<Boolean>read(session -> session.splits().findById(ids[2]).isEmpty()));
        assertEquals("source", Files.readString(media));
    }

    private JsonStore newStore() {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        return JsonStore.initializeNew(paths);
    }
}
