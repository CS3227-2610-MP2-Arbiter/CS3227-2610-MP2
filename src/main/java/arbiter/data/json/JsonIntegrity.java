package arbiter.data.json;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import arbiter.model.annotation.Annotation;
import arbiter.model.project.Assignment;
import arbiter.model.project.Item;
import arbiter.model.project.Label;
import arbiter.model.project.Project;
import arbiter.model.project.Split;
import arbiter.model.project.SplitItem;
import arbiter.model.project.TaxonomySettings;
import arbiter.model.resolution.Resolution;
import arbiter.model.user.User;

/** Checks storage constraints before a snapshot can be read or committed. */
final class JsonIntegrity {
    private JsonIntegrity() {
    }

    static void validate(JsonSnapshot state) {
        validateHeader(state);
        Set<Long> users = ids(state.users(), User::getId, "users");
        Set<Long> projects = ids(state.projects(), Project::getId, "projects");
        Set<Long> taxonomies = ids(state.taxonomySettings(), TaxonomySettings::getId, "taxonomy settings");
        Set<Long> labels = ids(state.labels(), Label::getId, "labels");
        Set<Long> items = ids(state.items(), Item::getId, "items");
        Set<Long> splits = ids(state.splits(), Split::getId, "splits");
        Set<Long> splitItems = ids(state.splitItems(), SplitItem::getId, "split items");
        Set<Long> assignments = ids(state.assignments(), Assignment::getId, "assignments");
        Set<Long> annotations = ids(state.annotations(), Annotation::getId, "annotations");
        Set<Long> resolutions = ids(state.resolutions(), Resolution::getId, "resolutions");

        validateNextId(state.nextId(), List.of(users, projects, taxonomies, labels, items, splits,
                splitItems, assignments, annotations, resolutions));

        validateUsers(state.users());
        validateProjects(state.projects());
        validateTaxonomies(state.taxonomySettings(), projects);
        validateLabels(state.labels(), projects);
        validateItems(state.items(), projects);
        validateSplits(state.splits(), projects);
        validateSplitItems(state.splitItems(), splits, items);
        validateAssignments(state.assignments(), state.splits(), users);
        validateAnnotations(state.annotations(), items, assignments, users, labels);
        validateResolutions(state.resolutions(), items, labels, users);
    }

    private static void validateHeader(JsonSnapshot state) {
        if (state == null || state.schemaVersion() != JsonSnapshot.CURRENT_VERSION || state.nextId() < 1) {
            throw invalid("snapshot version or next identifier");
        }
    }

    private static void validateNextId(long nextId, List<Set<Long>> idSets) {
        long maximum = idSets.stream().flatMap(Set::stream).mapToLong(Long::longValue).max().orElse(0);
        if (nextId <= maximum) {
            throw invalid("next identifier would reuse an existing identifier");
        }
    }

    private static void validateUsers(List<User> usersInSnapshot) {
        Set<String> usernames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (User user : usersInSnapshot) {
            require(user.getUsername(), "username");
            require(user.getPasswordHash(), "password hash");
            require(user.getPasswordSalt(), "password salt");
            require(user.getRole(), "user role");
            require(user.getAccountStatus(), "account status");
            require(user.getCreatedAt(), "user creation time");
            requireUnique(usernames, user.getUsername(), "duplicate username");
        }
    }

    private static void validateProjects(List<Project> projects) {
        for (Project project : projects) {
            require(project.getName(), "project name");
            require(project.getOutputFormat(), "project output format");
            require(project.getCreatedAt(), "project creation time");
        }
    }

    private static void validateTaxonomies(List<TaxonomySettings> taxonomies, Set<Long> projects) {
        Set<Long> configured = new HashSet<>();
        for (TaxonomySettings taxonomy : taxonomies) {
            reference(projects, taxonomy.getProjectId(), "taxonomy project");
            require(taxonomy.getKind(), "taxonomy kind");
            requireUnique(configured, taxonomy.getProjectId(), "duplicate taxonomy settings for a project");
            validateRange(taxonomy.getScaleMin(), taxonomy.getScaleMax());
        }
        if (!configured.containsAll(projects)) {
            throw invalid("missing taxonomy settings for a project");
        }
    }

    /** Checks that a scale range is unset, or has both ends, within the bounds, and its minimum below its maximum. */
    private static void validateRange(Integer minimum, Integer maximum) {
        if (minimum == null && maximum == null) {
            return;
        }
        if (minimum == null || maximum == null) {
            throw invalid("scale range with only one end");
        }
        if (minimum >= maximum) {
            throw invalid("scale range minimum not below its maximum");
        }
        if (minimum < TaxonomySettings.LOWEST_SCALE_VALUE || maximum > TaxonomySettings.HIGHEST_SCALE_VALUE) {
            throw invalid("scale range outside its bounds");
        }
    }

    /** Checks each label, and that no two labels of a project have the same key, ignoring case (#26). */
    private static void validateLabels(List<Label> labels, Set<Long> projects) {
        Map<Long, Set<String>> keys = new HashMap<>();
        for (Label label : labels) {
            reference(projects, label.getProjectId(), "label project");
            require(label.getKey(), "label key");
            require(label.getSequence(), "label sequence");
            requireUnique(keys.computeIfAbsent(label.getProjectId(), project -> new TreeSet<>(
                    String.CASE_INSENSITIVE_ORDER)), label.getKey(), "duplicate label key in a project");
        }
    }

    private static void validateItems(List<Item> items, Set<Long> projects) {
        for (Item item : items) {
            reference(projects, item.getProjectId(), "item project");
            require(item.getPath(), "item path");
            require(item.getContentHash(), "item hash");
            require(item.getImportedAt(), "item import time");
        }
    }

    private static void validateSplits(List<Split> splits, Set<Long> projects) {
        for (Split split : splits) {
            reference(projects, split.getProjectId(), "split project");
            require(split.getName(), "split name");
            require(split.getSeed(), "split seed");
            require(split.getRequestedBatchSize(), "split requested batch size");
            require(split.getCreatedAt(), "split creation time");
        }
    }

    private static void validateSplitItems(List<SplitItem> splitItems, Set<Long> splits, Set<Long> items) {
        for (SplitItem splitItem : splitItems) {
            reference(splits, splitItem.getSplitId(), "membership split");
            reference(items, splitItem.getItemId(), "membership item");
            require(splitItem.getSequence(), "membership sequence");
        }
    }

    /**
     * Checks each assignment, and that an assigned split has its annotations per item, no more assignments than
     * that and no annotator twice (rule 19).
     */
    private static void validateAssignments(List<Assignment> assignments, List<Split> splits, Set<Long> users) {
        Map<Long, Integer> annotationsPerItem = new HashMap<>();
        for (Split split : splits) {
            annotationsPerItem.put(split.getId(), split.getAnnotationsPerItem());
        }
        Map<Long, Integer> taken = new HashMap<>();
        Set<List<Long>> assigned = new HashSet<>();
        for (Assignment assignment : assignments) {
            reference(annotationsPerItem.keySet(), assignment.getSplitId(), "assignment split");
            reference(users, assignment.getAnnotatorId(), "assignment annotator");
            require(assignment.getStatus(), "assignment status");
            require(assignment.getAssignedAt(), "assignment time");
            Integer places = annotationsPerItem.get(assignment.getSplitId());
            require(places, "annotations per item of an assigned split");
            if (taken.merge(assignment.getSplitId(), 1, Integer::sum) > places) {
                throw invalid("more assignments than annotations per item on a split");
            }
            requireUnique(assigned, List.of(assignment.getSplitId(), assignment.getAnnotatorId()),
                    "duplicate assignment of an annotator to a split");
        }
    }

    private static void validateAnnotations(List<Annotation> annotations, Set<Long> items,
            Set<Long> assignments, Set<Long> users, Set<Long> labels) {
        for (Annotation annotation : annotations) {
            reference(items, annotation.getItemId(), "annotation item");
            reference(assignments, annotation.getAssignmentId(), "annotation assignment");
            reference(users, annotation.getAnnotatorId(), "annotation annotator");
            optionalReference(labels, annotation.getLabelId(), "annotation label");
            require(annotation.getSubmittedAt(), "submission time");
        }
    }

    private static void validateResolutions(List<Resolution> resolutions, Set<Long> items,
            Set<Long> labels, Set<Long> users) {
        for (Resolution resolution : resolutions) {
            reference(items, resolution.getItemId(), "resolution item");
            optionalReference(labels, resolution.getLabelId(), "resolution label");
            optionalReference(users, resolution.getDecidedByUserId(), "deciding user");
            require(resolution.getMethod(), "resolution method");
            require(resolution.getDecidedAt(), "resolution time");
        }
    }

    private static <T> Set<Long> ids(List<T> rows, Function<T, Long> id, String name) {
        if (rows == null) {
            throw invalid("missing " + name);
        }
        Set<Long> result = new HashSet<>();
        String invalidId = "invalid or duplicate identifier in " + name;
        for (T row : rows) {
            Long rowId = row == null ? null : id.apply(row);
            if (rowId == null || rowId < 1) {
                throw invalid(invalidId);
            }
            requireUnique(result, rowId, invalidId);
        }
        return result;
    }

    private static <T> void requireUnique(Set<T> seen, T value, String failure) {
        if (!seen.add(value)) {
            throw invalid(failure);
        }
    }

    private static void reference(Set<Long> ids, Long id, String name) {
        if (id == null || !ids.contains(id)) {
            throw invalid("missing reference: " + name);
        }
    }

    private static void optionalReference(Set<Long> ids, Long id, String name) {
        if (id != null) {
            reference(ids, id, name);
        }
    }

    private static void require(Object value, String name) {
        if (Objects.isNull(value)) {
            throw invalid("missing " + name);
        }
    }

    private static JsonStoreException invalid(String detail) {
        return new JsonStoreException("Invalid workspace data: " + detail);
    }
}
