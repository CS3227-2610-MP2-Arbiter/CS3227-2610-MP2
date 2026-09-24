package arbiter.data.json;

import static arbiter.testing.Records.answer;
import static arbiter.testing.Records.assignment;
import static arbiter.testing.Records.item;
import static arbiter.testing.Records.label;
import static arbiter.testing.Records.membership;
import static arbiter.testing.Records.project;
import static arbiter.testing.Records.resolution;
import static arbiter.testing.Records.settings;
import static arbiter.testing.Records.split;
import static arbiter.testing.Records.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.Label;
import arbiter.model.project.SplitItem;
import arbiter.model.user.Role;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;

/** Integration checks for the repository contracts over a persisted snapshot. */
class JsonStoreRepositoryTest {
    @TempDir
    Path temporary;

    @Test
    void repositories_relatedRecords_queriesSurviveReopen() {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        JsonStore store = JsonStore.initializeNew(paths);
        Fixture ids = store.write(session -> {
            long alice = session.users().save(user("Alice", Role.ANNOTATOR)).getId();
            long bob = session.users().save(user("Bob", Role.ANNOTATOR)).getId();
            long adjudicator = session.users().save(user("Judge", Role.ADJUDICATOR)).getId();
            long project = session.projects().save(project("Project")).getId();
            long settings = session.taxonomySettings().save(settings(project)).getId();
            long positive = session.labels().save(label(project, "positive", 2)).getId();
            long negative = session.labels().save(label(project, "negative", 1)).getId();
            long firstItem = session.items().save(item(project, "first", "hash-1")).getId();
            long secondItem = session.items().save(item(project, "second", "hash-2")).getId();
            long split = session.splits().save(split(project)).getId();
            session.splitItems().save(membership(split, firstItem, 2));
            session.splitItems().save(membership(split, secondItem, 1));
            long aliceAssignment = session.assignments().save(assignment(split, alice)).getId();
            long bobAssignment = session.assignments().save(assignment(split, bob)).getId();
            long aliceAnswer = session.annotations()
                    .insert(answer(firstItem, aliceAssignment, alice, positive)).getId();
            session.annotations().insert(answer(firstItem, bobAssignment, bob, positive));
            session.resolutions().save(resolution(firstItem, positive));
            return new Fixture(alice, bob, adjudicator, project, settings, positive, negative,
                    firstItem, secondItem, split, aliceAssignment, bobAssignment, aliceAnswer);
        });

        JsonStore reopened = JsonStore.open(paths);
        reopened.read(session -> {
            assertEquals("Alice", session.users().findByUsername("aLiCe").orElseThrow().getUsername());
            assertEquals(3, session.users().listAll().size());
            assertEquals(2, session.users().listByRole(Role.ANNOTATOR).size());
            assertEquals(1, session.users().countActiveByRole(Role.ADJUDICATOR));
            assertEquals(Long.valueOf(ids.adjudicator()),
                    session.users().findById(ids.adjudicator()).orElseThrow().getId());

            assertEquals(1, session.projects().listAll().size());
            assertEquals("Project", session.projects().findById(ids.project()).orElseThrow().getName());
            assertEquals(Long.valueOf(ids.settings()), session.taxonomySettings()
                    .findByProject(ids.project()).orElseThrow().getId());
            assertEquals(Long.valueOf(ids.project()), session.taxonomySettings().findById(ids.settings())
                    .orElseThrow().getProjectId());
            assertEquals(List.of("negative", "positive"), session.labels().listByProject(ids.project())
                    .stream().map(Label::getKey).toList());
            assertEquals("positive", session.labels().findById(ids.positive()).orElseThrow().getKey());
            assertEquals("negative", session.labels().findById(ids.negative()).orElseThrow().getKey());

            assertEquals(2, session.items().countByProject(ids.project()));
            assertEquals(2, session.items().listByProject(ids.project()).size());
            assertEquals(Long.valueOf(ids.firstItem()), session.items()
                    .findByProjectAndHash(ids.project(), "hash-1")
                    .orElseThrow().getId());
            assertEquals("media/first.txt", session.items().findById(ids.firstItem()).orElseThrow().getPath());
            assertEquals(Long.valueOf(ids.split()), session.splits()
                    .listByProject(ids.project()).getFirst().getId());
            assertEquals("Batch", session.splits().findById(ids.split()).orElseThrow().getName());
            assertEquals(List.of(ids.secondItem(), ids.firstItem()), session.splitItems().listBySplit(ids.split())
                    .stream().map(SplitItem::getItemId).toList());
            assertEquals(Long.valueOf(ids.split()), session.splitItems()
                    .findByItem(ids.secondItem()).orElseThrow().getSplitId());
            assertEquals(2, session.splitItems().countBySplit(ids.split()));

            assertEquals(Long.valueOf(ids.aliceAssignment()), session.assignments()
                    .listByAnnotator(ids.alice())
                    .getFirst().getId());
            assertEquals(Long.valueOf(ids.bobAssignment()), session.assignments()
                    .listByAnnotator(ids.bob())
                    .getFirst().getId());
            assertEquals(2, session.assignments().listBySplit(ids.split()).size());
            assertEquals(2, session.assignments().listByStatus(AssignmentStatus.SUBMITTED).size());
            assertEquals(Long.valueOf(ids.split()), session.assignments().findById(ids.aliceAssignment())
                    .orElseThrow().getSplitId());

            assertEquals(2, session.annotations().listByItem(ids.firstItem()).size());
            assertEquals(1, session.annotations().listByAssignment(ids.aliceAssignment()).size());
            assertEquals(Long.valueOf(ids.aliceAnswer()), session.annotations().findByItemAndAnnotator(
                    ids.firstItem(), ids.alice()).orElseThrow().getId());
            assertEquals(Long.valueOf(ids.positive()), session.annotations().findById(ids.aliceAnswer())
                    .orElseThrow().getLabelId());
            assertFalse(session.annotations().findByItemAndAnnotator(ids.firstItem(), ids.adjudicator()).isPresent());

            assertEquals(Long.valueOf(ids.positive()), session.resolutions().findByItem(ids.firstItem())
                    .orElseThrow().getLabelId());
            assertEquals(1, session.resolutions().listByItems(List.of(ids.secondItem(), ids.firstItem())).size());
            assertTrue(session.resolutions().findByItem(ids.secondItem()).isEmpty());
            return null;
        });
    }

    private record Fixture(long alice, long bob, long adjudicator, long project, long settings,
            long positive, long negative, long firstItem, long secondItem, long split,
            long aliceAssignment, long bobAssignment, long aliceAnswer) {
    }
}
