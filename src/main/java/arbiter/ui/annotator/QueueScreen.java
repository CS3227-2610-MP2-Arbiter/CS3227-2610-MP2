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
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * One assignment's queue (#13): the first file without the annotator's answer, in saved split order, or
 * completion once every file is answered.
 *
 * <p>Under the file, the annotator chooses an answer (#14), and Submit &amp; next is enabled only while it is
 * valid. Nothing here moves between files; the queue moves forward only when an answer is submitted (#17).
 */
public final class QueueScreen {
    private final Window owner;
    private final AnnotationService annotations;
    private final long assignmentId;
    private final Runnable onBack;

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
        Button back = new Button("Back to My splits");
        back.setOnAction(event -> onBack.run());
        QueueView view;
        try {
            view = annotations.forCurrentUser(assignmentId);
        } catch (ProjectException | AuthException | JsonStoreException e) {
            Dialogs.showError(owner, "This split could not be opened", e);
            return Components.page(Components.pageTitle("This split could not be opened"), back);
        }
        AssignmentProgress assignment = view.assignment();
        Node title = Components.pageTitle(assignment.projectName() + ": " + assignment.splitName());
        if (assignment.finished()) {
            return Components.page(title, Components.text("You have answered every file in this split."), back);
        }
        QueueItem current = view.current();
        Button submit = new Button("Submit & next");
        submit.setDefaultButton(true);
        submit.setOnAction(event -> Dialogs.showSuccess(owner, "Submit & next",
                "Submitting answers is not available yet, so your choice was not saved."));
        Node file;
        Node answer;
        if (current.readable()) {
            AnnotationEditor editor = new AnnotationEditor(view.taxonomy());
            // Enabled only while the choice is valid (#14); nothing is stored until submission (#17).
            submit.disableProperty().bind(editor.answerProperty().isNull());
            file = ItemView.of(current.text());
            answer = editor.view();
        } else {
            submit.setDisable(true);
            file = unreadable(current.failure());
            answer = new VBox();
        }
        // The page scrolls as a whole, so neither a long file nor many labels can push Submit & next out of reach.
        return Components.scrollingPage(title,
                Components.text("File " + (assignment.submitted() + 1) + " of " + assignment.total()), file, answer,
                submit, back);
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
