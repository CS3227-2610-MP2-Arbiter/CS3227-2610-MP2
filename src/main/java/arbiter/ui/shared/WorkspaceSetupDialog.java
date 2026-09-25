package arbiter.ui.shared;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

import arbiter.data.json.JsonStore;
import arbiter.data.json.JsonStoreException;
import arbiter.workspace.WorkspaceException;
import arbiter.workspace.WorkspaceLock;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

/** The start-up wizard, which creates a workspace or opens one the user chooses. */
public class WorkspaceSetupDialog {
    private static final String CREATE = "Create a workspace";
    private static final String OPEN = "Open a workspace";

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
            Optional<String> action = Dialogs.choose(owner, "Choose a workspace",
                    "Create a new workspace, or open an existing one.", CREATE, OPEN);
            if (action.isEmpty()) {
                return Optional.empty();
            }
            boolean creating = action.get().equals(CREATE);
            Path folder = chooseFolder(owner, creating
                    ? "Choose a folder for the new workspace"
                    : "Choose the workspace folder");
            if (folder == null) {
                continue;
            }
            Optional<WorkspaceLock> workspace = creating ? create(owner, folder) : open(owner, folder);
            if (workspace.isPresent()) {
                return workspace;
            }
        }
    }

    private Path chooseFolder(Window owner, String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        File chosen = chooser.showDialog(owner);
        return chosen == null ? null : chosen.toPath();
    }

    private Optional<WorkspaceLock> create(Window owner, Path folder) {
        String message = folder + "\n\nArbiter will add a data file and folders for media, exports and logs. "
                + "Existing files are left alone.";
        boolean confirmed = Dialogs.confirm(owner, "Create an Arbiter workspace in this folder?", message,
                "Create workspace");
        if (!confirmed) {
            return Optional.empty();
        }
        try {
            WorkspacePaths paths = service.create(folder);
            return lockAndCheck(paths, JsonStore::initializeNew);
        } catch (WorkspaceException | JsonStoreException e) {
            Dialogs.showError(owner, "That workspace could not be created", e);
            return Optional.empty();
        }
    }

    private Optional<WorkspaceLock> open(Window owner, Path folder) {
        try {
            WorkspacePaths paths = service.open(folder);
            return lockAndCheck(paths, JsonStore::open);
        } catch (WorkspaceException | JsonStoreException e) {
            Dialogs.showError(owner, "That workspace could not be opened", e);
            return Optional.empty();
        }
    }

    private Optional<WorkspaceLock> lockAndCheck(WorkspacePaths paths, Consumer<WorkspaceLock> checkData) {
        WorkspaceLock lock = WorkspaceLock.acquire(paths);
        try {
            checkData.accept(lock);
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
}
