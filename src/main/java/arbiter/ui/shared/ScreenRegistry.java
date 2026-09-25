package arbiter.ui.shared;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import arbiter.model.user.Role;

/** Holds shell destinations in registration order and guards access by role. */
public final class ScreenRegistry {
    private final Map<String, ScreenRoute> routes = new LinkedHashMap<>();

    /** Registers a screen without changing the shell's routing logic. */
    public void register(ScreenRoute route) {
        Objects.requireNonNull(route, "route");
        if (routes.putIfAbsent(route.id(), route) != null) {
            throw new IllegalArgumentException("Screen is already registered: " + route.id());
        }
    }

    /** Returns only the screens available to the given role, in registration order. */
    public List<ScreenRoute> forRole(Role role) {
        Objects.requireNonNull(role, "role");
        List<ScreenRoute> visible = new ArrayList<>();
        for (ScreenRoute route : routes.values()) {
            if (route.role() == role) {
                visible.add(route);
            }
        }
        return List.copyOf(visible);
    }

    /** Resolves a route only when the signed-in role owns it. */
    public ScreenRoute requireForRole(String id, Role role) {
        Objects.requireNonNull(role, "role");
        ScreenRoute route = routes.get(id);
        if (route == null || route.role() != role) {
            throw new IllegalArgumentException("Screen is not available");
        }
        return route;
    }
}
