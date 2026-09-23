package arbiter.ui.shared;

import java.util.List;
import java.util.Objects;

import arbiter.service.CurrentUser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** One shared window layout for the signed-in role's registered screens. */
public final class AppShell {
    private final Stage stage;
    private final CurrentUser user;
    private final ScreenRegistry screens;
    private final Runnable onLogout;
    private final BorderPane root = new BorderPane();

    /** Creates the shell for a signed-in user without opening another window. */
    public AppShell(Stage stage, CurrentUser user, ScreenRegistry screens, Runnable onLogout) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.user = Objects.requireNonNull(user, "user");
        this.screens = Objects.requireNonNull(screens, "screens");
        this.onLogout = Objects.requireNonNull(onLogout, "onLogout");
    }

    /** Shows the first registered screen for this role. */
    public void show() {
        List<ScreenRoute> destinations = screens.forRole(user.role());
        if (destinations.isEmpty()) {
            throw new IllegalStateException("No screen is registered for " + user.role());
        }
        root.setTop(topBar());
        root.setLeft(navigation(destinations));
        navigate(destinations.getFirst().id());
        stage.getScene().setRoot(root);
    }

    /** Swaps content after confirming that the current role may open the route. */
    public void navigate(String id) {
        ScreenRoute route = screens.requireForRole(id, user.role());
        Node content = Objects.requireNonNull(route.content().get(), "screen content");
        root.setCenter(content);
    }

    private HBox topBar() {
        Label title = new Label("Arbiter");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label account = new Label(user.username());
        Button logout = new Button("Log out");
        logout.setOnAction(event -> onLogout.run());
        HBox bar = new HBox(12, title, spacer, account, logout);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 16, 12, 16));
        bar.setStyle("-fx-background-color: #f2f4f7;");
        return bar;
    }

    private VBox navigation(List<ScreenRoute> destinations) {
        VBox navigation = new VBox(8);
        navigation.setPadding(new Insets(16));
        navigation.setPrefWidth(180);
        for (ScreenRoute route : destinations) {
            Button destination = new Button(route.title());
            destination.setMaxWidth(Double.MAX_VALUE);
            destination.setOnAction(event -> navigate(route.id()));
            navigation.getChildren().add(destination);
        }
        return navigation;
    }
}
