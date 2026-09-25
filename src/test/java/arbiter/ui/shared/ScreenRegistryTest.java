package arbiter.ui.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import arbiter.model.user.Role;

/** Role filtering and lookup checks for registered screens. */
class ScreenRegistryTest {
    @Test
    void register_distinctRoutes_preservesRoleOrderAndAllowsNewRoute() {
        ScreenRegistry registry = new ScreenRegistry();
        ScreenRoute queue = route("queue", Role.ANNOTATOR);
        ScreenRoute projects = route("projects", Role.ADJUDICATOR);
        ScreenRoute history = route("history", Role.ANNOTATOR);

        registry.register(queue);
        registry.register(projects);
        assertEquals(List.of(queue), registry.forRole(Role.ANNOTATOR));
        assertEquals(List.of(projects), registry.forRole(Role.ADJUDICATOR));

        registry.register(history);
        assertEquals(List.of(queue, history), registry.forRole(Role.ANNOTATOR));
        assertSame(history, registry.requireForRole("history", Role.ANNOTATOR));
    }

    @Test
    void register_duplicateIdAcrossRoles_rejectsSecondRoute() {
        ScreenRegistry registry = new ScreenRegistry();
        ScreenRoute first = route("home", Role.ANNOTATOR);
        registry.register(first);

        assertThrows(IllegalArgumentException.class, () -> registry.register(route("home", Role.ADJUDICATOR)));
        assertSame(first, registry.requireForRole("home", Role.ANNOTATOR));
        assertEquals(List.of(), registry.forRole(Role.ADJUDICATOR));
    }

    @Test
    void requireForRole_allowedRole_returnsRouteWithoutCreatingContent() {
        ScreenRegistry registry = new ScreenRegistry();
        AtomicInteger creations = new AtomicInteger();
        ScreenRoute route = new ScreenRoute("queue", "Queue", Role.ANNOTATOR, () -> {
            creations.incrementAndGet();
            return null;
        });
        registry.register(route);

        assertSame(route, registry.requireForRole("queue", Role.ANNOTATOR));
        assertEquals(0, creations.get());
    }

    @Test
    void requireForRole_otherRole_throwsWithoutCreatingContent() {
        for (Role allowed : Role.values()) {
            ScreenRegistry registry = new ScreenRegistry();
            AtomicInteger creations = new AtomicInteger();
            registry.register(new ScreenRoute("home", "Home", allowed, () -> {
                creations.incrementAndGet();
                return null;
            }));
            Role denied = allowed == Role.ANNOTATOR ? Role.ADJUDICATOR : Role.ANNOTATOR;

            assertThrows(IllegalArgumentException.class, () -> registry.requireForRole("home", denied));
            assertEquals(0, creations.get());
        }
    }

    @Test
    void requireForRole_unknownId_throws() {
        ScreenRegistry registry = new ScreenRegistry();
        registry.register(route("queue", Role.ANNOTATOR));

        assertThrows(IllegalArgumentException.class, () -> registry.requireForRole("missing", Role.ANNOTATOR));
    }

    private static ScreenRoute route(String id, Role role) {
        return new ScreenRoute(id, id, role, () -> null);
    }
}
