package arbiter;

import java.util.Optional;

import arbiter.data.json.JsonStore;
import arbiter.data.json.JsonStoreException;
import arbiter.model.user.Role;
import arbiter.service.AuthException;
import arbiter.service.AuthService;
import arbiter.service.CurrentUser;
import arbiter.ui.shared.AppShell;
import arbiter.ui.shared.AuthScreen;
import arbiter.ui.shared.ScreenRegistry;
import arbiter.ui.shared.ScreenRoute;
import arbiter.ui.shared.WorkspaceSetupDialog;
import arbiter.workspace.WorkspaceLock;
import arbiter.workspace.WorkspaceService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Provides Arbiter's JavaFX interface. */
public class Arbiter extends Application {
    private WorkspaceLock workspace;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Arbiter");
        stage.setScene(new Scene(new StackPane(new Label("Arbiter")), 960, 640));
        stage.setMinWidth(760);
        stage.setMinHeight(520);
        stage.show();

        WorkspaceSetupDialog wizard = new WorkspaceSetupDialog(new WorkspaceService());
        Optional<WorkspaceLock> selected = wizard.start(stage);
        if (selected.isEmpty()) {
            stage.close();
            return;
        }

        workspace = selected.get();
        try {
            stage.setOnHidden(event -> closeWorkspace());
            stage.setTitle("Arbiter - " + workspace.paths().root().getFileName());
            showAuth(stage, new AuthService(JsonStore.open(workspace)));
        } catch (AuthException | JsonStoreException e) {
            try {
                Alert error = new Alert(Alert.AlertType.ERROR, e.getMessage());
                error.initOwner(stage);
                error.setHeaderText("That workspace cannot be used for login");
                error.showAndWait();
            } finally {
                closeWorkspace();
                stage.close();
            }
        } catch (RuntimeException e) {
            closeWorkspace();
            throw e;
        }
    }

    private void showAuth(Stage stage, AuthService auth) {
        new AuthScreen(stage, auth, user -> showShell(stage, auth, user)).show();
    }

    private void showShell(Stage stage, AuthService auth, CurrentUser user) {
        ScreenRegistry screens = new ScreenRegistry();
        screens.register(new ScreenRoute("annotator-home", "My splits", Role.ANNOTATOR, () ->
                placeholder("My splits", "Your assigned splits will appear here.")));
        screens.register(new ScreenRoute("adjudicator-home", "Projects", Role.ADJUDICATOR, () ->
                placeholder("Projects", "Your projects will appear here.")));
        new AppShell(stage, user, screens, () -> {
            auth.logout();
            showAuth(stage, auth);
        }).show();
    }

    private static VBox placeholder(String title, String message) {
        VBox content = new VBox(12, new Label(title), new Label(message));
        content.setPadding(new Insets(24));
        return content;
    }

    @Override
    public void stop() {
        closeWorkspace();
    }

    private void closeWorkspace() {
        if (workspace != null) {
            WorkspaceLock held = workspace;
            workspace = null;
            held.close();
        }
    }
}
