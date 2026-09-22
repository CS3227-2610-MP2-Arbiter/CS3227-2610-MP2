package arbiter;

import java.util.Optional;

import arbiter.ui.shared.WorkspaceSetupDialog;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Provides Arbiter's JavaFX interface. */
public class Arbiter extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Arbiter");
        stage.setScene(new Scene(new StackPane(new Label("Arbiter")), 800, 600));
        stage.show();

        WorkspaceSetupDialog wizard = new WorkspaceSetupDialog(new WorkspaceService());
        Optional<WorkspacePaths> workspace = wizard.start(stage);
        if (workspace.isEmpty()) {
            stage.close();
            return;
        }
        stage.setTitle("Arbiter - " + workspace.get().root().getFileName());
    }
}
