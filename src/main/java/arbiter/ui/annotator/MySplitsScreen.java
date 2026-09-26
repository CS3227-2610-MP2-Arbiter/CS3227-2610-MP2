package arbiter.ui.annotator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import arbiter.data.json.JsonStoreException;
import arbiter.service.AnnotationService;
import arbiter.service.AssignmentProgress;
import arbiter.service.AuthException;
import arbiter.ui.shared.Components;
import arbiter.ui.shared.Dialogs;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** The annotator's home, listing their own assignments with the progress needed to continue (#12). */
public final class MySplitsScreen {
    private static final String TITLE = "My splits";

    private final Window owner;
    private final AnnotationService annotations;
    private final Consumer<AssignmentProgress> onOpen;

    /**
     * Creates the screen, whose dialogs belong to {@code owner}.
     *
     * @param onOpen what opening an unfinished assignment does, such as showing its queue (#13)
     */
    public MySplitsScreen(Window owner, AnnotationService annotations, Consumer<AssignmentProgress> onOpen) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.annotations = Objects.requireNonNull(annotations, "annotations");
        this.onOpen = Objects.requireNonNull(onOpen, "onOpen");
    }

    /** Returns the screen's content, showing the annotator's current assignments. */
    public Node content() {
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

    private VBox card(AssignmentProgress assignment) {
        CardState state = CardState.of(assignment);
        VBox card = Components.card(Components.text(assignment.projectName() + ": " + assignment.splitName()),
                Components.hint(state.statusText()),
                Components.text(assignment.submitted() + " of " + assignment.total() + " files submitted"));
        if (state.openLabel() != null) {
            Button open = new Button(state.openLabel());
            open.setOnAction(event -> onOpen.accept(assignment));
            card.getChildren().add(open);
        }
        return card;
    }

    /**
     * What a card shows, decided in one place from the assignment's stored status, which submission (#17) keeps.
     *
     * @param statusText the status as the card words it
     * @param openLabel the open button's text, or null for a finished assignment, which is never reopened
     */
    private record CardState(String statusText, String openLabel) {
        static CardState of(AssignmentProgress assignment) {
            return switch (assignment.status()) {
            case NOT_STARTED -> new CardState("Not started", "Start");
            case IN_PROGRESS -> new CardState("In progress", "Continue");
            case SUBMITTED -> new CardState("Finished: every file has your answer", null);
            };
        }
    }
}
