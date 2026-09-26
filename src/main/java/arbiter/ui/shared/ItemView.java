package arbiter.ui.shared;

import javafx.scene.layout.VBox;

/**
 * Shows one file's text for either role's screens. It takes the text as a plain value, so it never loads
 * anything itself. The text wraps and is shown in full, so a screen that holds it scrolls as a whole rather than
 * cutting it off. What to say when a file cannot be read is each screen's own wording.
 */
public final class ItemView {
    private ItemView() {
    }

    /** Returns a view of one file's text, shown in full. */
    public static VBox of(String text) {
        return new VBox(Components.text(text));
    }
}
