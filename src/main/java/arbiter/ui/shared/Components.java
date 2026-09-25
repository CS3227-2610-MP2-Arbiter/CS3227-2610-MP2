package arbiter.ui.shared;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Builds the basic display pieces both roles' screens share; their look comes from {@link Styles}. */
public final class Components {
    private Components() {
    }

    /** Returns a screen or form heading. */
    public static Label pageTitle(String text) {
        return styled(wrapping(text), Styles.PAGE_TITLE);
    }

    /** Returns secondary text that explains a control or a rule. */
    public static Label hint(String text) {
        return styled(wrapping(text), Styles.HINT);
    }

    /**
     * Returns an empty inline message for input the user can correct on the same form. Set its text
     * with {@link ErrorMessages#of}; failed actions use {@link Dialogs#showError} instead.
     */
    public static Label errorText() {
        return styled(wrapping(""), Styles.ERROR_TEXT);
    }

    /** Returns the content of a screen that has nothing to show yet. */
    public static VBox emptyState(String title, String message) {
        return styled(new VBox(pageTitle(title), hint(message)), Styles.EMPTY_STATE);
    }

    /** Returns a screen's content of these controls, laid out from the top. */
    public static VBox page(Node... controls) {
        return styled(new VBox(controls), Styles.PAGE);
    }

    /** Returns a form of these controls, centred in the available space. */
    public static StackPane form(Node... controls) {
        VBox form = styled(new VBox(controls), Styles.FORM);
        form.setAlignment(Pos.CENTER_LEFT);
        return new StackPane(form);
    }

    private static Label wrapping(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static <T extends Node> T styled(T node, String styleClass) {
        node.getStyleClass().add(styleClass);
        return node;
    }
}
