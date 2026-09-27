package arbiter.ui.adjudicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.service.AnnotationService;
import arbiter.service.AssignmentService;
import arbiter.service.AuthService;
import arbiter.service.CorpusService;
import arbiter.service.ExportService;
import arbiter.service.ProjectService;
import arbiter.service.ProjectSummary;
import arbiter.service.ResolutionService;
import arbiter.testing.ClassificationWorkflow;
import arbiter.testing.TestWorkspace;
import arbiter.ui.annotator.QueueScreen;
import arbiter.ui.shared.Styles;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Checks short-window scrolling for the project, progress and annotator pages (#33). */
class ProjectPageLayoutTest {
    private static boolean toolkitStarted;

    @BeforeAll
    static void startToolkit() {
        // Linux CI has no display server. Windows and macOS run the real JavaFX layout check.
        boolean headlessLinux = System.getProperty("os.name").startsWith("Linux")
                && (System.getenv("DISPLAY") == null || System.getenv("DISPLAY").isBlank());
        assumeTrue(!headlessLinux);
        Platform.startup(() -> { });
        toolkitStarted = true;
    }

    @AfterAll
    static void stopToolkit() {
        if (toolkitStarted) {
            Platform.exit();
        }
    }

    @Test
    void page_shortWindow_bothTablesKeepRowsReachableByScrolling(@TempDir Path temporary) throws Exception {
        TestWorkspace workspace = TestWorkspace.create(temporary.resolve("workspace"));
        ClassificationWorkflow flow = ClassificationWorkflow.single("yes", "no").items(2)
                .assign("alice").seed(workspace);
        AuthService auth = workspace.signInOwner();
        ProjectService projects = new ProjectService(workspace.store(), auth);
        ProjectSummary summary = projects.list().stream().filter(row -> row.id() == flow.projectId())
                .findFirst().orElseThrow();

        FutureTask<Layout> task = new FutureTask<>(() -> {
            Stage hiddenOwner = new Stage();
            try {
                StackPane page = (StackPane) new ProjectPage(hiddenOwner, projects,
                        new CorpusService(workspace.store(), auth, workspace.paths()),
                        new AssignmentService(workspace.store(), auth),
                        new ResolutionService(workspace.store(), auth, workspace.paths()),
                        new ExportService(workspace.store(), auth, workspace.paths()),
                        summary, () -> { }).content();
                StackPane root = new StackPane(page);
                Scene scene = new Scene(root, 520, 320);
                Styles.apply(scene);
                root.resize(520, 320);
                root.applyCss();
                root.layout();
                List<TableView<?>> tables = new ArrayList<>();
                for (Node node : root.lookupAll(".table-view")) {
                    tables.add((TableView<?>) node);
                }
                TableView<?> items = table(tables, "Path");
                TableView<?> splits = table(tables, "Name");
                Node pageBody = page.getChildren().getFirst();
                ScrollPane scroll = pageBody instanceof ScrollPane found ? found : null;
                double itemHeight = items.getHeight();
                double splitHeight = splits.getHeight();
                double viewportHeight = scroll == null ? 0 : scroll.getViewportBounds().getHeight();
                double contentHeight = scroll == null ? 0 : scroll.getContent().getLayoutBounds().getHeight();
                if (scroll != null) {
                    scroll.setVvalue(scroll.getVmax());
                    root.layout();
                }
                double splitTopAfterScroll = splits.localToScene(splits.getBoundsInLocal()).getMinY();
                double splitBottomAfterScroll = splits.localToScene(splits.getBoundsInLocal()).getMaxY();
                root.resize(800, 900);
                root.layout();
                return new Layout(itemHeight, splitHeight, viewportHeight, contentHeight,
                        splitTopAfterScroll, splitBottomAfterScroll, items.getHeight(), splits.getHeight());
            } finally {
                hiddenOwner.close();
            }
        });
        Platform.runLater(task);
        Layout layout = task.get(15, TimeUnit.SECONDS);

        assertTrue(layout.itemHeight() >= 80 && layout.splitHeight() >= 80,
                "Tables collapsed at 520x320: " + layout);
        assertTrue(layout.contentHeight() > layout.viewportHeight() && layout.viewportHeight() > 0,
                "The short page has no usable vertical scrolling: " + layout);
        assertTrue(layout.splitTopAfterScroll() < 320 && layout.splitBottomAfterScroll() > 0,
                "Scrolling to the bottom did not bring the splits table into view: " + layout);
        assertTrue(layout.largeItemHeight() >= 80 && layout.largeSplitHeight() >= 80,
                "Tables are still collapsed in a large window: " + layout);
        assertTrue(layout.largeItemHeight() > layout.itemHeight()
                || layout.largeSplitHeight() > layout.splitHeight(),
                "Larger windows do not give either table more room: " + layout);
    }

    @Test
    void progress_shortWindow_allTablesKeepRowsReachableByScrolling(@TempDir Path temporary) throws Exception {
        TestWorkspace workspace = TestWorkspace.create(temporary.resolve("workspace"));
        ClassificationWorkflow flow = ClassificationWorkflow.single("yes", "no").items(2)
                .assign("alice").seed(workspace);
        ProjectService projects = new ProjectService(workspace.store(), workspace.signInOwner());

        FutureTask<ProgressLayout> task = new FutureTask<>(() -> {
            Stage hiddenOwner = new Stage();
            try {
                Runnable noNavigation = () -> { };
                LongConsumer noAssignment = ignored -> { };
                StackPane page = (StackPane) new ProgressView(hiddenOwner, projects, flow.projectId(),
                        noNavigation, noAssignment, noNavigation).content();
                StackPane root = new StackPane(page);
                Scene scene = new Scene(root, 520, 320);
                Styles.apply(scene);
                root.resize(520, 320);
                root.applyCss();
                root.layout();
                List<TableView<?>> tables = new ArrayList<>();
                for (Node node : root.lookupAll(".table-view")) {
                    tables.add((TableView<?>) node);
                }
                TableView<?> annotators = tables.stream().filter(table -> table.getColumns().size() == 3)
                        .findFirst().orElseThrow();
                ScrollPane scroll = (ScrollPane) page.getChildren().getFirst();
                List<Double> heights = tables.stream().map(TableView::getHeight).toList();
                double viewportHeight = scroll.getViewportBounds().getHeight();
                double contentHeight = scroll.getContent().getLayoutBounds().getHeight();
                scroll.setVvalue(scroll.getVmax());
                root.layout();
                double annotatorTopAfterScroll = annotators.localToScene(annotators.getBoundsInLocal()).getMinY();
                double annotatorBottomAfterScroll = annotators.localToScene(annotators.getBoundsInLocal()).getMaxY();
                return new ProgressLayout(heights, viewportHeight, contentHeight,
                        annotatorTopAfterScroll, annotatorBottomAfterScroll);
            } finally {
                hiddenOwner.close();
            }
        });
        Platform.runLater(task);
        ProgressLayout layout = task.get(15, TimeUnit.SECONDS);

        assertEquals(3, layout.tableHeights().size());
        assertTrue(layout.tableHeights().stream().allMatch(height -> height >= 80),
                "Progress tables collapsed at 520x320: " + layout);
        assertTrue(layout.contentHeight() > layout.viewportHeight() && layout.viewportHeight() > 0,
                "Progress has no usable vertical scrolling: " + layout);
        assertTrue(layout.annotatorTopAfterScroll() < 320 && layout.annotatorBottomAfterScroll() > 0,
                "Scrolling to the bottom did not bring annotator progress into view: " + layout);
    }

    @Test
    void queue_narrowWindow_wrappedLabelChoicesKeepTheirHeight(@TempDir Path temporary) throws Exception {
        TestWorkspace workspace = TestWorkspace.create(temporary.resolve("workspace"));
        String[] labels = new String[6];
        for (int index = 0; index < labels.length; index++) {
            labels[index] = "The classification label has several words and wraps in a narrow window, choice "
                    + (index + 1);
        }
        ClassificationWorkflow flow = ClassificationWorkflow.single(labels).assign("alice").seed(workspace);
        AnnotationService annotations = new AnnotationService(workspace.store(), workspace.signIn("alice"),
                workspace.paths());

        FutureTask<ChoiceLayout> task = new FutureTask<>(() -> {
            Stage hiddenOwner = new Stage();
            try {
                StackPane page = (StackPane) new QueueScreen(hiddenOwner, annotations,
                        flow.assignmentId("alice"), () -> { }).content();
                StackPane root = new StackPane(page);
                Scene scene = new Scene(root, 280, 240);
                Styles.apply(scene);
                root.resize(280, 240);
                root.applyCss();
                root.layout();
                RadioButton choice = (RadioButton) root.lookup(".radio-button");
                ScrollPane scroll = (ScrollPane) page.getChildren().getFirst();
                return new ChoiceLayout(choice.getHeight(), choice.prefHeight(choice.getWidth()),
                        scroll.getViewportBounds().getHeight(),
                        scroll.getContent().getLayoutBounds().getHeight());
            } finally {
                hiddenOwner.close();
            }
        });
        Platform.runLater(task);
        ChoiceLayout layout = task.get(15, TimeUnit.SECONDS);

        assertTrue(layout.contentHeight() > layout.viewportHeight(), "Long choices should overflow: " + layout);
        assertTrue(layout.actualChoiceHeight() + 1 >= layout.preferredChoiceHeight(),
                "A wrapped label was squeezed below its preferred height: " + layout);
    }

    private static TableView<?> table(List<TableView<?>> tables, String firstColumn) {
        return tables.stream().filter(table -> firstColumn.equals(table.getColumns().getFirst().getText()))
                .findFirst().orElseThrow();
    }

    private record Layout(double itemHeight, double splitHeight, double viewportHeight, double contentHeight,
            double splitTopAfterScroll, double splitBottomAfterScroll,
            double largeItemHeight, double largeSplitHeight) {
    }

    private record ProgressLayout(List<Double> tableHeights, double viewportHeight, double contentHeight,
            double annotatorTopAfterScroll, double annotatorBottomAfterScroll) {
    }

    private record ChoiceLayout(double actualChoiceHeight, double preferredChoiceHeight,
            double viewportHeight, double contentHeight) {
    }
}
