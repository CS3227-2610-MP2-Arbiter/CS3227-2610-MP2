package arbiter;

import javafx.application.Application;

/** Entry point for the JavaFX application. */
public final class Launcher {
    private Launcher() {

    }

    public static void main(String[] args) {
        Application.launch(Arbiter.class, args);
    }
}
