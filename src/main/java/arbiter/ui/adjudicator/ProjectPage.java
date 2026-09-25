package arbiter.ui.adjudicator;

import java.io.File;
import java.util.List;
import java.util.Objects;

import arbiter.data.json.JsonStoreException;
import arbiter.model.project.Item;
import arbiter.service.AuthException;
import arbiter.service.CorpusService;
import arbiter.service.ProjectException;
import arbiter.service.ProjectSummary;
import arbiter.ui.shared.Components;
import arbiter.ui.shared.Dialogs;
import arbiter.workspace.SourceException;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** One project's page, where its files are registered and unregistered before its first assignment. */
final class ProjectPage {
    private static final String REGISTER_FAILED = "The files could not be registered";
    private static final String UNREGISTER_FAILED = "The file could not be unregistered";

    private final Window owner;
    private final CorpusService corpus;
    private final ProjectSummary project;
    private final Runnable showList;
    private final StackPane content = new StackPane();

    /**
     * Creates the page for a project as the project list showed it.
     *
     * @param showList shows the project list again
     */
    ProjectPage(Window owner, CorpusService corpus, ProjectSummary project, Runnable showList) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.corpus = Objects.requireNonNull(corpus, "corpus");
        this.project = Objects.requireNonNull(project, "project");
        this.showList = Objects.requireNonNull(showList, "showList");
    }

    /** Returns the page's content, showing the project's current items. */
    Node content() {
        show();
        return content;
    }

    private void show() {
        Button back = new Button("Back");
        back.setOnAction(event -> showList.run());
        List<Item> items;
        try {
            items = corpus.list(project.id());
        } catch (AuthException | JsonStoreException e) {
            Dialogs.showError(owner, "The project's files could not be loaded", e);
            VBox empty = Components.emptyState(project.name(), "The project's files could not be loaded.");
            empty.getChildren().add(back);
            content.getChildren().setAll(empty);
            return;
        }
        // A project's assignments cannot change on this page, so the list's count stays current.
        boolean frozen = project.assignmentCount() > 0;
        Button add = new Button("Add files...");
        add.setDisable(frozen);
        add.setOnAction(event -> addFiles());

        TableView<Item> table = new TableView<>(FXCollections.observableArrayList(items));
        table.getColumns().setAll(List.of(Components.column("Path", Item::getPath),
                Components.buttonColumn("Unregister", item -> frozen, this::unregister)));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(Components.hint("No files are registered yet."));
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().setAll(Components.page(back, Components.pageTitle(project.name()), add,
                Components.hint("Files can be added or unregistered only before the project's first assignment."),
                table));
    }

    private void addFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose files to register");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Plain-text files", "*.txt"));
        File media = corpus.mediaDirectory().toFile();
        if (media.isDirectory()) {
            chooser.setInitialDirectory(media);
        }
        List<File> chosen = chooser.showOpenMultipleDialog(owner);
        if (chosen == null) {
            return;
        }
        try {
            int count = corpus.register(project.id(), chosen.stream().map(File::toPath).toList()).size();
            Dialogs.showSuccess(owner, "Files registered",
                    count == 1 ? "1 file was registered." : count + " files were registered.");
        } catch (ProjectException | SourceException e) {
            // Nothing was stored, and the reload below brings an out-of-date page up to date.
            Dialogs.showError(owner, REGISTER_FAILED, e);
        } catch (AuthException | JsonStoreException e) {
            // Nothing changed, so the page is still current, and reloading would likely report this again.
            Dialogs.showError(owner, REGISTER_FAILED, e);
            return;
        }
        show();
    }

    private void unregister(Item item) {
        boolean confirmed = Dialogs.confirm(owner, "Unregister " + item.getPath() + "?",
                "The project stops using this file. The file itself is not deleted.", "Unregister");
        if (!confirmed) {
            return;
        }
        try {
            corpus.unregister(item.getId());
        } catch (ProjectException e) {
            // The page was out of date, so the reload below shows why.
            Dialogs.showError(owner, UNREGISTER_FAILED, e);
        } catch (AuthException | JsonStoreException e) {
            // Nothing changed, so the page is still current, and reloading would likely report this again.
            Dialogs.showError(owner, UNREGISTER_FAILED, e);
            return;
        }
        show();
    }
}
