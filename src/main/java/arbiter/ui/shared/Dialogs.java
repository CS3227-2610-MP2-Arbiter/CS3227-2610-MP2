package arbiter.ui.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/**
 * The only place a dialog is built, so every error, confirmation, success message and choice looks
 * and behaves the same for both roles.
 *
 * <p>Each dialog is owned by the calling window, titled "Arbiter" and styled by {@link Styles}.
 */
public final class Dialogs {
    private static final String TITLE = "Arbiter";

    private Dialogs() {
    }

    /**
     * Tells the user an action failed and records why in {@link DiagnosticLog}.
     *
     * @param heading what failed, such as "That workspace could not be opened"
     * @param error the failure; its message is shown only if it was written for the user
     */
    public static void showError(Window owner, String heading, Throwable error) {
        DiagnosticLog.record(heading, error);
        alert(Alert.AlertType.ERROR, owner, heading, ErrorMessages.of(error)).showAndWait();
    }

    /** Tells the user an action succeeded. */
    public static void showSuccess(Window owner, String heading, String message) {
        alert(Alert.AlertType.INFORMATION, owner, heading, message).showAndWait();
    }

    /**
     * Asks the user to confirm an action.
     *
     * @param confirmLabel the confirm button's text, naming the action, such as "Create workspace"
     * @return whether the user confirmed
     */
    public static boolean confirm(Window owner, String heading, String message, String confirmLabel) {
        Alert alert = alert(Alert.AlertType.CONFIRMATION, owner, heading, message);
        Button confirmButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        confirmButton.setText(confirmLabel);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    /**
     * Asks the user to pick one of several options, or cancel.
     *
     * @return the chosen option's text, or empty if the user cancelled
     */
    public static Optional<String> choose(Window owner, String heading, String message, String... options) {
        Alert alert = alert(Alert.AlertType.NONE, owner, heading, message);
        List<ButtonType> buttons = new ArrayList<>();
        for (String option : options) {
            buttons.add(new ButtonType(option));
        }
        buttons.add(ButtonType.CANCEL);
        alert.getButtonTypes().setAll(buttons);
        return alert.showAndWait().filter(choice -> choice != ButtonType.CANCEL).map(ButtonType::getText);
    }

    private static Alert alert(Alert.AlertType type, Window owner, String heading, String message) {
        Alert alert = new Alert(type);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(TITLE);
        alert.setHeaderText(heading);
        alert.setContentText(message);
        Styles.apply(alert.getDialogPane());
        return alert;
    }
}
