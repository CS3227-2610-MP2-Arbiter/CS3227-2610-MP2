package arbiter.ui.shared;

import java.util.Objects;
import java.util.function.Supplier;

import arbiter.model.user.Role;
import javafx.scene.Node;

/** A registered destination in the shared application shell. */
public record ScreenRoute(String id, String title, Role role, Supplier<Node> content) {
    /** Requires a named destination with an owner role and content factory. */
    public ScreenRoute {
        if (id == null || id.isBlank() || title == null || title.isBlank()) {
            throw new IllegalArgumentException("Screen ID and title are required");
        }
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(content, "content");
    }
}
