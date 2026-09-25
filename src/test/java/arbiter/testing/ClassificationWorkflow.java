package arbiter.testing;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import arbiter.data.json.RepositorySession;
import arbiter.model.annotation.Annotation;
import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.Split;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.project.TaxonomySettings;
import arbiter.model.resolution.Resolution;
import arbiter.model.resolution.ResolutionMethod;
import arbiter.model.user.AccountStatus;
import arbiter.model.user.Role;
import arbiter.model.user.User;
import arbiter.service.TestAccounts;
import arbiter.workspace.ResolvedSource;

/**
 * The identifiers of one seeded classification project: its owner, taxonomy, corpus, split,
 * accounts and assignments.
 *
 * <p>Build one with {@link #single} or {@link #scale}, describe the state a test starts from, and
 * {@link Builder#seed seed} it into a {@link TestWorkspace}. The fixture records only states the app
 * can reach. A test names each resolution it needs, so the expected answer is visible in the test,
 * and the fixture refuses one that does not follow from the answers under rule 10.
 */
public final class ClassificationWorkflow {
    private final long ownerId;
    private final long projectId;
    private final long splitId;
    private final List<Long> itemIds;
    private final Map<String, Long> labelIds;
    private final Map<String, Long> accountIds;
    private final Map<String, Long> assignmentIds;

    private ClassificationWorkflow(long ownerId, long projectId, long splitId, List<Long> itemIds,
            Map<String, Long> labelIds, Map<String, Long> accountIds, Map<String, Long> assignmentIds) {
        this.ownerId = ownerId;
        this.projectId = projectId;
        this.splitId = splitId;
        this.itemIds = List.copyOf(itemIds);
        this.labelIds = Map.copyOf(labelIds);
        this.accountIds = Map.copyOf(accountIds);
        this.assignmentIds = Map.copyOf(assignmentIds);
    }

    /** Starts a single-label project whose labels have these keys, in this order. */
    public static Builder single(String... labelKeys) {
        List<String> keys = List.of(labelKeys);
        if (keys.isEmpty() || new HashSet<>(keys).size() != keys.size()) {
            throw new IllegalArgumentException("A single-label taxonomy needs distinct label keys: " + keys);
        }
        return new Builder(TaxonomyKind.SINGLE, keys, 0, 0);
    }

    /** Starts a scale project whose ratings run from {@code minimum} to {@code maximum} inclusive. */
    public static Builder scale(int minimum, int maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException("The scale minimum is above its maximum");
        }
        return new Builder(TaxonomyKind.SCALE, List.of(), minimum, maximum);
    }

    /** Returns the owner's account identifier. */
    public long ownerId() {
        return ownerId;
    }

    /** Returns the project's identifier. */
    public long projectId() {
        return projectId;
    }

    /** Returns the split's identifier. */
    public long splitId() {
        return splitId;
    }

    /** Returns the item identifiers in split order. */
    public List<Long> itemIds() {
        return itemIds;
    }

    /** Returns the identifier of the item at this zero-based position in split order. */
    public long itemId(int index) {
        return itemIds.get(index);
    }

    /** Returns the identifier of the label with this key. */
    public long labelId(String key) {
        return lookUp(labelIds, key, "label");
    }

    /** Returns the account identifier of an annotator named when seeding. */
    public long annotatorId(String username) {
        return lookUp(accountIds, username, "annotator");
    }

    /** Returns the assignment identifier of an assigned annotator. */
    public long assignmentId(String username) {
        return lookUp(assignmentIds, username, "assignment for");
    }

    private static long lookUp(Map<String, Long> ids, String key, String what) {
        Long id = ids.get(key);
        if (id == null) {
            throw new IllegalArgumentException("This workflow has no " + what + " " + key);
        }
        return id;
    }

    /** Describes a classification project, then seeds it in one committed action. */
    public static final class Builder {
        private final TaxonomyKind kind;
        private final List<String> labelKeys;
        private final int minimum;
        private final int maximum;
        private final Map<String, List<?>> assignees = new LinkedHashMap<>();
        private final Set<String> unassigned = new LinkedHashSet<>();
        private final Set<String> disabled = new LinkedHashSet<>();
        private final Map<Integer, PlannedResolution> resolutions = new LinkedHashMap<>();
        private int itemCount = 1;
        private Integer annotationsPerItem;

        private Builder(TaxonomyKind kind, List<String> labelKeys, int minimum, int maximum) {
            this.kind = kind;
            this.labelKeys = labelKeys;
            this.minimum = minimum;
            this.maximum = maximum;
        }

        /** Sets how many items the corpus and the split hold; the default is one. */
        public Builder items(int count) {
            if (count < 1) {
                throw new IllegalArgumentException("A split needs at least one item");
            }
            itemCount = count;
            return this;
        }

        /**
         * Sets how many annotators each item needs (k). Unset, k is the number of assignees, or
         * null for an unassigned split, as it is before the adjudicator assigns one.
         */
        public Builder annotationsPerItem(int count) {
            if (count < 1) {
                throw new IllegalArgumentException("Each item needs at least one annotation");
            }
            annotationsPerItem = count;
            return this;
        }

        /** Assigns this annotator with nothing submitted yet. */
        public Builder assign(String username) {
            return assignWith(username, List.of());
        }

        /** Assigns this annotator, who has submitted these labels for the first items in split order. */
        public Builder assign(String username, String... labels) {
            requireKind(TaxonomyKind.SINGLE, "labels");
            for (String label : labels) {
                if (!labelKeys.contains(label)) {
                    throw new IllegalArgumentException("No label " + label + " in " + labelKeys);
                }
            }
            return assignWith(username, List.of(labels));
        }

        /** Assigns this annotator, who has submitted these ratings for the first items in split order. */
        public Builder assign(String username, int... ratings) {
            requireKind(TaxonomyKind.SCALE, "ratings");
            for (int rating : ratings) {
                if (rating < minimum || rating > maximum) {
                    throw new IllegalArgumentException("Rating " + rating + " is outside " + minimum + " to "
                            + maximum);
                }
            }
            return assignWith(username, Arrays.stream(ratings).boxed().toList());
        }

        /** Adds an annotator account that is not assigned to this split. */
        public Builder annotator(String username) {
            requireNewName(username);
            unassigned.add(username);
            return this;
        }

        /** Disables an annotator named in this workflow, keeping any assignment and submissions. */
        public Builder disabled(String username) {
            disabled.add(Objects.requireNonNull(username, "username"));
            return this;
        }

        /** Records an automatic resolution of an item to this label, which must hold a strict majority. */
        public Builder majority(int item, String label) {
            return resolve(item, ResolutionMethod.MAJORITY, label, null);
        }

        /** Records the owner's resolution of a disputed item, one with no strict majority, to this label. */
        public Builder adjudicated(int item, String label) {
            return resolve(item, ResolutionMethod.ADJUDICATED, label, null);
        }

        /** Records an automatic scale resolution of an item to this mean, which must be its ratings' exact mean. */
        public Builder mean(int item, double mean) {
            return resolve(item, ResolutionMethod.AUTO_SCALE, null, mean);
        }

        /**
         * Seeds the described state into a workspace and returns its identifiers.
         *
         * <p>The owner is created if the workspace has none. An annotator whose username already
         * exists is reused. Every check runs before anything is written.
         *
         * @throws IllegalArgumentException if the description is a state the app cannot reach
         */
        public ClassificationWorkflow seed(TestWorkspace workspace) {
            Objects.requireNonNull(workspace, "workspace");
            Integer k = validate();
            Map<String, User> newAccounts = newAccounts(workspace);
            workspace.ensureOwner();
            List<ResolvedSource> sources = writeCorpus(workspace);
            return workspace.store().write(session -> save(session, k, newAccounts, sources));
        }

        private Builder assignWith(String username, List<?> answers) {
            requireNewName(username);
            assignees.put(username, answers);
            return this;
        }

        private Builder resolve(int item, ResolutionMethod method, String label, Double mean) {
            TaxonomyKind needed = method == ResolutionMethod.AUTO_SCALE ? TaxonomyKind.SCALE : TaxonomyKind.SINGLE;
            requireKind(needed, method + " resolutions");
            if (label != null && !labelKeys.contains(label)) {
                throw new IllegalArgumentException("No label " + label + " in " + labelKeys);
            }
            if (resolutions.putIfAbsent(item, new PlannedResolution(item, method, label, mean)) != null) {
                throw new IllegalArgumentException("Item " + item + " already has a resolution");
            }
            return this;
        }

        private void requireKind(TaxonomyKind needed, String what) {
            if (kind != needed) {
                throw new IllegalArgumentException("A " + kind + " project cannot take " + what);
            }
        }

        private void requireNewName(String username) {
            Objects.requireNonNull(username, "username");
            if (username.equalsIgnoreCase(TestWorkspace.OWNER)) {
                throw new IllegalArgumentException(username + " is the owner's username");
            }
            // Usernames are unique regardless of case, as the store compares them.
            boolean named = assignees.keySet().stream().anyMatch(username::equalsIgnoreCase)
                    || unassigned.stream().anyMatch(username::equalsIgnoreCase);
            if (named) {
                throw new IllegalArgumentException(username + " is already named in this workflow");
            }
        }

        private Integer validate() {
            Integer k = annotationsPerItem;
            if (k == null && !assignees.isEmpty()) {
                k = assignees.size();
            }
            if (k != null && assignees.size() > k) {
                throw new IllegalArgumentException(assignees.size() + " assignees exceed k = " + k);
            }
            assignees.forEach((username, answers) -> {
                if (answers.size() > itemCount) {
                    throw new IllegalArgumentException(username + " has more answers than the split has items");
                }
            });
            for (String username : disabled) {
                if (!assignees.containsKey(username) && !unassigned.contains(username)) {
                    throw new IllegalArgumentException(username + " is not named in this workflow");
                }
            }
            for (PlannedResolution resolution : resolutions.values()) {
                int item = resolution.item();
                if (item < 0 || item >= itemCount) {
                    throw new IllegalArgumentException("No item at position " + item);
                }
                long answered = assignees.values().stream().filter(answers -> answers.size() > item).count();
                if (k == null || answered != k) {
                    throw new IllegalArgumentException("Item " + item + " has " + answered + " answers, not k = " + k
                            + ", so it cannot be resolved yet");
                }
                requireConsistent(resolution, k);
            }
            return k;
        }

        private void requireConsistent(PlannedResolution resolution, int k) {
            int item = resolution.item();
            List<Object> answers = assignees.values().stream().filter(given -> given.size() > item)
                    .map(given -> (Object) given.get(item)).toList();
            if (resolution.method() == ResolutionMethod.AUTO_SCALE) {
                double mean = answers.stream().mapToInt(rating -> (Integer) rating).sum() / (double) k;
                if (Double.compare(mean, resolution.mean()) != 0) {
                    throw new IllegalArgumentException("Item " + item + "'s ratings " + answers + " average " + mean
                            + ", not " + resolution.mean());
                }
                return;
            }
            boolean majority = answers.stream().anyMatch(label -> 2 * Collections.frequency(answers, label) > k);
            if (resolution.method() == ResolutionMethod.ADJUDICATED && majority) {
                throw new IllegalArgumentException("Item " + item + "'s answers " + answers
                        + " have a strict majority, so it is not a dispute");
            }
            if (resolution.method() == ResolutionMethod.MAJORITY
                    && 2 * Collections.frequency(answers, resolution.label()) <= k) {
                throw new IllegalArgumentException(resolution.label() + " does not hold a strict majority of item "
                        + item + "'s answers " + answers);
            }
        }

        private Map<String, User> newAccounts(TestWorkspace workspace) {
            List<String> names = new ArrayList<>(assignees.keySet());
            names.addAll(unassigned);
            Map<String, User> created = new HashMap<>();
            for (String username : names) {
                User existing = workspace.store().read(session ->
                        session.users().findByUsername(username).orElse(null));
                if (existing != null && existing.getRole() != Role.ANNOTATOR) {
                    throw new IllegalArgumentException(username + " exists and is not an annotator");
                }
                if (existing == null) {
                    created.put(username, TestAccounts.annotator(username, TestWorkspace.PASSWORD,
                            AccountStatus.ACTIVE));
                }
            }
            return created;
        }

        private List<ResolvedSource> writeCorpus(TestWorkspace workspace) {
            // Each workflow gets its own folder, so seeding a second project never rewrites a file
            // the first project recorded a hash for.
            int corpus = 1;
            while (Files.exists(workspace.paths().mediaDirectory().resolve("corpus-" + corpus))) {
                corpus++;
            }
            List<ResolvedSource> sources = new ArrayList<>();
            for (int item = 1; item <= itemCount; item++) {
                sources.add(workspace.writeSource("corpus-" + corpus + "/item-" + item + ".txt",
                        "Synthetic item " + item + " of corpus " + corpus));
            }
            return sources;
        }

        private ClassificationWorkflow save(RepositorySession session, Integer k, Map<String, User> newAccounts,
                List<ResolvedSource> sources) {
            long ownerId = session.users().listByRole(Role.ADJUDICATOR).getFirst().getId();
            long projectId = session.projects().save(Records.project("Synthetic project")).getId();
            TaxonomySettings settings = kind == TaxonomyKind.SCALE
                    ? Records.scaleSettings(projectId, minimum, maximum)
                    : Records.settings(projectId);
            session.taxonomySettings().save(settings);
            Map<String, Long> labelIds = new HashMap<>();
            for (int index = 0; index < labelKeys.size(); index++) {
                String key = labelKeys.get(index);
                labelIds.put(key, session.labels().save(Records.label(projectId, key, index + 1)).getId());
            }

            List<Long> itemIds = new ArrayList<>();
            for (ResolvedSource source : sources) {
                itemIds.add(session.items().save(Records.item(projectId, source)).getId());
            }
            Split split = Records.split(projectId, "Batch 1");
            split.setAnnotationsPerItem(k);
            split.setRequestedBatchSize(itemCount);
            long splitId = session.splits().save(split).getId();
            for (int index = 0; index < itemIds.size(); index++) {
                session.splitItems().save(Records.membership(splitId, itemIds.get(index), index + 1));
            }

            Map<String, Long> accountIds = new HashMap<>();
            List<String> names = new ArrayList<>(assignees.keySet());
            names.addAll(unassigned);
            for (String username : names) {
                User account = newAccounts.containsKey(username)
                        ? newAccounts.get(username)
                        : session.users().findByUsername(username).orElseThrow();
                if (disabled.contains(username)) {
                    account.setAccountStatus(AccountStatus.DISABLED);
                }
                accountIds.put(username, session.users().save(account).getId());
            }

            Map<String, Long> assignmentIds = new HashMap<>();
            assignees.forEach((username, answers) -> {
                long annotatorId = accountIds.get(username);
                Assignment assignment = Records.assignment(splitId, annotatorId);
                assignment.setStatus(answers.isEmpty() ? AssignmentStatus.NOT_STARTED
                        : answers.size() == itemCount ? AssignmentStatus.SUBMITTED : AssignmentStatus.IN_PROGRESS);
                long assignmentId = session.assignments().save(assignment).getId();
                assignmentIds.put(username, assignmentId);
                for (int index = 0; index < answers.size(); index++) {
                    Annotation answer = answers.get(index) instanceof Integer rating
                            ? Records.scaleAnswer(itemIds.get(index), assignmentId, annotatorId, rating)
                            : Records.answer(itemIds.get(index), assignmentId, annotatorId,
                                    labelIds.get((String) answers.get(index)));
                    session.annotations().insert(answer);
                }
            });

            for (PlannedResolution planned : resolutions.values()) {
                long itemId = itemIds.get(planned.item());
                Resolution resolution = planned.method() == ResolutionMethod.AUTO_SCALE
                        ? Records.scaleResolution(itemId, planned.mean())
                        : Records.resolution(itemId, labelIds.get(planned.label()));
                resolution.setMethod(planned.method());
                if (planned.method() == ResolutionMethod.ADJUDICATED) {
                    resolution.setDecidedByUserId(ownerId);
                }
                session.resolutions().save(resolution);
            }
            return new ClassificationWorkflow(ownerId, projectId, splitId, itemIds, labelIds, accountIds,
                    assignmentIds);
        }
    }

    private record PlannedResolution(int item, ResolutionMethod method, String label, Double mean) {
    }
}
