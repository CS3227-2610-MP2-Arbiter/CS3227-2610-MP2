package arbiter.ui.shared;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Shows one file's text for either role's screens. It takes the text as a plain value, so it never loads
 * anything itself, and the text wraps and scrolls rather than being cut off. What to say when a file cannot be
 * read is each screen's own wording.
 */
public final class ItemView {
    private ItemView() {
    }

    /** Returns a view of one file's text that grows to fill its parent's height. */
    public static VBox of(String text) {
        ScrollPane scroll = Components.scrollingPage(Components.text(text));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox view = new VBox(scroll);
        VBox.setVgrow(view, Priority.ALWAYS);
        return view;
    }
}
