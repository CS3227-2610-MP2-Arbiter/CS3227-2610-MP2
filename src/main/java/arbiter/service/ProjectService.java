package arbiter.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.project.Assignment;
import arbiter.model.project.Item;
import arbiter.model.project.OutputFormat;
import arbiter.model.project.Project;
import arbiter.model.project.Split;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.project.TaxonomySettings;
import arbiter.model.user.User;

/**
 * Creates, lists and deletes projects (#24, #30), and reads adjudicator progress (#33).
 *
 * <p>Every method first requires the signed-in adjudicator through {@link AuthService#requireAdjudicator},
 * so anyone else gets its {@link AuthException}. Nothing here changes a project's taxonomy kind or output
 * format, which are fixed at creation (rule 4).
 */
public final class ProjectService {
    private static final Pattern NAME_PATTERN = Pattern.compile("[ -~]{1,100}");

    private final JsonStore store;
    private final AuthService auth;

    /** Manages one workspace's projects on behalf of whoever is signed in to {@code auth}. */
    public ProjectService(JsonStore store, AuthService auth) {
        this.store = Objects.requireNonNull(store, "store");
        this.auth = Objects.requireNonNull(auth, "auth");
    }

    /**
     * Creates a project and its taxonomy settings in one committed action, stamped with the current time.
     *
     * <p>The name is stripped of surrounding whitespace and must then meet #24's name rule. A null or blank
     * description is stored as null, and any other description is stored stripped.
     *
     * @return the stored project
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if the name breaks that rule or is already used, or the kind or format
     *     is null; nothing is stored
     */
    public Project create(String name, String description, TaxonomyKind kind, OutputFormat outputFormat) {
        auth.requireAdjudicator();
        String stripped = requireName(name, "Project name");
        if (kind == null) {
            throw new ProjectException("Choose a taxonomy kind");
        }
        if (outputFormat == null) {
            throw new ProjectException("Choose an output format");
        }
        return store.write(session -> {
            boolean taken = session.projects().listAll().stream()
                    .anyMatch(project -> project.getName().equalsIgnoreCase(stripped));
            if (taken) {
                throw new ProjectException("A project with this name already exists");
            }
            Project project = new Project();
            project.setName(stripped);
            project.setDescription(optionalText(description));
            project.setOutputFormat(outputFormat);
            project.setCreatedAt(Instant.now());
            Project stored = session.projects().save(project);

            TaxonomySettings settings = new TaxonomySettings();
            settings.setProjectId(stored.getId());
            settings.setKind(kind);
            session.taxonomySettings().save(settings);
            return stored;
        });
    }

    /**
     * Returns every project in creation order with its taxonomy kind and counts, read from one snapshot.
     *
     * <p>The counts cover every annotator's assignments and the resolutions, so only adjudicator screens
     * may call this (rule 1).
     *
     * @throws AuthException if the caller is not the signed-in adjudicator
     */
    public List<ProjectSummary> list() {
        auth.requireAdjudicator();
        return store.read(session -> session.projects().listAll().stream()
                .sorted(Comparator.comparing(Project::getId))
                .map(project -> summarize(session, project))
                .toList());
    }

    /**
     * Returns #33's adjudicator-only progress from one repository snapshot. The answer denominator covers only
     * assigned split-item pairs; answers from disabled accounts remain counted.
     *
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if no project has this identifier
     */
    public ProjectProgress progress(long projectId) {
        auth.requireAdjudicator();
        return store.read(session -> readProgress(session, projectId));
    }

    /**
     * Deletes a project and every stored record it owns in one committed action, leaving its source files
     * and every account untouched (rule 5).
     *
     * <p>Both checks run inside that action, so a stale project list cannot bypass them (#24).
     *
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if no project has this identifier, or it has had its first assignment
     *     ({@link FirstAssignment}); nothing is changed
     */
    public void delete(long projectId) {
        auth.requireAdjudicator();
        store.write(session -> {
            FirstAssignment.requireNotReached(session, projectId,
                    "A project cannot be deleted after its first assignment");
            session.projects().deleteById(projectId);
            return null;
        });
    }

    /**
     * Strips a name the adjudicator typed of surrounding whitespace and requires it to meet #24's name rule, which
     * label keys also follow (#26).
     *
     * @param subject what the name is, such as "Project name", which starts the message any other name is rejected
     *     with
     * @return the stripped name
     * @throws ProjectException if the name breaks that rule
     */
    static String requireName(String name, String subject) {
        String stripped = name == null ? "" : name.strip();
        if (!NAME_PATTERN.matcher(stripped).matches()) {
            throw new ProjectException(subject + " must be 1 to 100 ASCII letters, digits, spaces or punctuation");
        }
        return stripped;
    }

    /** Returns optional text, such as a description, as it is stored: null if it is null or blank, else stripped. */
    static String optionalText(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    private static ProjectSummary summarize(RepositorySession session, Project project) {
        long id = project.getId();
        TaxonomyKind kind = session.taxonomySettings().findByProject(id).orElseThrow().getKind();
        List<Item> items = session.items().listByProject(id);
        List<Split> splits = session.splits().listByProject(id);
        return new ProjectSummary(id, project.getName(), kind, project.getOutputFormat(), items.size(),
                splits.size(), assignmentCount(session, splits), unresolvedCount(session, items));
    }

    private static ProjectProgress readProgress(RepositorySession session, long projectId) {
        Project project = session.projects().findById(projectId)
                .orElseThrow(() -> new ProjectException("This project no longer exists"));
        TaxonomyKind kind = session.taxonomySettings().findByProject(projectId).orElseThrow().getKind();
        List<Item> items = session.items().listByProject(projectId);
        List<ProjectProgress.SplitProgress> splits = session.splits().listByProject(projectId).stream()
                .sorted(Comparator.comparing(Split::getId))
                .map(split -> summarizeSplit(session, split))
                .toList();
        long submitted = splits.stream().mapToLong(ProjectProgress.SplitProgress::submitted).sum();
        long total = splits.stream().mapToLong(ProjectProgress.SplitProgress::total).sum();
        long disputes = kind == TaxonomyKind.SINGLE ? items.stream()
                .filter(item -> ResolutionService.isUnresolvedDispute(session, item.getId())).count() : 0;
        return new ProjectProgress(projectId, project.getName(), kind, items.size(), submitted, total,
                unresolvedCount(session, items), disputes, splits, summarizeAnnotators(splits));
    }

    private static ProjectProgress.SplitProgress summarizeSplit(RepositorySession session, Split split) {
        SplitSummary summary = CorpusService.summarize(session, split);
        long itemCount = summary.itemIds().size();
        List<ProjectProgress.AssignmentProgressRow> assignments = session.assignments().listBySplit(split.getId())
                .stream().sorted(Comparator.comparing(Assignment::getId))
                .map(assignment -> summarizeAssignment(session, split, assignment, itemCount))
                .toList();
        long submitted = assignments.stream().mapToLong(ProjectProgress.AssignmentProgressRow::submitted).sum();
        long total = itemCount * assignments.size();
        return new ProjectProgress.SplitProgress(summary, submitted, total, assignments);
    }

    private static ProjectProgress.AssignmentProgressRow summarizeAssignment(RepositorySession session, Split split,
            Assignment assignment, long itemCount) {
        User annotator = session.users().findById(assignment.getAnnotatorId()).orElseThrow();
        long submitted = session.annotations().listByAssignment(assignment.getId()).size();
        return new ProjectProgress.AssignmentProgressRow(assignment.getId(), split.getId(), split.getName(),
                annotator.getId(), annotator.getUsername(), annotator.getAccountStatus(), assignment.getStatus(),
                submitted, itemCount);
    }

    private static List<ProjectProgress.AnnotatorProgress> summarizeAnnotators(
            List<ProjectProgress.SplitProgress> splits) {
        Map<Long, List<ProjectProgress.AssignmentProgressRow>> byAnnotator = new LinkedHashMap<>();
        for (ProjectProgress.SplitProgress split : splits) {
            for (ProjectProgress.AssignmentProgressRow assignment : split.assignments()) {
                byAnnotator.computeIfAbsent(assignment.annotatorId(), ignored -> new ArrayList<>()).add(assignment);
            }
        }
        return byAnnotator.entrySet().stream()
                .map(entry -> summarizeAnnotator(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** Counts the items that have no resolution in this session, as the project list and the export report them. */
    static long unresolvedCount(RepositorySession session, List<Item> items) {
        return items.stream()
                .filter(item -> session.resolutions().findByItem(item.getId()).isEmpty())
                .count();
    }

    private static long assignmentCount(RepositorySession session, List<Split> splits) {
        return splits.stream().mapToLong(split -> session.assignments().listBySplit(split.getId()).size()).sum();
    }

    private static ProjectProgress.AnnotatorProgress summarizeAnnotator(long id,
            List<ProjectProgress.AssignmentProgressRow> assignments) {
        ProjectProgress.AssignmentProgressRow first = assignments.getFirst();
        long submitted = assignments.stream().mapToLong(ProjectProgress.AssignmentProgressRow::submitted).sum();
        long total = assignments.stream().mapToLong(ProjectProgress.AssignmentProgressRow::total).sum();
        return new ProjectProgress.AnnotatorProgress(id, first.username(), first.accountStatus(), submitted, total);
    }
}
