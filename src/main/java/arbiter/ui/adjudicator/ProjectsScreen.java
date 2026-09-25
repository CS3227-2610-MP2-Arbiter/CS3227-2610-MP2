package arbiter.ui.adjudicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import arbiter.data.json.JsonStoreException;
import arbiter.model.project.OutputFormat;
import arbiter.model.project.TaxonomyKind;
import arbiter.service.AuthException;
import arbiter.service.CorpusService;
import arbiter.service.ProjectException;
import arbiter.service.ProjectService;
import arbiter.service.ProjectSummary;
import arbiter.ui.shared.Components;
import arbiter.ui.shared.Dialogs;
import arbiter.ui.shared.ErrorMessages;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * The adjudicator's project list, where projects are created, opened, and deleted before their first
 * assignment.
 */
public final class ProjectsScreen {
    private static final String DELETE_FAILED = "The project could not be deleted";

    private final Window owner;
    private final ProjectService projects;
    private final CorpusService corpus;
    private final StackPane content = new StackPane();

    /** Creates the screen, whose dialogs belong to {@code owner}. */
    public ProjectsScreen(Window owner, ProjectService projects, CorpusService corpus) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.projects = Objects.requireNonNull(projects, "projects");
        this.corpus = Objects.requireNonNull(corpus, "corpus");
    }

    /** Returns the screen's content, showing the current project list. */
    public Node content() {
        showList();
        return content;
    }

    private void showList() {
        List<ProjectSummary> summaries;
        try {
            summaries = projects.list();
        } catch (AuthException | JsonStoreException e) {
            Dialogs.showError(owner, "The projects could not be loaded", e);
            content.getChildren().setAll(Components.emptyState("Projects", "The projects could not be loaded."));
            return;
        }
        Button create = new Button("New project");
        create.setOnAction(event -> showCreateForm());
        if (summaries.isEmpty()) {
            VBox empty = Components.emptyState("Projects", "There are no projects yet.");
            empty.getChildren().add(create);
            content.getChildren().setAll(empty);
            return;
        }
        TableView<ProjectSummary> table = new TableView<>(FXCollections.observableArrayList(summaries));
        table.getColumns().setAll(List.of(
                Components.column("Name", ProjectSummary::name),
                Components.column("Taxonomy kind", ProjectSummary::kind),
                Components.column("Output format", ProjectSummary::outputFormat),
                Components.column("Items", ProjectSummary::itemCount),
                Components.column("Splits", ProjectSummary::splitCount),
                Components.column("Assignments", ProjectSummary::assignmentCount),
                Components.column("Unresolved", ProjectSummary::unresolvedCount),
                Components.buttonColumn("Open", project -> false, this::open),
                Components.buttonColumn("Delete", project -> project.assignmentCount() > 0, this::delete)));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().setAll(Components.page(Components.pageTitle("Projects"), create,
                Components.hint("A project can be deleted only before its first assignment."), table));
    }

    private void showCreateForm() {
        TextField name = new TextField();
        name.setPromptText("Name");
        TextField description = new TextField();
        description.setPromptText("Description (optional)");
        ToggleGroup kinds = new ToggleGroup();
        ToggleGroup formats = new ToggleGroup();
        Label error = Components.errorText();

        Button create = new Button("Create project");
        create.setDefaultButton(true);
        create.setOnAction(event -> {
            error.setText("");
            try {
                projects.create(name.getText(), description.getText(), selected(kinds, TaxonomyKind.class),
                        selected(formats, OutputFormat.class));
                showList();
            } catch (ProjectException e) {
                error.setText(ErrorMessages.of(e));
            } catch (AuthException | JsonStoreException e) {
                Dialogs.showError(owner, "The project could not be created", e);
            }
        });
        Button cancel = new Button("Cancel");
        cancel.setCancelButton(true);
        cancel.setOnAction(event -> showList());
        ButtonBar actions = new ButtonBar();
        ButtonBar.setButtonData(create, ButtonBar.ButtonData.OK_DONE);
        ButtonBar.setButtonData(cancel, ButtonBar.ButtonData.CANCEL_CLOSE);
        actions.getButtons().setAll(create, cancel);

        List<Node> controls = new ArrayList<>(List.of(Components.pageTitle("New project"), name, description,
                new Label("Taxonomy kind")));
        for (TaxonomyKind kind : TaxonomyKind.values()) {
            controls.add(choice(kinds, kind));
        }
        controls.add(new Label("Output format"));
        for (OutputFormat format : OutputFormat.values()) {
            controls.add(choice(formats, format));
        }
        controls.add(Components.hint("The taxonomy kind and output format cannot be changed later."));
        controls.add(actions);
        controls.add(error);
        content.getChildren().setAll(Components.form(controls.toArray(Node[]::new)));
    }

    private void delete(ProjectSummary project) {
        boolean confirmed = Dialogs.confirm(owner, "Delete project " + project.name() + "?",
                "This cannot be undone.", "Delete project");
        if (!confirmed) {
            return;
        }
        try {
            projects.delete(project.id());
        } catch (ProjectException e) {
            // The list was out of date, so the reload below shows why.
            Dialogs.showError(owner, DELETE_FAILED, e);
        } catch (AuthException | JsonStoreException e) {
            // Nothing changed, so the list is still current, and reloading would likely report this again.
            Dialogs.showError(owner, DELETE_FAILED, e);
            return;
        }
        showList();
    }

    private void open(ProjectSummary project) {
        content.getChildren().setAll(new ProjectPage(owner, corpus, project, this::showList).content());
    }

    private static RadioButton choice(ToggleGroup group, Enum<?> value) {
        RadioButton button = new RadioButton(value.name());
        button.setToggleGroup(group);
        button.setUserData(value);
        return button;
    }

    private static <T> T selected(ToggleGroup group, Class<T> type) {
        Toggle toggle = group.getSelectedToggle();
        return toggle == null ? null : type.cast(toggle.getUserData());
    }
}
