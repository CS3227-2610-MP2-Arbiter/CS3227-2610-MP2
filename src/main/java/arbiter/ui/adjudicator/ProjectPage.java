package arbiter.ui.adjudicator;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import arbiter.data.json.JsonStoreException;
import arbiter.model.project.Item;
import arbiter.service.AuthException;
import arbiter.service.CorpusService;
import arbiter.service.ProjectException;
import arbiter.service.ProjectSummary;
import arbiter.service.SplitSummary;
import arbiter.ui.shared.Components;
import arbiter.ui.shared.Dialogs;
import arbiter.ui.shared.ErrorMessages;
import arbiter.workspace.SourceException;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * One project's page, where its files are registered and unregistered before its first assignment, and its
 * splits are generated and deleted.
 */
final class ProjectPage {
    private static final String REGISTER_FAILED = "The files could not be registered";
    private static final String UNREGISTER_FAILED = "The file could not be unregistered";
    private static final String GENERATE_FAILED = "The splits could not be generated";
    private static final String DELETE_SPLIT_FAILED = "The split could not be deleted";

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

    /** Returns the page's content, showing the project's current items and splits. */
    Node content() {
        show();
        return content;
    }

    private void show() {
        Button back = new Button("Back");
        back.setOnAction(event -> showList.run());
        List<Item> items;
        List<SplitSummary> splits;
        try {
            items = corpus.list(project.id());
            splits = corpus.listSplits(project.id());
        } catch (AuthException | JsonStoreException e) {
            Dialogs.showError(owner, "The project's files and splits could not be loaded", e);
            VBox empty = Components.emptyState(project.name(), "The project's files and splits could not be loaded.");
            empty.getChildren().add(back);
            content.getChildren().setAll(empty);
            return;
        }
        // A project's assignments cannot change on this page, so the list's count stays current.
        boolean frozen = project.assignmentCount() > 0;
        Button add = new Button("Add files...");
        add.setDisable(frozen);
        add.setOnAction(event -> addFiles());

        TextField itemsPerSplit = new TextField();
        itemsPerSplit.setPromptText("Files per split");
        Label error = Components.errorText();
        Button generate = new Button("Generate splits");
        generate.setOnAction(event -> generateSplits(itemsPerSplit.getText(), error));
        content.getChildren().setAll(Components.page(back, Components.pageTitle(project.name()), add,
                Components.hint("Files can be added or unregistered only before the project's first assignment."),
                itemTable(items, splits, frozen),
                Components.hint("Generate splits shuffles the files not yet in a split into new splits. "
                        + "A split can be deleted only before its first assignment."),
                itemsPerSplit, generate, error, splitTable(splits)));
    }

    private TableView<Item> itemTable(List<Item> items, List<SplitSummary> splits, boolean frozen) {
        Map<Long, String> places = places(splits);
        TableView<Item> table = new TableView<>(FXCollections.observableArrayList(items));
        table.getColumns().setAll(List.of(Components.column("Path", Item::getPath),
                Components.column("Split", item -> places.getOrDefault(item.getId(), "")),
                Components.buttonColumn("Unregister", item -> frozen, this::unregister)));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(Components.hint("No files are registered yet."));
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    /** Returns each split item's place by item identifier, such as "Split 1, position 3". */
    private static Map<Long, String> places(List<SplitSummary> splits) {
        Map<Long, String> places = new HashMap<>();
        for (SplitSummary split : splits) {
            for (int index = 0; index < split.itemIds().size(); index++) {
                places.put(split.itemIds().get(index), split.name() + ", position " + (index + 1));
            }
        }
        return places;
    }

    private TableView<SplitSummary> splitTable(List<SplitSummary> splits) {
        TableView<SplitSummary> table = new TableView<>(FXCollections.observableArrayList(splits));
        table.getColumns().setAll(List.of(Components.column("Name", SplitSummary::name),
                Components.column("Files", split -> split.itemIds().size()),
                Components.buttonColumn("Delete", SplitSummary::assigned, this::deleteSplit)));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(Components.hint("No splits are generated yet."));
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
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

    private void generateSplits(String itemsPerSplit, Label error) {
        error.setText("");
        List<Integer> sizes;
        try {
            sizes = corpus.previewSplits(project.id(), itemsPerSplit);
        } catch (ProjectException e) {
            error.setText(ErrorMessages.of(e));
            return;
        } catch (AuthException | JsonStoreException e) {
            Dialogs.showError(owner, GENERATE_FAILED, e);
            return;
        }
        String heading = sizes.size() == 1 ? "Generate 1 split?" : "Generate " + sizes.size() + " splits?";
        boolean confirmed = Dialogs.confirm(owner, heading, describeSizes(sizes), "Generate splits");
        if (!confirmed) {
            return;
        }
        try {
            corpus.generateSplits(project.id(), itemsPerSplit, sizes);
        } catch (ProjectException e) {
            // The page was out of date, so the reload below shows why.
            Dialogs.showError(owner, GENERATE_FAILED, e);
        } catch (AuthException | JsonStoreException e) {
            // Nothing changed, so the page is still current, and reloading would likely report this again.
            Dialogs.showError(owner, GENERATE_FAILED, e);
            return;
        }
        show();
    }

    /**
     * Summarises previewed split sizes, which are equal except perhaps a smaller last one (#28), such as
     * "2 splits of 50 files and 1 of 20."
     */
    private static String describeSizes(List<Integer> sizes) {
        int size = sizes.getFirst();
        int last = sizes.getLast();
        if (last == size) {
            return plural(sizes.size(), "split") + " of " + plural(size, "file") + ".";
        }
        return plural(sizes.size() - 1, "split") + " of " + plural(size, "file") + " and 1 of " + last + ".";
    }

    private static String plural(int count, String noun) {
        return count == 1 ? "1 " + noun : count + " " + noun + "s";
    }

    private void deleteSplit(SplitSummary split) {
        boolean confirmed = Dialogs.confirm(owner, "Delete " + split.name() + "?",
                "Its files return to those not yet in a split.", "Delete split");
        if (!confirmed) {
            return;
        }
        try {
            corpus.deleteSplit(split.id());
        } catch (ProjectException e) {
            // The page was out of date, so the reload below shows why.
            Dialogs.showError(owner, DELETE_SPLIT_FAILED, e);
        } catch (AuthException | JsonStoreException e) {
            // Nothing changed, so the page is still current, and reloading would likely report this again.
            Dialogs.showError(owner, DELETE_SPLIT_FAILED, e);
            return;
        }
        show();
    }
}
