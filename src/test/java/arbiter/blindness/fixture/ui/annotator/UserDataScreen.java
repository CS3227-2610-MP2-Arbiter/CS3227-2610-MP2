package arbiter.blindness.fixture.ui.annotator;

import arbiter.model.resolution.Resolution;
import javafx.scene.Node;

/** Leaks a resolution handed over untyped, as JavaFX user data, by reading it. */
public final class UserDataScreen {
    /** Shows the label of the resolution stored on a node. */
    public String show(Node node) {
        return String.valueOf(((Resolution) node.getUserData()).getLabelId());
    }
}
