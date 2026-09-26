package arbiter.ui.shared;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

/** The masked password and confirmation fields of every form that sets a password. */
public final class PasswordFields {
    private final PasswordField password = new PasswordField();
    private final PasswordField confirmation = new PasswordField();

    /** Creates both fields, prompting with {@code prompt} and {@code confirmationPrompt}. */
    public PasswordFields(String prompt, String confirmationPrompt) {
        password.setPromptText(prompt);
        confirmation.setPromptText(confirmationPrompt);
    }

    /** Returns the field for the password. */
    public PasswordField password() {
        return password;
    }

    /** Returns the field that repeats the password. */
    public PasswordField confirmation() {
        return confirmation;
    }

    /** Returns whether the confirmation repeats the password, and if not, says so on {@code error}. */
    public boolean confirmed(Label error) {
        if (password.getText().equals(confirmation.getText())) {
            return true;
        }
        error.setText("Passwords do not match");
        return false;
    }
}
