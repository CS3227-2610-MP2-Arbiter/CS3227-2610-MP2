package arbiter.ui.shared;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import arbiter.workspace.WorkspaceException;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

/** The start-up wizard, which creates a workspace or opens one the user chooses. */
public class WorkspaceSetupDialog {
    private static final ButtonType CREATE = new ButtonType("Create a workspace");
    private static final ButtonType OPEN = new ButtonType("Open a workspace");

    private final WorkspaceService service;

    /** Creates a wizard backed by a workspace service. */
    public WorkspaceSetupDialog(WorkspaceService service) {
        this.service = service;
    }

    /**
     * Asks the user to create or open a workspace, returning to that choice after a cancelled or failed
     * step.
     *
     * @param owner the window the dialogs belong to
     * @return the workspace, or empty if the user cancelled
     */
    public Optional<WorkspacePaths> start(Window owner) {
        while (true) {
            ButtonType action = chooseAction(owner);
            if (action != CREATE && action != OPEN) {
                return Optional.empty();
            }
            Path folder = chooseFolder(owner, action == CREATE
                    ? "Choose a folder for the new workspace"
                    : "Choose the workspace folder");
            if (folder == null) {
                continue;
            }
            Optional<WorkspacePaths> workspace = action == CREATE ? create(owner, folder) : open(owner, folder);
            if (workspace.isPresent()) {
                return workspace;
            }
        }
    }

    private ButtonType chooseAction(Window owner) {
        Alert alert = new Alert(Alert.AlertType.NONE, "Create a new workspace, or open an existing one.",
                CREATE, OPEN, ButtonType.CANCEL);
        alert.initOwner(owner);
        alert.setTitle("Arbiter");
        alert.setHeaderText("Choose a workspace");
        return alert.showAndWait().orElse(ButtonType.CANCEL);
    }

    private Path chooseFolder(Window owner, String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        File chosen = chooser.showDialog(owner);
        return chosen == null ? null : chosen.toPath();
    }

    private Optional<WorkspacePaths> create(Window owner, Path folder) {
        if (!confirm(owner, folder)) {
            return Optional.empty();
        }
        try {
            return Optional.of(service.create(folder));
        } catch (WorkspaceException e) {
            report(owner, "That workspace could not be created", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<WorkspacePaths> open(Window owner, Path folder) {
        try {
            return Optional.of(service.open(folder));
        } catch (WorkspaceException e) {
            report(owner, "That workspace could not be opened", e.getMessage());
            return Optional.empty();
        }
    }

    private boolean confirm(Window owner, Path folder) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle("Create a workspace");
        alert.setHeaderText("Create an Arbiter workspace in this folder?");
        VBox content = new VBox(8,
                new Label(folder.toString()),
                new Label("Arbiter will add a database, and folders for media, exports and logs. "
                        + "Existing files are left alone."));
        content.setPadding(new Insets(8));
        alert.getDialogPane().setContent(content);
        Button ok = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        ok.setText("Create workspace");
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void report(Window owner, String header, String detail) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle("Arbiter");
        alert.setHeaderText(header);
        alert.setContentText(detail);
        alert.showAndWait();
    }
}
