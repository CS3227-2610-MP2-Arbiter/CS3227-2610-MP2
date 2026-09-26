package arbiter.ui.adjudicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import arbiter.data.json.JsonStoreException;
import arbiter.service.AccountSummary;
import arbiter.service.AnnotatorLoad;
import arbiter.service.AssignmentOptions;
import arbiter.service.AssignmentService;
import arbiter.service.AuthException;
import arbiter.service.ProjectException;
import arbiter.service.SplitSummary;
import arbiter.ui.shared.Components;
import arbiter.ui.shared.Dialogs;
import arbiter.ui.shared.ErrorMessages;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Window;

/**
 * The form on a project's page that assigns annotators to one split (#32), showing the split's k, its assignees
 * and the annotators it can take with their load, then asking to confirm.
 */
final class AssignForm {
    private static final String ASSIGN_FAILED = "The annotators could not be assigned";

    private final Window owner;
    private final AssignmentService assignments;
    private final SplitSummary split;
    private final boolean frozen;
    private final AssignmentOptions options;
    private final Runnable showPage;

    /**
     * Creates the form for a split as the project page listed it.
     *
     * @param frozen whether the split's project was frozen (rule 3) when the page loaded
     * @param options what {@link AssignmentService#options} returned for the split
     * @param showPage shows the project page again, reloaded
     */
    AssignForm(Window owner, AssignmentService assignments, SplitSummary split, boolean frozen,
            AssignmentOptions options, Runnable showPage) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.assignments = Objects.requireNonNull(assignments, "assignments");
        this.split = Objects.requireNonNull(split, "split");
        this.frozen = frozen;
        this.options = Objects.requireNonNull(options, "options");
        this.showPage = Objects.requireNonNull(showPage, "showPage");
    }

    /** Returns the form's content. */
    Node content() {
        boolean saved = options.annotationsPerItem() != null;
        TextField annotationsPerItem = new TextField(String.valueOf(options.prefilledAnnotationsPerItem()));
        annotationsPerItem.setPromptText("Annotators per file");
        annotationsPerItem.setDisable(saved);
        List<Node> controls = new ArrayList<>(List.of(Components.pageTitle("Assign annotators to " + split.name()),
                new Label("Annotators per file (k)"), annotationsPerItem,
                Components.hint(annotationsPerItemHint(saved)), new Label("Assigned")));
        if (options.assignees().isEmpty()) {
            controls.add(Components.hint("No annotators are assigned yet."));
        }
        for (AccountSummary assignee : options.assignees()) {
            controls.add(Components.text(assignee.username() + " (" + assignee.status() + ")"));
        }
        controls.add(new Label("Annotators to assign"));
        List<CheckBox> choices = new ArrayList<>();
        for (AnnotatorLoad load : options.offered()) {
            CheckBox choice = new CheckBox(load.username() + ": " + ProjectPage.plural(load.unfinishedAssignments(),
                    "unfinished assignment") + ", " + ProjectPage.plural(load.unfinishedFiles(), "file"));
            choice.setWrapText(true);
            choice.setUserData(load);
            choices.add(choice);
        }
        if (choices.isEmpty()) {
            controls.add(Components.hint("No other active annotator can be assigned. "
                    + "Create annotators on the Accounts screen."));
        }
        controls.addAll(choices);
        Label error = Components.errorText();
        Button assign = new Button("Assign");
        assign.setDefaultButton(true);
        assign.setDisable(choices.isEmpty());
        assign.setOnAction(event -> assign(annotationsPerItem.getText(), chosen(choices), error));
        controls.add(Components.formActions(assign, showPage));
        controls.add(error);
        return Components.form(controls.toArray(Node[]::new));
    }

    /** Returns the hint under k: that it is fixed once saved, or else its range. */
    private String annotationsPerItemHint(boolean saved) {
        if (saved) {
            return "Set by the split's first assignment, and cannot be changed.";
        }
        String range = options.activeAnnotators() == 0 ? "From 1 to the number of active annotators."
                : "From 1 to " + options.activeAnnotators() + ", the number of active annotators.";
        return range + " It cannot be changed after the split's first assignment.";
    }

    private static List<AnnotatorLoad> chosen(List<CheckBox> choices) {
        return choices.stream()
                .filter(CheckBox::isSelected)
                .map(choice -> (AnnotatorLoad) choice.getUserData())
                .toList();
    }

    private void assign(String annotationsPerItem, List<AnnotatorLoad> chosen, Label error) {
        error.setText("");
        List<Long> annotatorIds = chosen.stream().map(AnnotatorLoad::id).toList();
        int confirmedPerItem;
        try {
            confirmedPerItem = assignments.check(split.id(), annotationsPerItem, annotatorIds);
        } catch (ProjectException e) {
            error.setText(ErrorMessages.of(e));
            return;
        } catch (AuthException | JsonStoreException e) {
            Dialogs.showError(owner, ASSIGN_FAILED, e);
            return;
        }
        String names = String.join(", ", chosen.stream().map(AnnotatorLoad::username).toList());
        String message = names + " will be assigned to " + split.name() + ", with "
                + ProjectPage.plural(confirmedPerItem, "annotator") + " per file. Assignments cannot be removed, "
                + "moved or replaced, and a disabled account keeps its place.";
        if (!frozen) {
            message += " This is the project's first assignment, so its files and taxonomy freeze and the project "
                    + "can no longer be deleted.";
        }
        boolean confirmed = Dialogs.confirm(owner, "Assign annotators to " + split.name() + "?", message, "Assign");
        if (!confirmed) {
            return;
        }
        ProjectPage.commit(owner, ASSIGN_FAILED, () -> assignments.assign(split.id(), annotationsPerItem,
                annotatorIds), showPage);
    }
}
