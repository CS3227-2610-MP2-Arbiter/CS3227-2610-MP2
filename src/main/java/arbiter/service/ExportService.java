package arbiter.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.annotation.Annotation;
import arbiter.model.project.Item;
import arbiter.model.project.Label;
import arbiter.model.project.OutputFormat;
import arbiter.model.project.Project;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.resolution.Resolution;
import arbiter.model.resolution.ResolutionMethod;
import arbiter.model.user.User;
import arbiter.workspace.SourceException;
import arbiter.workspace.SourceResolver;
import arbiter.workspace.WorkspacePaths;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

/**
 * Writes a project's dataset in its output format to the workspace's {@code exports/} folder, with compact
 * provenance for each current decision (#37, rules 15 and 17).
 *
 * <p>Every public method first requires the signed-in adjudicator through {@link AuthService#requireAdjudicator},
 * so anyone else gets its {@link AuthException}. They read every annotator's answers and names, so only adjudicator
 * screens may call them (rule 1). Neither changes a stored record.
 */
public final class ExportService {
    /** The CSV's columns for an item's own fields, in order. */
    private static final List<Column<ExportedItem>> ITEM_COLUMNS = List.of(new Column<>("path", ExportedItem::path),
            new Column<>("status", ExportedItem::status), new Column<>("answer", ExportedItem::answer),
            new Column<>("method", ExportedItem::method), new Column<>("decided_by", ExportedItem::decidedBy),
            new Column<>("decided_at", ExportedItem::decidedAt));
    /** The CSV's columns for one submission, repeated in numbered groups such as {@code annotator_1}. */
    private static final List<Column<Submission>> SUBMISSION_COLUMNS = List.of(
            new Column<>("annotator", Submission::annotator), new Column<>("answer", Submission::answer),
            new Column<>("submitted_at", Submission::submittedAt));

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();
    private static final CsvMapper CSV_MAPPER = new CsvMapper();

    private final JsonStore store;
    private final AuthService auth;
    private final WorkspacePaths paths;
    private final SourceResolver sources;

    /** Exports one workspace's projects on behalf of whoever is signed in to {@code auth}. */
    public ExportService(JsonStore store, AuthService auth, WorkspacePaths paths) {
        this.store = Objects.requireNonNull(store, "store");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.paths = Objects.requireNonNull(paths, "paths");
        this.sources = new SourceResolver(paths);
    }

    /**
     * Returns where a project's export would be written and how many of its items have a resolution, read from one
     * snapshot. It writes nothing.
     *
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if no project has this identifier
     */
    public ExportSummary preview(long projectId) {
        auth.requireAdjudicator();
        return store.read(session -> {
            Project project = requireProject(session, projectId);
            List<Item> items = session.items().listByProject(projectId);
            long unresolved = ProjectService.unresolvedCount(session, items);
            return new ExportSummary(file(project), items.size() - unresolved, unresolved);
        });
    }

    /**
     * Writes a project's dataset, read from one snapshot, to its file under {@code exports/} in its output format,
     * replacing any earlier export whole (#37). Every item is written in registration order: a resolved one with
     * its stored decision and its submitted answers by submission time, and any other one marked
     * {@code UNRESOLVED}.
     *
     * <p>Every item's source is checked before anything is written (rule 21), and the first that fails stops the
     * export, so an earlier export is left as it was.
     *
     * @return where the dataset was written and how many of its items are resolved
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if no project has this identifier, or the file cannot be written
     * @throws SourceException if an item's source is missing, unreadable or changed, with the resolver's message,
     *     which names the file
     */
    public ExportSummary export(long projectId) {
        auth.requireAdjudicator();
        Snapshot snapshot = store.read(session -> {
            Project project = requireProject(session, projectId);
            TaxonomyKind kind = session.taxonomySettings().findByProject(projectId).orElseThrow().getKind();
            Map<Long, String> keys = session.labels().listByProject(projectId).stream()
                    .collect(Collectors.toMap(Label::getId, Label::getKey));
            Map<Long, String> usernames = session.users().listAll().stream()
                    .collect(Collectors.toMap(User::getId, User::getUsername));
            List<Item> items = CorpusService.itemsOf(session, projectId);
            List<ExportedItem> exported = items.stream()
                    .map(item -> exportedItem(session, item, keys, usernames))
                    .toList();
            return new Snapshot(file(project), project.getOutputFormat(), items,
                    new Dataset(project.getName(), kind, exported));
        });
        // The files are read outside the store's action, which holds the workspace's data, not its media.
        for (Item item : snapshot.items()) {
            sources.resolve(item.getPath(), item.getContentHash());
        }
        Dataset dataset = snapshot.dataset();
        byte[] bytes = switch (snapshot.format()) {
        case CSV -> csv(dataset.items());
        case JSON -> JSON_MAPPER.writeValueAsBytes(dataset);
        };
        try {
            JsonStore.publishAtomically(snapshot.file(), bytes);
        } catch (IOException e) {
            throw new ProjectException("The export could not be written to " + snapshot.file()
                    + ". If another program has it open, close it and try again.", e);
        }
        long resolved = dataset.items().stream().filter(item -> item.status() == Status.RESOLVED).count();
        return new ExportSummary(snapshot.file(), resolved, dataset.items().size() - resolved);
    }

    private static Project requireProject(RepositorySession session, long projectId) {
        return session.projects().findById(projectId)
                .orElseThrow(() -> new ProjectException("This project no longer exists"));
    }

    /** Returns the file a project is exported to, such as {@code exports/project-3.csv}. */
    private Path file(Project project) {
        return paths.exportsDirectory().resolve("project-" + project.getId() + "."
                + project.getOutputFormat().name().toLowerCase(Locale.ROOT));
    }

    /** Returns an item as the export writes it, from its stored resolution and submitted answers if it has one. */
    private static ExportedItem exportedItem(RepositorySession session, Item item, Map<Long, String> keys,
            Map<Long, String> usernames) {
        Resolution resolution = session.resolutions().findByItem(item.getId()).orElse(null);
        if (resolution == null) {
            return new ExportedItem(item.getPath(), Status.UNRESOLVED, null, null, null, null, List.of());
        }
        List<Submission> submissions = session.annotations().listByItem(item.getId()).stream()
                .sorted(Comparator.comparing(Annotation::getSubmittedAt).thenComparing(Annotation::getId))
                .map(annotation -> new Submission(usernames.get(annotation.getAnnotatorId()),
                        answer(keys, annotation.getLabelId(), annotation.getScaleValue()),
                        annotation.getSubmittedAt()))
                .toList();
        Long deciderId = resolution.getDecidedByUserId();
        return new ExportedItem(item.getPath(), Status.RESOLVED,
                answer(keys, resolution.getLabelId(), resolution.getScaleValue()), resolution.getMethod(),
                deciderId == null ? null : usernames.get(deciderId), resolution.getDecidedAt(), submissions);
    }

    /**
     * Returns an answer as the export writes it: a SINGLE label's key, or a SCALE number. A label that is not one of
     * the project's, which only corrupt data can hold, fails rather than being written blank.
     */
    private static Object answer(Map<Long, String> keys, Long labelId, Number scaleValue) {
        return labelId == null ? scaleValue : Optional.ofNullable(keys.get(labelId)).orElseThrow();
    }

    /**
     * Returns the items as CSV, one row each: their fields, then a numbered group of columns for each submission,
     * padded to the most submissions any item has.
     */
    private static byte[] csv(List<ExportedItem> items) {
        int groups = items.stream().mapToInt(item -> item.submissions().size()).max().orElse(0);
        CsvSchema.Builder schema = CsvSchema.builder().setUseHeader(true);
        ITEM_COLUMNS.forEach(column -> schema.addColumn(column.name()));
        for (int number = 1; number <= groups; number++) {
            for (Column<Submission> column : SUBMISSION_COLUMNS) {
                schema.addColumn(column.name() + "_" + number);
            }
        }
        List<List<Object>> rows = new ArrayList<>();
        for (ExportedItem item : items) {
            // Jackson leaves the columns after a shorter row's last value empty.
            List<Object> row = new ArrayList<>(cells(ITEM_COLUMNS, item));
            for (Submission submission : item.submissions()) {
                row.addAll(cells(SUBMISSION_COLUMNS, submission));
            }
            rows.add(row);
        }
        return CSV_MAPPER.writer(schema.build()).writeValueAsBytes(rows);
    }

    /** Returns the cells these columns take from one value, in column order. */
    private static <T> List<Object> cells(List<Column<T>> columns, T value) {
        return columns.stream().map(column -> column.cell().apply(value)).toList();
    }

    /** What {@link #export} reads from one snapshot, before it checks the sources. */
    private record Snapshot(Path file, OutputFormat format, List<Item> items, Dataset dataset) {
    }

    /**
     * A project's dataset as the JSON export writes it; the CSV export writes its items.
     *
     * @param project the project's name
     * @param taxonomyKind the project's taxonomy kind
     * @param items every item in registration order
     */
    private record Dataset(String project, TaxonomyKind taxonomyKind, List<ExportedItem> items) {
    }

    /** Whether an exported item has a resolution. */
    private enum Status {
        RESOLVED,
        UNRESOLVED
    }

    /**
     * One item as the export writes it. An unresolved item has only its path and status.
     *
     * @param path the item's stored path
     * @param status {@code RESOLVED} if the item has a resolution, else {@code UNRESOLVED}
     * @param answer the resolved label's key or SCALE mean, or null if the item is unresolved
     * @param method how it was resolved, or null if it is unresolved
     * @param decidedBy the adjudicator's username for an {@code ADJUDICATED} decision, else null
     * @param decidedAt when it was resolved, or null if it is unresolved
     * @param submissions its submitted answers by submission time, or none if it is unresolved
     */
    private record ExportedItem(String path, Status status, Object answer, ResolutionMethod method, String decidedBy,
            Instant decidedAt, List<Submission> submissions) {
    }

    /**
     * One submitted answer as the export writes it.
     *
     * @param annotator the annotator's username, even if their account is deactivated
     * @param answer the chosen label's key or SCALE rating
     * @param submittedAt when it was submitted
     */
    private record Submission(String annotator, Object answer, Instant submittedAt) {
    }

    /**
     * One CSV column.
     *
     * @param name its header, or for a submission column the header each numbered group extends
     * @param cell what it holds for one value
     */
    private record Column<T>(String name, Function<T, Object> cell) {
    }
}
