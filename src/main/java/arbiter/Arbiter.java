package arbiter;

import java.util.Optional;

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

    @Override
    public void start(Stage stage) {
        stage.setTitle("Arbiter");
        stage.setScene(new Scene(new StackPane(new Label("Arbiter")), 800, 600));
        stage.show();

        WorkspaceSetupDialog wizard = new WorkspaceSetupDialog(new WorkspaceService());
        Optional<WorkspaceLock> selected = wizard.start(stage);
        if (selected.isEmpty()) {
            stage.close();
            return;
        }
        workspace = selected.get();
        try {
            stage.setTitle("Arbiter - " + workspace.paths().root().getFileName());
            stage.setOnHidden(event -> closeWorkspace());
        } catch (RuntimeException e) {
            closeWorkspace();
            throw e;
        }
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
