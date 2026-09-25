package arbiter.ui.shared;

import java.net.URL;
import java.util.List;
import java.util.Objects;

import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * Owns Arbiter's one stylesheet and the names of its style classes.
 *
 * <p>Colours, fonts and spacing live in the stylesheet, never in code. A dialog has its own scene,
 * so {@link Dialogs} applies the stylesheet to each one as well as {@code Arbiter}'s scene.
 */
public final class Styles {
    /** The stylesheet's resource name, next to this class. */
    public static final String STYLESHEET = "arbiter.css";

    /** The shell's top bar. */
    public static final String TOP_BAR = "top-bar";

    /** The application name in the top bar. */
    public static final String APP_TITLE = "app-title";

    /** The shell's navigation column. */
    public static final String NAVIGATION = "navigation";

    /** A screen or form heading. */
    public static final String PAGE_TITLE = "page-title";

    /** Secondary text that explains a control or a rule. */
    public static final String HINT = "hint";

    /** An inline message about input the user can correct on the same form. */
    public static final String ERROR_TEXT = "error-text";

    /** A centred form. */
    public static final String FORM = "form";

    /** A screen with nothing to show yet. */
    public static final String EMPTY_STATE = "empty-state";

    /** Every style class above, each of which has a rule in the stylesheet. */
    public static final List<String> CLASSES = List.of(TOP_BAR, APP_TITLE, NAVIGATION, PAGE_TITLE, HINT, ERROR_TEXT,
            FORM, EMPTY_STATE);

    private Styles() {
    }

    /** Returns the stylesheet's location. */
    public static URL stylesheet() {
        return Objects.requireNonNull(Styles.class.getResource(STYLESHEET), "The stylesheet is missing");
    }

    /** Applies the stylesheet to a scene, once. */
    public static void apply(Scene scene) {
        addOnce(scene.getStylesheets());
    }

    /** Applies the stylesheet to a node with a scene of its own, such as a dialog pane, once. */
    public static void apply(Parent root) {
        addOnce(root.getStylesheets());
    }

    private static void addOnce(List<String> stylesheets) {
        String location = stylesheet().toExternalForm();
        if (!stylesheets.contains(location)) {
            stylesheets.add(location);
        }
    }
}
