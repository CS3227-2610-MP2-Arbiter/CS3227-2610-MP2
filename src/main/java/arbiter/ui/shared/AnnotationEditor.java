package arbiter.ui.shared;

import java.util.Objects;
import java.util.OptionalInt;

import arbiter.model.project.TaxonomyKind;
import arbiter.service.Answer;
import arbiter.service.LabelOption;
import arbiter.service.Taxonomy;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

/**
 * The controls for choosing one answer from a project's taxonomy (#14): one label for SINGLE, or one whole
 * number within the range for SCALE.
 *
 * <p>It holds only the current, unsubmitted choice, which is screen state and is lost if the screen closes
 * (rule 18). It takes the taxonomy as plain values, so it never loads anything, and whether a choice is valid is
 * decided by {@link Taxonomy}, the same rules submission (#17) applies.
 */
public final class AnnotationEditor {
    private final Taxonomy taxonomy;
    private final ReadOnlyObjectWrapper<Answer> answer = new ReadOnlyObjectWrapper<>();
    private final VBox view = new VBox();

    /** Creates an editor for answers following this taxonomy, with nothing chosen. */
    public AnnotationEditor(Taxonomy taxonomy) {
        this.taxonomy = Objects.requireNonNull(taxonomy, "taxonomy");
        if (!taxonomy.answerable()) {
            view.getChildren().add(Components.hint(taxonomy.kind() == TaxonomyKind.SINGLE
                    ? "This project's labels are not set up yet, so its files cannot be answered."
                    : "This project's rating range is not set up yet, so its files cannot be answered."));
        } else if (taxonomy.kind() == TaxonomyKind.SINGLE) {
            buildLabelChoice();
        } else {
            buildRatingField();
        }
    }

    /** Returns the editor's controls. */
    public VBox view() {
        return view;
    }

    /** Returns the current answer, which is null while nothing valid is chosen. */
    public ReadOnlyObjectProperty<Answer> answerProperty() {
        return answer.getReadOnlyProperty();
    }

    private void buildLabelChoice() {
        ToggleGroup group = new ToggleGroup();
        for (LabelOption label : taxonomy.labels()) {
            RadioButton choice = new RadioButton(label.key());
            choice.setToggleGroup(group);
            choice.setUserData(label.id());
            choice.setWrapText(true);
            choice.setMaxWidth(Double.MAX_VALUE);
            view.getChildren().add(choice);
            if (label.description() != null && !label.description().isBlank()) {
                view.getChildren().add(Components.hint(label.description()));
            }
        }
        group.selectedToggleProperty().addListener((observable, previous, selected) ->
                answer.set(selected == null ? null : Answer.label((Long) selected.getUserData())));
    }

    private void buildRatingField() {
        TextField rating = new TextField();
        rating.setPromptText("Rating");
        Label error = Components.errorText();
        rating.textProperty().addListener((observable, previous, text) -> {
            OptionalInt parsed = taxonomy.parseRating(text);
            answer.set(parsed.isPresent() ? Answer.rating(parsed.getAsInt()) : null);
            error.setText(parsed.isPresent() || text.isBlank() ? "" : "Enter a whole number from "
                    + taxonomy.scaleMin() + " to " + taxonomy.scaleMax() + ".");
        });
        view.getChildren().addAll(Components.hint("A whole number from " + taxonomy.scaleMin() + " to "
                + taxonomy.scaleMax()), rating, error);
    }
}
