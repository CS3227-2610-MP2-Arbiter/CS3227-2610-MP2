package arbiter;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Provides Arbiter's JavaFX interface. */
public class Arbiter extends Application {
    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane(new Label("Arbiter"));
        stage.setTitle("Arbiter");
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }
}
