package arbiter.ui.shared;

import java.util.Objects;
import java.util.function.Consumer;

import arbiter.data.json.JsonStoreException;
import arbiter.service.AuthException;
import arbiter.service.AuthService;
import arbiter.service.CurrentUser;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/** Owner setup and login surface for one workspace. */
public final class AuthScreen {
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
        PasswordFields passwords = new PasswordFields("Password", "Confirm password");
        Label error = Components.errorText();
        Button create = new Button("Create adjudicator account");
        create.setDefaultButton(true);
        create.setOnAction(event -> {
            if (!passwords.confirmed(error)) {
                return;
            }
            try {
                auth.bootstrapOwner(username.getText(), passwords.password().getText());
                showLogin();
            } catch (AuthException e) {
                error.setText(ErrorMessages.of(e));
            } catch (JsonStoreException e) {
                Dialogs.showError(stage, "The adjudicator account could not be saved", e);
            }
        });
        showForm(Components.pageTitle("Create the workspace adjudicator"),
                Components.hint("Keep this password safe; owner recovery is not available."),
                Components.hint(AuthService.USERNAME_RULE), username, Components.hint(AuthService.PASSWORD_RULE),
                passwords.password(), passwords.confirmation(), create, error);
    }

    private void showLogin() {
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Label error = Components.errorText();
        Button login = new Button("Log in");
        login.setDefaultButton(true);
        login.setOnAction(event -> {
            try {
                CurrentUser user = auth.login(username.getText(), password.getText());
                onLogin.accept(user);
            } catch (AuthException e) {
                password.clear();
                error.setText(ErrorMessages.of(e));
            } catch (JsonStoreException e) {
                Dialogs.showError(stage, "Arbiter could not read the accounts", e);
            }
        });
        showForm(Components.pageTitle("Log in to Arbiter"), username, password, login, error);
    }

    private void showForm(Node... controls) {
        stage.getScene().setRoot(Components.form(controls));
    }
}
