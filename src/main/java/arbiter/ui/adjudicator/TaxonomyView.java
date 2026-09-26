package arbiter.ui.adjudicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import arbiter.data.json.JsonStoreException;
import arbiter.model.project.Label;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.project.TaxonomySettings;
import arbiter.service.AuthException;
import arbiter.service.CorpusService;
import arbiter.service.ProjectException;
import arbiter.service.ProjectSummary;
import arbiter.service.TaxonomySummary;
import arbiter.ui.shared.Components;
import arbiter.ui.shared.Dialogs;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * The view on a project's page where a SINGLE project's labels or a SCALE project's range are set up (#26), which
 * is read-only once the project's first assignment has frozen them (rule 3).
 */
final class TaxonomyView {
    private static final String LOAD_FAILED = "The taxonomy could not be loaded";
    private static final String SAVE_FAILED = "The taxonomy could not be saved";
    private static final String MOVE_FAILED = "The label could not be moved";
    private static final String DELETE_FAILED = "The label could not be deleted";

    private final Window owner;
    private final CorpusService corpus;
    private final ProjectSummary project;
    private final Runnable showPage;
    private final StackPane content = new StackPane();

    /** The label the form edits, or null while it adds one. */
    private Label editing;

    /**
     * Creates the view for a project as the project list showed it.
     *
     * @param showPage shows the project page again, reloaded
     */
    TaxonomyView(Window owner, CorpusService corpus, ProjectSummary project, Runnable showPage) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.corpus = Objects.requireNonNull(corpus, "corpus");
        this.project = Objects.requireNonNull(project, "project");
        this.showPage = Objects.requireNonNull(showPage, "showPage");
    }

    /** Returns the view's content, showing the project's current taxonomy. */
    Node content() {
        show();
        return content;
    }

    private void show() {
        Button back = new Button("Back");
        back.setOnAction(event -> showPage.run());
        String title = "Taxonomy of " + project.name();
        TaxonomySummary taxonomy;
        try {
            taxonomy = corpus.taxonomy(project.id());
        } catch (ProjectException | AuthException | JsonStoreException e) {
            Dialogs.showError(owner, LOAD_FAILED, e);
            VBox empty = Components.emptyState(title, "The taxonomy could not be loaded.");
            empty.getChildren().add(back);
            content.getChildren().setAll(empty);
            return;
        }
        List<Node> controls = new ArrayList<>(List.of(back, Components.pageTitle(title)));
        if (taxonomy.frozen()) {
            controls.add(Components.hint("The project's first assignment froze its taxonomy, so it cannot change."));
        }
        if (taxonomy.kind() == TaxonomyKind.SINGLE) {
            addLabels(controls, taxonomy.labels(), taxonomy.frozen());
        } else {
            addRange(controls, taxonomy);
        }
        content.getChildren().setAll(Components.page(controls.toArray(Node[]::new)));
    }

    /** Adds the labels table and, before the freeze, the form that adds or edits a label. */
    private void addLabels(List<Node> controls, List<Label> labels, boolean frozen) {
        TableView<Label> table = new TableView<>(FXCollections.observableArrayList(labels));
        table.getColumns().setAll(List.of(Components.wrappingColumn("Key", Label::getKey),
                Components.wrappingColumn("Description", Label::getDescription),
                Components.buttonColumn("Up", label -> frozen || label == labels.getFirst(),
                        label -> move(() -> corpus.moveLabelUp(label.getId()))),
                Components.buttonColumn("Down", label -> frozen || label == labels.getLast(),
                        label -> move(() -> corpus.moveLabelDown(label.getId()))),
                Components.buttonColumn("Edit", label -> frozen, this::edit),
                Components.buttonColumn("Delete", label -> frozen, this::delete)));
        // The rows stay in the labels' order, which Up and Down change.
        table.getColumns().forEach(column -> column.setSortable(false));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(Components.hint("No labels are set up yet."));
        VBox.setVgrow(table, Priority.ALWAYS);
        controls.add(table);
        if (frozen) {
            return;
        }
        controls.add(Components.hint("Labels can change only before the project's first assignment, "
                + "which needs at least two."));
        Label edited = editing;
        TextField key = new TextField(edited == null ? "" : edited.getKey());
        key.setPromptText("Key");
        TextArea description = new TextArea(edited == null ? "" : Objects.toString(edited.getDescription(), ""));
        description.setPromptText("Description (optional)");
        // A long description wraps here rather than scrolling out of sight.
        description.setWrapText(true);
        description.setPrefRowCount(3);
        Labeled error = Components.errorText();
        Button save = new Button("Save");
        save.setDefaultButton(true);
        save.setOnAction(event -> save(error, edited == null
                ? () -> corpus.addLabel(project.id(), key.getText(), description.getText())
                : () -> corpus.editLabel(edited.getId(), key.getText(), description.getText())));
        controls.addAll(List.of(Components.text(edited == null ? "New label" : "Edit label " + edited.getKey()),
                key, description, Components.formActions(save, () -> {
                    editing = null;
                    show();
                }), error));
    }

    /** Adds the range fields, which are read-only after the freeze. */
    private void addRange(List<Node> controls, TaxonomySummary taxonomy) {
        boolean saved = taxonomy.scaleMin() != null;
        controls.add(Components.text(saved ? "Saved range: " + taxonomy.scaleMin() + " to " + taxonomy.scaleMax()
                + "." : "No range is saved yet."));
        TextField minimum = new TextField(saved ? String.valueOf(taxonomy.scaleMin()) : "");
        minimum.setPromptText("Minimum");
        minimum.setDisable(taxonomy.frozen());
        TextField maximum = new TextField(saved ? String.valueOf(taxonomy.scaleMax()) : "");
        maximum.setPromptText("Maximum");
        maximum.setDisable(taxonomy.frozen());
        Labeled error = Components.errorText();
        Button save = new Button("Save");
        save.setDefaultButton(true);
        save.setDisable(taxonomy.frozen());
        save.setOnAction(event -> save(error, () -> corpus.saveRange(project.id(), minimum.getText(),
                maximum.getText())));
        controls.addAll(List.of(Components.text("Minimum"), minimum, Components.text("Maximum"), maximum,
                Components.hint("Annotators rate each file with a whole number in this range, ends included. "
                        + "Each end is from " + TaxonomySettings.LOWEST_SCALE_VALUE + " to "
                        + TaxonomySettings.HIGHEST_SCALE_VALUE + ", and the range can change only before the "
                        + "project's first assignment, which needs one."),
                save, error));
    }

    /**
     * Runs a save from the view's form through {@link ProjectPage#runInline}, then, if it succeeded, shows its
     * result.
     */
    private void save(Labeled error, Runnable change) {
        Optional<Boolean> saved = ProjectPage.runInline(owner, SAVE_FAILED, error, () -> {
            change.run();
            return true;
        });
        if (saved.isPresent()) {
            editing = null;
            show();
        }
    }

    private void move(Runnable change) {
        ProjectPage.commit(owner, MOVE_FAILED, change, this::show);
    }

    private void edit(Label label) {
        editing = label;
        show();
    }

    private void delete(Label label) {
        boolean confirmed = Dialogs.confirm(owner, "Delete label " + label.getKey() + "?", "This cannot be undone.",
                "Delete label");
        if (!confirmed) {
            return;
        }
        if (editing != null && editing.getId().equals(label.getId())) {
            editing = null;
        }
        ProjectPage.commit(owner, DELETE_FAILED, () -> corpus.deleteLabel(label.getId()), this::show);
    }
}
