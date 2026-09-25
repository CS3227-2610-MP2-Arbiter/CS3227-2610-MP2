package arbiter.data.json;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import arbiter.data.annotation.AnnotationRepository;
import arbiter.data.project.AssignmentRepository;
import arbiter.data.project.ItemRepository;
import arbiter.data.project.LabelRepository;
import arbiter.data.project.ProjectRepository;
import arbiter.data.project.SplitItemRepository;
import arbiter.data.project.SplitRepository;
import arbiter.data.project.TaxonomySettingsRepository;
import arbiter.data.resolution.ResolutionRepository;
import arbiter.data.user.UserRepository;
import arbiter.model.annotation.Annotation;
import arbiter.model.project.Assignment;
import arbiter.model.project.AssignmentStatus;
import arbiter.model.project.Item;
import arbiter.model.project.Label;
import arbiter.model.project.Project;
import arbiter.model.project.Split;
import arbiter.model.project.SplitItem;
import arbiter.model.project.TaxonomySettings;
import arbiter.model.resolution.Resolution;
import arbiter.model.user.AccountStatus;
import arbiter.model.user.Role;
import arbiter.model.user.User;

/** Repository access to one private snapshot during a read or write action. */
public final class RepositorySession {
    private final JsonSnapshot state;
    private final boolean writable;
    private long nextId;
    private boolean active = true;

    RepositorySession(JsonSnapshot state, boolean writable) {
        this.state = state;
        this.writable = writable;
        this.nextId = state.nextId();
    }

    JsonSnapshot snapshot() {
        checkActive();
        return new JsonSnapshot(state.schemaVersion(), nextId, state.users(), state.projects(),
                state.taxonomySettings(), state.labels(), state.items(), state.splits(),
                state.splitItems(), state.assignments(), state.annotations(), state.resolutions());
    }

    void close() {
        active = false;
    }

    /** Returns whether this workspace has never stored a record. */
    public boolean isPristine() {
        checkActive();
        return nextId == 1;
    }

    /** Returns account operations for this action. */
    public UserRepository users() {
        checkActive();
        return new UserRepository() {
            @Override
            public User save(User user) {
                return RepositorySession.this.save(state.users(), user, User::getId, User::setId, User.class);
            }

            @Override
            public Optional<User> findById(long id) {
                return find(state.users(), User::getId, id, User.class);
            }

            @Override
            public Optional<User> findByUsername(String username) {
                Objects.requireNonNull(username, "username");
                return matching(state.users(), user -> username.equalsIgnoreCase(user.getUsername()),
                        User.class).stream().findFirst();
            }

            @Override
            public List<User> listAll() {
                return matching(state.users(), user -> true, User.class);
            }

            @Override
            public List<User> listByRole(Role role) {
                return matching(state.users(), user -> user.getRole() == role, User.class);
            }

            @Override
            public long countActiveByRole(Role role) {
                checkActive();
                return state.users().stream().filter(user -> user.getRole() == role
                        && user.getAccountStatus() == AccountStatus.ACTIVE).count();
            }
        };
    }

    /** Returns project operations for this action. */
    public ProjectRepository projects() {
        checkActive();
        return new ProjectRepository() {
            @Override
            public Project save(Project project) {
                return RepositorySession.this.save(state.projects(), project, Project::getId,
                        Project::setId, Project.class);
            }

            @Override
            public Optional<Project> findById(long id) {
                return find(state.projects(), Project::getId, id, Project.class);
            }

            @Override
            public List<Project> listAll() {
                return matching(state.projects(), project -> true, Project.class);
            }

            @Override
            public void deleteById(long id) {
                deleteProject(id);
            }
        };
    }

    /** Returns taxonomy settings operations for this action. */
    public TaxonomySettingsRepository taxonomySettings() {
        checkActive();
        return new TaxonomySettingsRepository() {
            @Override
            public TaxonomySettings save(TaxonomySettings settings) {
                return RepositorySession.this.save(state.taxonomySettings(), settings,
                        TaxonomySettings::getId, TaxonomySettings::setId, TaxonomySettings.class);
            }

            @Override
            public Optional<TaxonomySettings> findById(long id) {
                return find(state.taxonomySettings(), TaxonomySettings::getId, id, TaxonomySettings.class);
            }

            @Override
            public Optional<TaxonomySettings> findByProject(long projectId) {
                return matching(state.taxonomySettings(), settings -> equalsId(settings.getProjectId(), projectId),
                        TaxonomySettings.class).stream().findFirst();
            }
        };
    }

    /** Returns label operations for this action. */
    public LabelRepository labels() {
        checkActive();
        return new LabelRepository() {
            @Override
            public Label save(Label label) {
                return RepositorySession.this.save(state.labels(), label, Label::getId, Label::setId,
                        Label.class);
            }

            @Override
            public Optional<Label> findById(long id) {
                return find(state.labels(), Label::getId, id, Label.class);
            }

            @Override
            public List<Label> listByProject(long projectId) {
                return matching(state.labels(), label -> equalsId(label.getProjectId(), projectId),
                        Label.class, Comparator.comparing(Label::getSequence));
            }

            @Override
            public void deleteById(long id) {
                checkWritable();
                if (state.annotations().stream().anyMatch(answer -> equalsId(answer.getLabelId(), id))
                        || state.resolutions().stream().anyMatch(result -> equalsId(result.getLabelId(), id))) {
                    throw new JsonStoreException("A submitted answer or resolution still uses this label");
                }
                removeIf(state.labels(), label -> equalsId(label.getId(), id));
            }
        };
    }

    /** Returns imported-item operations for this action. */
    public ItemRepository items() {
        checkActive();
        return new ItemRepository() {
            @Override
            public Item save(Item item) {
                return RepositorySession.this.save(state.items(), item, Item::getId, Item::setId, Item.class);
            }

            @Override
            public Optional<Item> findById(long id) {
                return find(state.items(), Item::getId, id, Item.class);
            }

            @Override
            public List<Item> listByProject(long projectId) {
                return matching(state.items(), item -> equalsId(item.getProjectId(), projectId), Item.class);
            }

            @Override
            public Optional<Item> findByProjectAndHash(long projectId, String contentHash) {
                Objects.requireNonNull(contentHash, "contentHash");
                return matching(state.items(), item -> equalsId(item.getProjectId(), projectId)
                        && contentHash.equals(item.getContentHash()), Item.class).stream().findFirst();
            }

            @Override
            public void deleteById(long id) {
                deleteItem(id);
            }

            @Override
            public long countByProject(long projectId) {
                checkActive();
                return state.items().stream().filter(item -> equalsId(item.getProjectId(), projectId)).count();
            }
        };
    }

    /** Returns split operations for this action. */
    public SplitRepository splits() {
        checkActive();
        return new SplitRepository() {
            @Override
            public Split save(Split split) {
                return RepositorySession.this.save(state.splits(), split, Split::getId, Split::setId,
                        Split.class);
            }

            @Override
            public Optional<Split> findById(long id) {
                return find(state.splits(), Split::getId, id, Split.class);
            }

            @Override
            public List<Split> listByProject(long projectId) {
                return matching(state.splits(), split -> equalsId(split.getProjectId(), projectId), Split.class);
            }

            @Override
            public void deleteById(long id) {
                deleteSplit(id);
            }
        };
    }

    /** Returns split-membership operations for this action. */
    public SplitItemRepository splitItems() {
        checkActive();
        return new SplitItemRepository() {
            @Override
            public SplitItem save(SplitItem splitItem) {
                return RepositorySession.this.save(state.splitItems(), splitItem, SplitItem::getId,
                        SplitItem::setId, SplitItem.class);
            }

            @Override
            public List<SplitItem> listBySplit(long splitId) {
                return matching(state.splitItems(), member -> equalsId(member.getSplitId(), splitId),
                        SplitItem.class, Comparator.comparing(SplitItem::getSequence));
            }

            @Override
            public Optional<SplitItem> findByItem(long itemId) {
                return matching(state.splitItems(), member -> equalsId(member.getItemId(), itemId),
                        SplitItem.class).stream().findFirst();
            }

            @Override
            public long countBySplit(long splitId) {
                checkActive();
                return state.splitItems().stream().filter(member -> equalsId(member.getSplitId(), splitId)).count();
            }
        };
    }

    /** Returns assignment operations for this action. */
    public AssignmentRepository assignments() {
        checkActive();
        return new AssignmentRepository() {
            @Override
            public Assignment save(Assignment assignment) {
                return RepositorySession.this.save(state.assignments(), assignment, Assignment::getId,
                        Assignment::setId, Assignment.class);
            }

            @Override
            public Optional<Assignment> findById(long id) {
                return find(state.assignments(), Assignment::getId, id, Assignment.class);
            }

            @Override
            public List<Assignment> listByAnnotator(long annotatorId) {
                return matching(state.assignments(), assignment -> equalsId(assignment.getAnnotatorId(),
                        annotatorId), Assignment.class);
            }

            @Override
            public List<Assignment> listBySplit(long splitId) {
                return matching(state.assignments(), assignment -> equalsId(assignment.getSplitId(), splitId),
                        Assignment.class);
            }

            @Override
            public List<Assignment> listByStatus(AssignmentStatus status) {
                return matching(state.assignments(), assignment -> assignment.getStatus() == status,
                        Assignment.class);
            }
        };
    }

    /** Returns immutable-submission operations for this action. */
    public AnnotationRepository annotations() {
        checkActive();
        return new AnnotationRepository() {
            @Override
            public Annotation insert(Annotation annotation) {
                if (annotation.getId() != null) {
                    throw new JsonStoreException("A submitted answer cannot be updated");
                }
                return RepositorySession.this.save(state.annotations(), annotation, Annotation::getId,
                        Annotation::setId, Annotation.class);
            }

            @Override
            public Optional<Annotation> findById(long id) {
                return find(state.annotations(), Annotation::getId, id, Annotation.class);
            }

            @Override
            public List<Annotation> listByItem(long itemId) {
                return matching(state.annotations(), answer -> equalsId(answer.getItemId(), itemId),
                        Annotation.class);
            }

            @Override
            public Optional<Annotation> findByItemAndAnnotator(long itemId, long annotatorId) {
                return matching(state.annotations(), answer -> equalsId(answer.getItemId(), itemId)
                        && equalsId(answer.getAnnotatorId(), annotatorId), Annotation.class).stream().findFirst();
            }

            @Override
            public List<Annotation> listByAssignment(long assignmentId) {
                return matching(state.annotations(), answer -> equalsId(answer.getAssignmentId(), assignmentId),
                        Annotation.class);
            }
        };
    }

    /** Returns resolution operations for this action. */
    public ResolutionRepository resolutions() {
        checkActive();
        return new ResolutionRepository() {
            @Override
            public Resolution save(Resolution resolution) {
                return RepositorySession.this.save(state.resolutions(), resolution, Resolution::getId,
                        Resolution::setId, Resolution.class);
            }

            @Override
            public Optional<Resolution> findByItem(long itemId) {
                return matching(state.resolutions(), result -> equalsId(result.getItemId(), itemId),
                        Resolution.class).stream().findFirst();
            }

            @Override
            public List<Resolution> listByItems(List<Long> itemIds) {
                Objects.requireNonNull(itemIds, "itemIds");
                return matching(state.resolutions(), result -> itemIds.contains(result.getItemId()),
                        Resolution.class);
            }
        };
    }

    private <T> T save(List<T> rows, T input, Function<T, Long> getId, BiConsumer<T, Long> setId,
            Class<T> type) {
        checkWritable();
        T value = copy(Objects.requireNonNull(input, "input"), type);
        Long id = getId.apply(value);
        if (id == null) {
            setId.accept(value, nextId++);
            rows.add(value);
        } else {
            int index = indexOf(rows, getId, id);
            if (index < 0) {
                throw new JsonStoreException("No stored record has identifier " + id);
            }
            rows.set(index, value);
        }
        return copy(value, type);
    }

    private <T> Optional<T> find(List<T> rows, Function<T, Long> getId, long id, Class<T> type) {
        checkActive();
        int index = indexOf(rows, getId, id);
        return index < 0 ? Optional.empty() : Optional.of(copy(rows.get(index), type));
    }

    private <T> List<T> matching(List<T> rows, Predicate<T> predicate, Class<T> type) {
        return matching(rows, predicate, type, null);
    }

    private <T> List<T> matching(List<T> rows, Predicate<T> predicate, Class<T> type,
            Comparator<T> order) {
        checkActive();
        List<T> selected = new ArrayList<>();
        for (T row : rows) {
            if (predicate.test(row)) {
                selected.add(copy(row, type));
            }
        }
        if (order != null) {
            selected.sort(order);
        }
        return selected;
    }

    private <T> void removeIf(List<T> rows, Predicate<T> predicate) {
        checkWritable();
        rows.removeIf(predicate);
    }

    private void deleteProject(long id) {
        checkWritable();
        List<Long> itemIds = state.items().stream().filter(item -> equalsId(item.getProjectId(), id))
                .map(Item::getId).toList();
        List<Long> splitIds = state.splits().stream().filter(split -> equalsId(split.getProjectId(), id))
                .map(Split::getId).toList();
        for (Long itemId : itemIds) {
            deleteItem(itemId);
        }
        for (Long splitId : splitIds) {
            state.assignments().removeIf(assignment -> equalsId(assignment.getSplitId(), splitId));
            state.splitItems().removeIf(member -> equalsId(member.getSplitId(), splitId));
        }
        state.splits().removeIf(split -> equalsId(split.getProjectId(), id));
        state.labels().removeIf(label -> equalsId(label.getProjectId(), id));
        state.taxonomySettings().removeIf(settings -> equalsId(settings.getProjectId(), id));
        state.projects().removeIf(project -> equalsId(project.getId(), id));
    }

    private void deleteItem(long id) {
        checkWritable();
        state.annotations().removeIf(answer -> equalsId(answer.getItemId(), id));
        state.resolutions().removeIf(result -> equalsId(result.getItemId(), id));
        state.splitItems().removeIf(member -> equalsId(member.getItemId(), id));
        state.items().removeIf(item -> equalsId(item.getId(), id));
    }

    private void deleteSplit(long id) {
        checkWritable();
        if (state.assignments().stream().anyMatch(assignment -> equalsId(assignment.getSplitId(), id))) {
            throw new JsonStoreException("This split still has assignments");
        }
        state.splitItems().removeIf(member -> equalsId(member.getSplitId(), id));
        state.splits().removeIf(split -> equalsId(split.getId(), id));
    }

    private <T> T copy(T value, Class<T> type) {
        return JsonStore.JSON.readValue(JsonStore.JSON.writeValueAsBytes(value), type);
    }

    private static <T> int indexOf(List<T> rows, Function<T, Long> getId, long id) {
        for (int index = 0; index < rows.size(); index++) {
            if (equalsId(getId.apply(rows.get(index)), id)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean equalsId(Long actual, long expected) {
        return actual != null && actual == expected;
    }

    private void checkActive() {
        if (!active) {
            throw new IllegalStateException("The repository action has ended");
        }
    }

    private void checkWritable() {
        checkActive();
        if (!writable) {
            throw new IllegalStateException("A read action cannot modify workspace data");
        }
    }
}
