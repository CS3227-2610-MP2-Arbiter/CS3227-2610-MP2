package arbiter.data.json;

import java.time.Instant;

import arbiter.model.annotation.Annotation;
import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.Item;
import arbiter.model.project.Label;
import arbiter.model.project.OutputFormat;
import arbiter.model.project.Project;
import arbiter.model.project.Split;
import arbiter.model.project.SplitItem;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.project.TaxonomySettings;
import arbiter.model.resolution.Resolution;
import arbiter.model.resolution.ResolutionMethod;
import arbiter.model.user.AccountStatus;
import arbiter.model.user.Role;
import arbiter.model.user.User;

/** Shared records for JSON store integration tests. */
final class JsonStoreFixtures {
    static final Instant NOW = Instant.parse("2026-09-20T08:30:00Z");

    private JsonStoreFixtures() {
    }

    static User user(String username) {
        return user(username, Role.ANNOTATOR);
    }

    static User user(String username, Role role) {
        User value = new User();
        value.setUsername(username);
        value.setPasswordHash("hash");
        value.setPasswordSalt("salt");
        value.setRole(role);
        value.setAccountStatus(AccountStatus.ACTIVE);
        value.setCreatedAt(NOW);
        return value;
    }

    static Project project(String name) {
        Project value = new Project();
        value.setName(name);
        value.setDescription("Synthetic project");
        value.setOutputFormat(OutputFormat.JSON);
        value.setCreatedAt(NOW);
        return value;
    }

    static TaxonomySettings settings(long projectId) {
        TaxonomySettings value = new TaxonomySettings();
        value.setProjectId(projectId);
        value.setKind(TaxonomyKind.SINGLE);
        return value;
    }

    static Label label(long projectId) {
        return label(projectId, "positive", 1);
    }

    static Label label(long projectId, String key, int sequence) {
        Label value = new Label();
        value.setProjectId(projectId);
        value.setKey(key);
        value.setDescription("Synthetic label");
        value.setSequence(sequence);
        return value;
    }

    static Item item(long projectId, String hash) {
        return item(projectId, hash, hash);
    }

    static Item item(long projectId, String name, String hash) {
        Item value = new Item();
        value.setProjectId(projectId);
        value.setPath("media/" + name + ".txt");
        value.setContentHash(hash);
        value.setImportedAt(NOW);
        return value;
    }

    static Split split(long projectId) {
        return split(projectId, "Batch", 2);
    }

    static Split split(long projectId, String name) {
        return split(projectId, name, 1);
    }

    private static Split split(long projectId, String name, int batchSize) {
        Split value = new Split();
        value.setProjectId(projectId);
        value.setName(name);
        value.setAnnotationsPerItem(2);
        value.setSeed(42L);
        value.setRequestedBatchSize(batchSize);
        value.setCreatedAt(NOW);
        return value;
    }

    static SplitItem membership(long splitId, long itemId) {
        return membership(splitId, itemId, 1);
    }

    static SplitItem membership(long splitId, long itemId, int sequence) {
        SplitItem value = new SplitItem();
        value.setSplitId(splitId);
        value.setItemId(itemId);
        value.setSequence(sequence);
        return value;
    }

    static Assignment assignment(long splitId, long annotatorId) {
        Assignment value = new Assignment();
        value.setSplitId(splitId);
        value.setAnnotatorId(annotatorId);
        value.setStatus(AssignmentStatus.SUBMITTED);
        value.setAssignedAt(NOW);
        return value;
    }

    static Annotation answer(long itemId, long assignmentId, long annotatorId, long labelId) {
        Annotation value = new Annotation();
        value.setItemId(itemId);
        value.setAssignmentId(assignmentId);
        value.setAnnotatorId(annotatorId);
        value.setLabelId(labelId);
        value.setSubmittedAt(NOW);
        return value;
    }

    static Resolution resolution(long itemId, long labelId) {
        Resolution value = new Resolution();
        value.setItemId(itemId);
        value.setLabelId(labelId);
        value.setMethod(ResolutionMethod.MAJORITY);
        value.setDecidedAt(NOW);
        return value;
    }
}
