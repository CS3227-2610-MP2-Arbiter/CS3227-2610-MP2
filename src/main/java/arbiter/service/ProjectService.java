package arbiter.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.project.Item;
import arbiter.model.project.OutputFormat;
import arbiter.model.project.Project;
import arbiter.model.project.Split;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.project.TaxonomySettings;

/**
 * Creates, lists and deletes projects (#24, #30).
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
        long unresolved = items.stream()
                .filter(item -> session.resolutions().findByItem(item.getId()).isEmpty())
                .count();
        return new ProjectSummary(id, project.getName(), kind, project.getOutputFormat(), items.size(),
                splits.size(), assignmentCount(session, splits), unresolved);
    }

    private static long assignmentCount(RepositorySession session, List<Split> splits) {
        return splits.stream().mapToLong(split -> session.assignments().listBySplit(split.getId()).size()).sum();
    }
}
