package arbiter;

import java.io.IOException;
import java.util.Optional;

import arbiter.data.json.JsonStore;
import arbiter.data.json.JsonStoreException;
import arbiter.model.user.Role;
import arbiter.service.AuthException;
import arbiter.service.AuthService;
import arbiter.service.CurrentUser;
import arbiter.service.ProjectService;
import arbiter.ui.adjudicator.ProjectsScreen;
import arbiter.ui.shared.AppShell;
import arbiter.ui.shared.AuthScreen;
import arbiter.ui.shared.Components;
import arbiter.ui.shared.DiagnosticLog;
import arbiter.ui.shared.Dialogs;
import arbiter.ui.shared.ScreenRegistry;
import arbiter.ui.shared.ScreenRoute;
import arbiter.ui.shared.Styles;
import arbiter.ui.shared.WorkspaceSetupDialog;
import arbiter.workspace.WorkspaceLock;
import arbiter.workspace.WorkspaceService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Provides Arbiter's JavaFX interface. */
public class Arbiter extends Application {
    private WorkspaceLock workspace;
    private AuthService auth;
    private ProjectService projects;

    @Override
    public void start(Stage stage) {
        Thread.UncaughtExceptionHandler report = (thread, error) -> Dialogs.showUncaught(stage, error);
        Thread.setDefaultUncaughtExceptionHandler(report);
        Thread.currentThread().setUncaughtExceptionHandler(report);
        try {
            open(stage);
        } catch (AuthException | JsonStoreException e) {
            failToStart(stage, "That workspace cannot be used for login", e);
        } catch (RuntimeException e) {
            failToStart(stage, "Arbiter could not start", e);
        }
    }

    private void open(Stage stage) {
        stage.setTitle("Arbiter");
        Scene scene = new Scene(new StackPane(new Label("Arbiter")), 960, 640);
        Styles.apply(scene);
        stage.setScene(scene);
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
            DiagnosticLog.attach(workspace.paths().logsDirectory());
        } catch (IOException e) {
            // The console still gets every record, so Arbiter carries on without the workspace log.
            DiagnosticLog.record("Open the workspace log", e);
        }
        stage.setOnHidden(event -> closeWorkspace());
        stage.setTitle("Arbiter - " + workspace.paths().root().getFileName());
        JsonStore store = JsonStore.open(workspace);
        auth = new AuthService(store);
        projects = new ProjectService(store, auth);
        showAuth(stage);
    }

    private void failToStart(Stage stage, String heading, RuntimeException error) {
        // Shown, and so logged, before the workspace is released, so the failure reaches its log.
        try {
            Dialogs.showError(stage, heading, error);
        } finally {
            closeWorkspace();
            stage.close();
        }
    }

    private void showAuth(Stage stage) {
        new AuthScreen(stage, auth, user -> showShell(stage, user)).show();
    }

    private void showShell(Stage stage, CurrentUser user) {
        ScreenRegistry screens = new ScreenRegistry();
        screens.register(new ScreenRoute("annotator-home", "My splits", Role.ANNOTATOR, () ->
                Components.emptyState("My splits", "Your assigned splits will appear here.")));
        screens.register(new ScreenRoute("adjudicator-home", "Projects", Role.ADJUDICATOR, () ->
                new ProjectsScreen(stage, projects).content()));
        new AppShell(stage, user, screens, () -> {
            auth.logout();
            showAuth(stage);
        }).show();
    }

    @Override
    public void stop() {
        closeWorkspace();
    }

    private void closeWorkspace() {
        if (workspace != null) {
            WorkspaceLock held = workspace;
            workspace = null;
            DiagnosticLog.detach();
            held.close();
        }
    }
}
