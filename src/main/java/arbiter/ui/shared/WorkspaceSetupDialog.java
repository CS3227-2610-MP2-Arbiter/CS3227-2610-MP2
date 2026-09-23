package arbiter.ui.shared;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import arbiter.data.json.JsonStore;
import arbiter.data.json.JsonStoreException;
import arbiter.workspace.WorkspaceException;
import arbiter.workspace.WorkspaceLock;
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
     * @return the held workspace lock, or empty if the user cancelled
     */
    public Optional<WorkspaceLock> start(Window owner) {
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
            Optional<WorkspaceLock> workspace = action == CREATE ? create(owner, folder) : open(owner, folder);
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

    private Optional<WorkspaceLock> create(Window owner, Path folder) {
        if (!confirm(owner, folder)) {
            return Optional.empty();
        }
        try {
            WorkspacePaths paths = service.create(folder);
            return lockAndCheck(paths, () -> JsonStore.initializeNew(paths));
        } catch (WorkspaceException | JsonStoreException e) {
            report(owner, "That workspace could not be created", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<WorkspaceLock> open(Window owner, Path folder) {
        try {
            WorkspacePaths paths = service.open(folder);
            return lockAndCheck(paths, () -> JsonStore.open(paths));
        } catch (WorkspaceException | JsonStoreException e) {
            report(owner, "That workspace could not be opened", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<WorkspaceLock> lockAndCheck(WorkspacePaths paths, Runnable checkData) {
        WorkspaceLock lock = WorkspaceLock.acquire(paths);
        try {
            checkData.run();
            return Optional.of(lock);
        } catch (RuntimeException e) {
            try {
                lock.close();
            } catch (WorkspaceException closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    private boolean confirm(Window owner, Path folder) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle("Create a workspace");
        alert.setHeaderText("Create an Arbiter workspace in this folder?");
        VBox content = new VBox(8,
                new Label(folder.toString()),
                new Label("Arbiter will add a data file and folders for media, exports and logs. "
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
