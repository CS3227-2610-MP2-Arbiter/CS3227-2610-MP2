package arbiter.ui.shared;

import java.util.Objects;
import java.util.function.Consumer;

import arbiter.data.json.JsonStoreException;
import arbiter.service.AuthException;
import arbiter.service.AuthService;
import arbiter.service.CurrentUser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Owner setup and login surface for one workspace. */
public final class AuthScreen {
    private static final String USERNAME_HINT = "1-64 ASCII letters, digits, dots, underscores or hyphens";

    private final Stage stage;
    private final AuthService auth;
    private final Consumer<CurrentUser> onLogin;

    /** Uses the existing application stage and hands successful login to the shell. */
    public AuthScreen(Stage stage, AuthService auth, Consumer<CurrentUser> onLogin) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.onLogin = Objects.requireNonNull(onLogin, "onLogin");
    }

    /** Shows owner setup for a new workspace or login for an initialized one. */
    public void show() {
        if (auth.needsBootstrap()) {
            showOwnerSetup();
        } else {
            showLogin();
        }
    }

    private void showOwnerSetup() {
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        PasswordField confirmation = new PasswordField();
        confirmation.setPromptText("Confirm password");
        Label error = errorLabel();
        Button create = new Button("Create adjudicator account");
        create.setDefaultButton(true);
        create.setOnAction(event -> {
            if (!password.getText().equals(confirmation.getText())) {
                error.setText("Passwords do not match");
                return;
            }
            try {
                auth.bootstrapOwner(username.getText(), password.getText());
                showLogin();
            } catch (AuthException | JsonStoreException e) {
                error.setText(e.getMessage());
            }
        });
        showForm(new Label("Create the workspace adjudicator"),
                new Label("Keep this password safe; owner recovery is not available."),
                new Label(USERNAME_HINT), username,
                new Label("Password: at least 8 characters using only ASCII letters and digits"),
                password, confirmation, create, error);
    }

    private void showLogin() {
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Label error = errorLabel();
        Button login = new Button("Log in");
        login.setDefaultButton(true);
        login.setOnAction(event -> {
            try {
                CurrentUser user = auth.login(username.getText(), password.getText());
                onLogin.accept(user);
            } catch (AuthException e) {
                password.clear();
                error.setText(e.getMessage());
            }
        });
        showForm(new Label("Log in to Arbiter"), username, password, login, error);
    }

    private void showForm(Node... controls) {
        for (Node control : controls) {
            if (control instanceof Label label) {
                label.setWrapText(true);
                label.setMaxWidth(Double.MAX_VALUE);
            }
        }
        VBox form = new VBox(12, controls);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPadding(new Insets(24));
        form.setMaxWidth(420);
        stage.getScene().setRoot(new StackPane(form));
    }

    private static Label errorLabel() {
        Label error = new Label();
        error.setStyle("-fx-text-fill: #b00020;");
        return error;
    }
}
