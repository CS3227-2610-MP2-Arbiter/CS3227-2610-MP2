package arbiter.ui.adjudicator;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import arbiter.data.json.JsonStoreException;
import arbiter.model.project.TaxonomyKind;
import arbiter.service.AuthException;
import arbiter.service.ProjectException;
import arbiter.service.ProjectProgress;
import arbiter.service.ProjectService;
import arbiter.service.SplitSummary;
import arbiter.ui.shared.Components;
import arbiter.ui.shared.Dialogs;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;

/** The adjudicator's read-only project progress view (#33). */
final class ProgressView {
    private static final String LOAD_FAILED = "The project progress could not be loaded";

    private final Window owner;
    private final ProjectService projects;
    private final long projectId;
    private final Runnable showPage;
    private final BiConsumer<SplitSummary, Boolean> showAssign;
    private final Runnable showDisputes;
    private final StackPane content = new StackPane();

    /**
     * Creates the view for one project's progress.
     *
     * @param showPage shows the project page again, reloaded
     * @param showAssign opens the form that assigns annotators to a split, given whether the split's project was
     *     frozen (rule 3) when this view loaded
     * @param showDisputes opens the project's dispute list
     */
    ProgressView(Window owner, ProjectService projects, long projectId, Runnable showPage,
            BiConsumer<SplitSummary, Boolean> showAssign, Runnable showDisputes) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.projects = Objects.requireNonNull(projects, "projects");
        this.projectId = projectId;
        this.showPage = Objects.requireNonNull(showPage, "showPage");
        this.showAssign = Objects.requireNonNull(showAssign, "showAssign");
        this.showDisputes = Objects.requireNonNull(showDisputes, "showDisputes");
    }

    Node content() {
        show();
        return content;
    }

    private void show() {
        Button back = new Button("Back");
        back.setOnAction(event -> showPage.run());
        ProjectProgress progress;
        try {
            progress = projects.progress(projectId);
        } catch (ProjectException | AuthException | JsonStoreException e) {
            Dialogs.showError(owner, LOAD_FAILED, e);
            var empty = Components.emptyState("Progress", LOAD_FAILED + ".");
            empty.getChildren().add(back);
            content.getChildren().setAll(empty);
            return;
        }
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> show());
        Button disputes = new Button("Disputes");
        disputes.setDisable(progress.kind() != TaxonomyKind.SINGLE);
        disputes.setOnAction(event -> showDisputes.run());

        boolean frozen = progress.splits().stream().anyMatch(split -> split.summary().assigned());
        TableView<ProjectProgress.SplitProgress> splits = splitTable(progress.splits(), frozen);
        TableView<ProjectProgress.AssignmentProgressRow> assignments = assignmentTable();
        splits.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) ->
                assignments.setItems(FXCollections.observableArrayList(
                        selected == null ? List.of() : selected.assignments())));
        splits.getSelectionModel().selectFirst();

        content.getChildren().setAll(Components.scrollingPage(back,
                Components.pageTitle("Progress in " + progress.projectName()), refresh, disputes,
                Components.text(progress.itemCount() + " files; " + progress.submitted() + " of "
                        + progress.total() + " assigned answers submitted"),
                Components.text(progress.unresolvedCount() + " unresolved files; "
                        + progress.unresolvedDisputeCount() + " unresolved disputes"),
                Components.hint("Unresolved files include work that is unassigned or incomplete. "
                        + "Only undecided SINGLE disagreements count as disputes."),
                Components.text("Splits"), splits,
                Components.hint("Answer totals use assigned annotator places. Vacant places are shown separately."),
                Components.text("Assignments in selected split"), assignments,
                Components.text("Annotators"), annotatorTable(progress.annotators())));
    }

    /** Returns the splits table, where {@code frozen} says whether the project has had its first assignment. */
    private TableView<ProjectProgress.SplitProgress> splitTable(List<ProjectProgress.SplitProgress> rows,
            boolean frozen) {
        TableView<ProjectProgress.SplitProgress> table = new TableView<>(FXCollections.observableArrayList(rows));
        table.getColumns().setAll(List.of(
                Components.wrappingColumn("Split", split -> split.summary().name()),
                Components.wrappingColumn("Files", split -> Integer.toString(split.summary().itemIds().size())),
                Components.wrappingColumn("Places", split -> places(split.summary())),
                Components.wrappingColumn("Answers", split -> count(split.submitted(), split.total())),
                Components.buttonColumn("Assign", split -> split.summary().full(),
                        split -> showAssign.accept(split.summary(), frozen))));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(Components.hint("No splits are generated yet."));
        table.setPrefHeight(240);
        return table;
    }

    private TableView<ProjectProgress.AssignmentProgressRow> assignmentTable() {
        TableView<ProjectProgress.AssignmentProgressRow> table = new TableView<>();
        table.getColumns().setAll(List.of(
                Components.wrappingColumn("Annotator", ProjectProgress.AssignmentProgressRow::username),
                Components.wrappingColumn("Account", row -> row.accountStatus().toString()),
                Components.wrappingColumn("Status", row -> row.status().toString()),
                Components.wrappingColumn("Answers", row -> count(row.submitted(), row.total()))));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(Components.hint("Select a split to see its assignments."));
        table.setPrefHeight(200);
        return table;
    }

    private static TableView<ProjectProgress.AnnotatorProgress> annotatorTable(
            List<ProjectProgress.AnnotatorProgress> rows) {
        TableView<ProjectProgress.AnnotatorProgress> table = new TableView<>(FXCollections.observableArrayList(rows));
        table.getColumns().setAll(List.of(
                Components.wrappingColumn("Annotator", ProjectProgress.AnnotatorProgress::username),
                Components.wrappingColumn("Account", row -> row.accountStatus().toString()),
                Components.wrappingColumn("Answers", row -> count(row.submitted(), row.total()))));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(Components.hint("No annotators are assigned yet."));
        table.setPrefHeight(240);
        return table;
    }

    /** Describes a split's taken and vacant places (rule 19), such as "2 of 3 (1 vacant)". */
    private static String places(SplitSummary split) {
        return split.assigned() ? split.assignmentCount() + " of " + split.annotationsPerItem() + " ("
                + split.vacantPlaces() + " vacant)" : "Not assigned yet";
    }

    private static String count(long submitted, long total) {
        return submitted + " of " + total;
    }
}
