package arbiter.ui.adjudicator;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import arbiter.data.json.JsonStoreException;
import arbiter.service.AccountSummary;
import arbiter.service.AuthException;
import arbiter.service.AuthService;
import arbiter.ui.shared.Components;
import arbiter.ui.shared.Dialogs;
import arbiter.ui.shared.ErrorMessages;
import arbiter.ui.shared.PasswordFields;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * The adjudicator's account list, where annotators are created and deactivated (#31) and their passwords are
 * reset (#23).
 */
public final class AccountsScreen {
    private static final String DEACTIVATE_FAILED = "The account could not be deactivated";

    private final Window owner;
    private final AuthService auth;
    private final StackPane content = new StackPane();

    /** Creates the screen, whose dialogs belong to {@code owner}. */
    public AccountsScreen(Window owner, AuthService auth) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.auth = Objects.requireNonNull(auth, "auth");
    }

    /** Returns the screen's content, showing the current account list. */
    public Node content() {
        showList();
        return content;
    }

    private void showList() {
        List<AccountSummary> accounts;
        try {
            accounts = auth.listAccounts();
        } catch (AuthException | JsonStoreException e) {
            Dialogs.showError(owner, "The accounts could not be loaded", e);
            content.getChildren().setAll(Components.emptyState("Accounts", "The accounts could not be loaded."));
            return;
        }
        Button create = new Button("New annotator");
        create.setOnAction(event -> showCreateForm());
        Predicate<AccountSummary> unchangeable = Predicate.not(AccountSummary::isActiveAnnotator);
        TableView<AccountSummary> table = new TableView<>(FXCollections.observableArrayList(accounts));
        table.getColumns().setAll(List.of(
                Components.column("Username", AccountSummary::username),
                Components.column("Role", AccountSummary::role),
                Components.column("Status", AccountSummary::status),
                Components.buttonColumn("Reset password", unchangeable, this::showResetForm),
                Components.buttonColumn("Deactivate", unchangeable, this::deactivate)));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().setAll(Components.page(Components.pageTitle("Accounts"), create,
                Components.hint("Only an active annotator's password can be reset. Deactivation is permanent."),
                table));
    }

    private void showCreateForm() {
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordFields passwords = new PasswordFields("Password", "Confirm password");
        Label error = Components.errorText();

        Button create = new Button("Create annotator");
        create.setDefaultButton(true);
        create.setOnAction(event -> {
            error.setText("");
            if (!passwords.confirmed(error)) {
                return;
            }
            try {
                auth.createAnnotator(username.getText(), passwords.password().getText());
                showList();
            } catch (AuthException e) {
                error.setText(ErrorMessages.of(e));
            } catch (JsonStoreException e) {
                Dialogs.showError(owner, "The annotator could not be created", e);
            }
        });
        content.getChildren().setAll(Components.form(Components.pageTitle("New annotator"),
                Components.hint(AuthService.USERNAME_RULE), username, Components.hint(AuthService.PASSWORD_RULE),
                passwords.password(), passwords.confirmation(), Components.formActions(create, this::showList),
                error));
    }

    private void showResetForm(AccountSummary account) {
        PasswordFields passwords = new PasswordFields("New password", "Confirm new password");
        Label error = Components.errorText();

        Button reset = new Button("Reset password");
        reset.setDefaultButton(true);
        reset.setOnAction(event -> {
            error.setText("");
            if (!passwords.confirmed(error)) {
                return;
            }
            try {
                auth.resetAnnotatorPassword(account.id(), passwords.password().getText());
            } catch (AuthException e) {
                // A refusal about the account also shows here; Cancel reloads the list.
                error.setText(ErrorMessages.of(e));
                return;
            } catch (JsonStoreException e) {
                Dialogs.showError(owner, "The password could not be reset", e);
                return;
            }
            Dialogs.showSuccess(owner, "Password reset", "The password for " + account.username() + " was reset.");
            showList();
        });
        content.getChildren().setAll(Components.form(Components.pageTitle("Reset password for " + account.username()),
                Components.hint(AuthService.PASSWORD_RULE), passwords.password(), passwords.confirmation(),
                Components.formActions(reset, this::showList), error));
    }

    private void deactivate(AccountSummary account) {
        boolean confirmed = Dialogs.confirm(owner, "Deactivate " + account.username() + "?",
                "This is permanent: the account can no longer log in and cannot be reactivated. "
                        + "Its work and assignments are kept.", "Deactivate");
        if (!confirmed) {
            return;
        }
        try {
            auth.deactivateAnnotator(account.id());
        } catch (AuthException e) {
            // Usually the list was out of date, so the reload below shows why. The session itself is refused
            // only if the data file is edited outside Arbiter, and then the reload reports that again.
            Dialogs.showError(owner, DEACTIVATE_FAILED, e);
        } catch (JsonStoreException e) {
            // Nothing was saved, so the list is still current.
            Dialogs.showError(owner, DEACTIVATE_FAILED, e);
            return;
        }
        showList();
    }
}
