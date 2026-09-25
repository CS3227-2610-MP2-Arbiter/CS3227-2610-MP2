package arbiter.testing;

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
import arbiter.workspace.ResolvedSource;

/**
 * Builds unsaved model records with synthetic values, for tests that save them through a repository.
 *
 * <p>Accounts built here carry placeholder credentials and cannot sign in; use
 * {@link ClassificationWorkflow} or {@link arbiter.service.TestAccounts} for an account that can.
 */
public final class Records {
    /** The fixed time every record is stamped with, so assertions are reproducible. */
    public static final Instant NOW = Instant.parse("2026-09-20T08:30:00Z");

    private Records() {
    }

    /** Returns an active annotator account with placeholder credentials. */
    public static User user(String username) {
        return user(username, Role.ANNOTATOR);
    }

    /** Returns an active account with this role and placeholder credentials. */
    public static User user(String username, Role role) {
        User value = new User();
        value.setUsername(username);
        value.setPasswordHash("hash");
        value.setPasswordSalt("salt");
        value.setRole(role);
        value.setAccountStatus(AccountStatus.ACTIVE);
        value.setCreatedAt(NOW);
        return value;
    }

    /** Returns a project with this name and JSON output. */
    public static Project project(String name) {
        Project value = new Project();
        value.setName(name);
        value.setDescription("Synthetic project");
        value.setOutputFormat(OutputFormat.JSON);
        value.setCreatedAt(NOW);
        return value;
    }

    /** Returns single-label taxonomy settings for a project. */
    public static TaxonomySettings settings(long projectId) {
        TaxonomySettings value = new TaxonomySettings();
        value.setProjectId(projectId);
        value.setKind(TaxonomyKind.SINGLE);
        return value;
    }

    /** Returns scale taxonomy settings for a project, with this inclusive range. */
    public static TaxonomySettings scaleSettings(long projectId, int minimum, int maximum) {
        TaxonomySettings value = settings(projectId);
        value.setKind(TaxonomyKind.SCALE);
        value.setScaleMin(minimum);
        value.setScaleMax(maximum);
        return value;
    }

    /** Returns the label {@code positive} in the first position. */
    public static Label label(long projectId) {
        return label(projectId, "positive", 1);
    }

    /** Returns a label with this key in this position. */
    public static Label label(long projectId, String key, int sequence) {
        Label value = new Label();
        value.setProjectId(projectId);
        value.setKey(key);
        value.setDescription("Synthetic label");
        value.setSequence(sequence);
        return value;
    }

    /** Returns an item at {@code media/<hash>.txt} whose recorded hash is {@code hash}; no file is written. */
    public static Item item(long projectId, String hash) {
        return item(projectId, hash, hash);
    }

    /** Returns an item at {@code media/<name>.txt} with this recorded hash; no file is written. */
    public static Item item(long projectId, String name, String hash) {
        Item value = new Item();
        value.setProjectId(projectId);
        value.setPath("media/" + name + ".txt");
        value.setContentHash(hash);
        value.setImportedAt(NOW);
        return value;
    }

    /** Returns an item recording a source file registered through {@link arbiter.workspace.SourceResolver}. */
    public static Item item(long projectId, ResolvedSource source) {
        Item value = new Item();
        value.setProjectId(projectId);
        value.setPath(source.storedPath());
        value.setContentHash(source.contentHash());
        value.setImportedAt(NOW);
        return value;
    }

    /** Returns an unassigned split named {@code Batch}, with two annotations per item. */
    public static Split split(long projectId) {
        return split(projectId, "Batch", 2);
    }

    /** Returns an unassigned split with this name, with two annotations per item. */
    public static Split split(long projectId, String name) {
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

    /** Returns an item's membership in the first position of a split. */
    public static SplitItem membership(long splitId, long itemId) {
        return membership(splitId, itemId, 1);
    }

    /** Returns an item's membership in this position of a split. */
    public static SplitItem membership(long splitId, long itemId, int sequence) {
        SplitItem value = new SplitItem();
        value.setSplitId(splitId);
        value.setItemId(itemId);
        value.setSequence(sequence);
        return value;
    }

    /** Returns an assignment whose every item has been answered. */
    public static Assignment assignment(long splitId, long annotatorId) {
        Assignment value = new Assignment();
        value.setSplitId(splitId);
        value.setAnnotatorId(annotatorId);
        value.setStatus(AssignmentStatus.SUBMITTED);
        value.setAssignedAt(NOW);
        return value;
    }

    /** Returns a submitted single-label answer. */
    public static Annotation answer(long itemId, long assignmentId, long annotatorId, long labelId) {
        Annotation value = submission(itemId, assignmentId, annotatorId);
        value.setLabelId(labelId);
        return value;
    }

    /** Returns a submitted scale rating. */
    public static Annotation scaleAnswer(long itemId, long assignmentId, long annotatorId, int rating) {
        Annotation value = submission(itemId, assignmentId, annotatorId);
        value.setScaleValue(rating);
        return value;
    }

    private static Annotation submission(long itemId, long assignmentId, long annotatorId) {
        Annotation value = new Annotation();
        value.setItemId(itemId);
        value.setAssignmentId(assignmentId);
        value.setAnnotatorId(annotatorId);
        value.setSubmittedAt(NOW);
        return value;
    }

    /** Returns an automatic majority resolution to this label. */
    public static Resolution resolution(long itemId, long labelId) {
        Resolution value = decision(itemId, ResolutionMethod.MAJORITY);
        value.setLabelId(labelId);
        return value;
    }

    /** Returns an automatic scale resolution to this mean. */
    public static Resolution scaleResolution(long itemId, double mean) {
        Resolution value = decision(itemId, ResolutionMethod.AUTO_SCALE);
        value.setScaleValue(mean);
        return value;
    }

    private static Resolution decision(long itemId, ResolutionMethod method) {
        Resolution value = new Resolution();
        value.setItemId(itemId);
        value.setMethod(method);
        value.setDecidedAt(NOW);
        return value;
    }
}
