package arbiter.ui.annotator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import arbiter.data.json.JsonStoreException;
import arbiter.service.AnnotationService;
import arbiter.service.AssignmentProgress;
import arbiter.service.AuthException;
import arbiter.ui.shared.Components;
import arbiter.ui.shared.Dialogs;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * The annotator's home, listing their own assignments with the progress needed to continue (#12), from which an
 * unfinished one opens in its queue (#13).
 */
public final class MySplitsScreen {
    private static final String TITLE = "My splits";

    private final Window owner;
    private final AnnotationService annotations;
    private final StackPane content = new StackPane();

    /** Creates the screen, whose dialogs belong to {@code owner}. */
    public MySplitsScreen(Window owner, AnnotationService annotations) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.annotations = Objects.requireNonNull(annotations, "annotations");
    }

    /** Returns the screen's content, showing the annotator's current assignments. */
    public Node content() {
        showList();
        return content;
    }

    private void showList() {
        content.getChildren().setAll(list());
    }

    private Node list() {
        List<AssignmentProgress> assignments;
        try {
            assignments = annotations.forCurrentUser();
        } catch (AuthException | JsonStoreException e) {
            Dialogs.showError(owner, "Your splits could not be loaded", e);
            return Components.emptyState(TITLE, "Your splits could not be loaded.");
        }
        if (assignments.isEmpty()) {
            return Components.emptyState(TITLE, "You have no assigned splits yet. Your adjudicator assigns them.");
        }
        List<Node> controls = new ArrayList<>();
        controls.add(Components.pageTitle(TITLE));
        for (AssignmentProgress assignment : assignments) {
            controls.add(card(assignment));
        }
        return Components.scrollingPage(controls.toArray(Node[]::new));
    }

    private void open(AssignmentProgress assignment) {
        content.getChildren().setAll(new QueueScreen(owner, annotations, assignment.assignmentId(), this::showList)
                .content());
    }

    private VBox card(AssignmentProgress assignment) {
        VBox card = Components.card(Components.text(assignment.projectName() + ": " + assignment.splitName()),
                Components.hint(statusText(assignment)),
                Components.text(assignment.submitted() + " of " + assignment.total() + " files submitted"));
        if (!assignment.finished()) {
            Button open = new Button(assignment.submitted() == 0 ? "Start" : "Continue");
            open.setOnAction(event -> open(assignment));
            card.getChildren().add(open);
        }
        return card;
    }

    private static String statusText(AssignmentProgress assignment) {
        return switch (assignment.status()) {
        case NOT_STARTED -> "Not started";
        case IN_PROGRESS -> "In progress";
        case SUBMITTED -> "Finished: every file has your answer";
        };
    }
}
