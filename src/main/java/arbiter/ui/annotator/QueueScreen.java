package arbiter.ui.annotator;

import java.util.Objects;

import arbiter.data.json.JsonStoreException;
import arbiter.service.AnnotationService;
import arbiter.service.AssignmentProgress;
import arbiter.service.AuthException;
import arbiter.service.ProjectException;
import arbiter.service.QueueItem;
import arbiter.service.QueueView;
import arbiter.ui.shared.Components;
import arbiter.ui.shared.Dialogs;
import arbiter.ui.shared.ItemView;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.stage.Window;

/**
 * One assignment's queue (#13): the first file without the annotator's answer, in saved split order, or
 * completion once every file is answered.
 *
 * <p>Nothing here moves between files; the queue moves forward only when an answer is submitted (#17).
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
        if (view.finished()) {
            return Components.page(title, Components.text("You have answered every file in this split."), back);
        }
        QueueItem current = view.current();
        return Components.page(title,
                Components.text("File " + (assignment.submitted() + 1) + " of " + assignment.total()),
                ItemView.of(current.storedPath(), current.text(), current.sourceError()), back);
    }
}
