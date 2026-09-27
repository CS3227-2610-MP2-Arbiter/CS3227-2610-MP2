package arbiter.ui.adjudicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import arbiter.data.json.JsonStoreException;
import arbiter.service.Answer;
import arbiter.service.AuthException;
import arbiter.service.DisputeComparison;
import arbiter.service.DisputeSummary;
import arbiter.service.ProjectException;
import arbiter.service.ProjectSummary;
import arbiter.service.ResolutionService;
import arbiter.ui.shared.AnnotationEditor;
import arbiter.ui.shared.Components;
import arbiter.ui.shared.Dialogs;
import arbiter.ui.shared.ItemView;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * The view on a SINGLE project's page that lists its disputes and the items the adjudicator has decided, and
 * compares one item's anonymous answers so a decision can be saved or replaced (#34).
 */
final class DisputesView {
    private static final String LOAD_FAILED = "The disputes could not be loaded";
    private static final String OPEN_FAILED = "The file could not be opened";
    private static final String SAVE_FAILED = "The decision could not be saved";

    private final Window owner;
    private final ResolutionService resolutions;
    private final ProjectSummary project;
    private final Runnable showPage;
    private final StackPane content = new StackPane();

    /**
     * Creates the view for a project as the project list showed it.
     *
     * @param showPage shows the project page again, reloaded
     */
    DisputesView(Window owner, ResolutionService resolutions, ProjectSummary project, Runnable showPage) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.resolutions = Objects.requireNonNull(resolutions, "resolutions");
        this.project = Objects.requireNonNull(project, "project");
        this.showPage = Objects.requireNonNull(showPage, "showPage");
    }

    /** Returns the view's content, showing the project's current dispute list. */
    Node content() {
        show();
        return content;
    }

    private void show() {
        Button back = new Button("Back");
        back.setOnAction(event -> showPage.run());
        String title = "Disputes in " + project.name();
        List<DisputeSummary> disputes;
        try {
            disputes = resolutions.disputes(project.id());
        } catch (AuthException | JsonStoreException e) {
            Dialogs.showError(owner, LOAD_FAILED, e);
            VBox empty = Components.emptyState(title, "The disputes could not be loaded.");
            empty.getChildren().add(back);
            content.getChildren().setAll(empty);
            return;
        }
        TableView<DisputeSummary> table = new TableView<>(FXCollections.observableArrayList(disputes));
        table.getColumns().setAll(List.of(Components.wrappingColumn("Path", DisputeSummary::path),
                Components.column("Split", DisputeSummary::splitName),
                Components.column("Position", DisputeSummary::position),
                Components.wrappingColumn("Decision", dispute -> dispute.decided()
                        ? "Decided: " + dispute.decisionKey() : "Unresolved"),
                Components.buttonColumn("Open", dispute -> false, dispute -> showComparison(dispute.itemId()))));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(Components.hint("This project has no disputes."));
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().setAll(Components.page(back, Components.pageTitle(title),
                Components.hint("A decided file stays listed, and its decision can be replaced."), table));
    }

    /** Shows one item's text and anonymous answers, where a decision on it is saved. */
    private void showComparison(long itemId) {
        DisputeComparison comparison;
        try {
            comparison = resolutions.comparison(itemId);
        } catch (ProjectException e) {
            // The file is no longer listed, so the list is reloaded as it now stands.
            Dialogs.showError(owner, OPEN_FAILED, e);
            show();
            return;
        } catch (AuthException | JsonStoreException e) {
            // Nothing changed, so the list is still current, and reloading would likely report this again.
            Dialogs.showError(owner, OPEN_FAILED, e);
            return;
        }
        Button back = new Button("Back");
        back.setOnAction(event -> show());
        List<Node> controls = new ArrayList<>(List.of(back, Components.pageTitle(comparison.path())));
        controls.add(comparison.readable() ? ItemView.of(comparison.text()) : unreadable(comparison.failure()));
        controls.add(Components.text("Submitted labels"));
        for (String key : comparison.answerKeys()) {
            controls.add(Components.card(Components.text(key)));
        }
        if (comparison.decisionKey() != null) {
            controls.add(Components.text("Current decision: " + comparison.decisionKey()));
        }
        Button save = new Button("Save");
        save.setDefaultButton(true);
        if (comparison.readable()) {
            AnnotationEditor editor = new AnnotationEditor(comparison.taxonomy());
            // Enabled once a label is picked; nothing is stored until it is saved.
            save.disableProperty().bind(editor.answerProperty().isNull());
            save.setOnAction(event -> save(itemId, editor));
            controls.add(editor.view());
        } else {
            save.setDisable(true);
        }
        controls.add(save);
        // The page scrolls as a whole, so neither a long file nor many labels can push Save out of reach.
        content.getChildren().setAll(Components.scrollingPage(controls.toArray(Node[]::new)));
    }

    private void save(long itemId, AnnotationEditor editor) {
        // A SINGLE project's editor gives only a label.
        long labelId = ((Answer.LabelChoice) editor.answerProperty().get()).labelId();
        ProjectPage.commit(owner, SAVE_FAILED, () -> resolutions.adjudicate(itemId, labelId), this::show);
    }

    /** Explains why a file cannot be read, with the resolver's message, which names it. */
    private static Node unreadable(String failure) {
        Label error = Components.errorText();
        error.setText(failure);
        return new VBox(error, Components.hint("No decision can be saved until the file is restored."));
    }
}
