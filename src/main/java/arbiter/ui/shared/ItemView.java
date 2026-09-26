package arbiter.ui.shared;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Shows one file's text, or why its source cannot be read (rule 21), for either role's screens.
 *
 * <p>It takes plain values, so it never loads anything itself, and the text wraps and scrolls rather than
 * being cut off.
 */
public final class ItemView {
    private ItemView() {
    }

    /**
     * Returns a view of one file that grows to fill its parent's height.
     *
     * @param storedPath the file's workspace-relative path, shown as a hint
     * @param text the file's text, shown when {@code sourceError} is null
     * @param sourceError the source resolver's message, or null if the text was read
     */
    public static VBox of(String storedPath, String text, String sourceError) {
        VBox view = new VBox(Components.hint(storedPath));
        if (sourceError != null) {
            Label error = Components.errorText();
            error.setText(sourceError);
            view.getChildren().addAll(error,
                    Components.hint("This file cannot be answered until its original contents are restored."));
        } else {
            ScrollPane scroll = Components.scrollingPage(Components.text(text));
            VBox.setVgrow(scroll, Priority.ALWAYS);
            view.getChildren().add(scroll);
        }
        VBox.setVgrow(view, Priority.ALWAYS);
        return view;
    }
}
