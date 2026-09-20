package arbiter.ui.shared;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import arbiter.workspace.WorkspaceException;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

/**
 * The first-run wizard, which chooses and opens the folder a workspace lives in.
 *
 * <p>This is a thin adapter over {@link WorkspaceService}: every rule about workspaces is decided
 * there, so the acceptance criteria are covered by tests that need no JavaFX toolkit.
 *
 * <p>It lives in the shared layer because first-run behaviour belongs to the shell, not to either
 * role.
 */
public class WorkspaceSetupDialog {
    private final WorkspaceService service;

    /** Creates a wizard backed by a workspace service. */
    public WorkspaceSetupDialog(WorkspaceService service) {
        this.service = service;
    }

    /**
     * Opens the remembered workspace, or asks for one when there is none.
     *
     * <p>This is the whole first-run step: on later launches the remembered workspace is reopened
     * without asking anything.
     *
     * @param owner the window the dialogs belong to
     * @return the chosen workspace, or empty if the user cancelled
     */
    public Optional<WorkspacePaths> start(Window owner) {
        Optional<WorkspacePaths> remembered = service.openRemembered();
        if (remembered.isPresent()) {
            return remembered;
        }
        List<Path> recent = service.getRecent().getPaths();
        if (recent.isEmpty()) {
            Path folder = chooseNewFolder(owner);
            return folder == null ? Optional.empty() : create(owner, folder);
        }
        Path folder = chooseRemembered(owner, recent);
        if (folder == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(open(owner, folder));
    }

    private Path chooseRemembered(Window owner, List<Path> remembered) {
        ChoiceDialog<Path> dialog = new ChoiceDialog<>(remembered.get(0), remembered);
        dialog.initOwner(owner);
        dialog.setTitle("Open a workspace");
        dialog.setHeaderText("Choose the workspace to open");
        dialog.setContentText("Workspace");
        return dialog.showAndWait().orElse(null);
    }

    private Path chooseNewFolder(Window owner) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose a folder for the workspace");
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

    private WorkspacePaths open(Window owner, Path folder) {
        try {
            return service.open(folder);
        } catch (WorkspaceException e) {
            report(owner, "That workspace could not be opened", e.getMessage());
            return null;
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
