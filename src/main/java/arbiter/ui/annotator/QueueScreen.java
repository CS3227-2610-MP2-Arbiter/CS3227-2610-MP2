package arbiter.ui.annotator;

import java.util.Objects;

import arbiter.data.json.JsonStoreException;
import arbiter.service.AnnotationService;
import arbiter.service.AssignmentProgress;
import arbiter.service.AuthException;
import arbiter.service.ProjectException;
import arbiter.service.QueueItem;
import arbiter.service.QueueView;
import arbiter.ui.shared.AnnotationEditor;
import arbiter.ui.shared.Components;
import arbiter.ui.shared.Dialogs;
import arbiter.ui.shared.ItemView;
import arbiter.workspace.SourceFailure;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * One assignment's queue (#13): the first file without the annotator's answer, in saved split order, or
 * completion once every file is answered.
 *
 * <p>Under the file, the annotator chooses an answer (#14), and Submit &amp; next is enabled only while it is
 * valid. Choosing it submits the answer and shows the next file (#17); nothing else moves between files.
 */
public final class QueueScreen {
    private static final String SUBMIT_FAILED = "Your answer could not be submitted";

    private final Window owner;
    private final AnnotationService annotations;
    private final long assignmentId;
    private final Runnable onBack;
    private final StackPane content = new StackPane();

    /**
     * Creates the screen, whose dialogs belong to {@code owner}.
     *
     * @param onBack what Back does, which is to return to the annotator's home
     */
    public QueueScreen(Window owner, AnnotationService annotations, long assignmentId, Runnable onBack) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.annotations = Objects.requireNonNull(annotations, "annotations");
        this.assignmentId = assignmentId;
        this.onBack = Objects.requireNonNull(onBack, "onBack");
    }

    /** Returns the screen's content, showing where the assignment stands now. */
    public Node content() {
        show();
        return content;
    }

    /** Shows where the assignment stands now, read afresh, so the screen always matches what is stored. */
    private void show() {
        Button back = new Button("Back to My splits");
        back.setOnAction(event -> onBack.run());
        QueueView view;
        try {
            view = annotations.forCurrentUser(assignmentId);
        } catch (ProjectException | AuthException | JsonStoreException e) {
            Dialogs.showError(owner, "This split could not be opened", e);
            content.getChildren().setAll(Components.page(Components.pageTitle("This split could not be opened"),
                    back));
            return;
        }
        AssignmentProgress assignment = view.assignment();
        Node title = Components.pageTitle(assignment.projectName() + ": " + assignment.splitName());
        if (assignment.finished()) {
            content.getChildren().setAll(Components.page(title,
                    Components.text("You have answered every file in this split."), back));
            return;
        }
        QueueItem current = view.current();
        Button submit = new Button("Submit & next");
        submit.setDefaultButton(true);
        Node file;
        Node answer;
        if (current.readable()) {
            AnnotationEditor editor = new AnnotationEditor(view.taxonomy());
            // Enabled only while the choice is valid (#14); nothing is stored until it is submitted (#17).
            submit.disableProperty().bind(editor.answerProperty().isNull());
            submit.setOnAction(event -> submit(submit, editor, current.itemId()));
            file = ItemView.of(current.text());
            answer = editor.view();
        } else {
            submit.setDisable(true);
            file = unreadable(current.failure());
            answer = new VBox();
        }
        content.getChildren().setAll(Components.page(title,
                Components.text("File " + (assignment.submitted() + 1) + " of " + assignment.total()), file, answer,
                submit, back));
    }

    private void submit(Button submit, AnnotationEditor editor, long itemId) {
        // Disabled until the outcome is known, so a double-click cannot submit twice.
        submit.disableProperty().unbind();
        submit.setDisable(true);
        try {
            annotations.submit(assignmentId, itemId, editor.answerProperty().get());
            show();
        } catch (ProjectException e) {
            // The queue may have moved elsewhere, such as from another screen, so show where it really is.
            Dialogs.showError(owner, SUBMIT_FAILED, e);
            show();
        } catch (AuthException | JsonStoreException e) {
            // Nothing was stored, so the choice stays on screen to submit again (rule 18).
            Dialogs.showError(owner, SUBMIT_FAILED, e);
            submit.disableProperty().bind(editor.answerProperty().isNull());
        }
    }

    /** Explains an unreadable file without naming it, since a file's name can hint at its label. */
    private static Node unreadable(SourceFailure failure) {
        Label error = Components.errorText();
        error.setText(switch (failure) {
        case MISSING, NOT_A_FILE -> "This file is missing from the workspace.";
        case HASH_MISMATCH -> "This file has changed since it was added to the project.";
        default -> "This file cannot be read.";
        });
        return new VBox(error, Components.hint("It cannot be answered until your adjudicator restores it."));
    }
}
