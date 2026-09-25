package arbiter.blindness.fixture.ui.adjudicator;

import java.util.List;
import java.util.Optional;

import arbiter.data.json.JsonStore;
import arbiter.model.annotation.Annotation;
import arbiter.model.resolution.Resolution;

/** The adjudicator's screen, which may read everything and is never checked. */
public final class DisputeScreen {
    private final JsonStore store;

    /** Creates the screen. */
    public DisputeScreen(JsonStore store) {
        this.store = store;
    }

    /** Returns every answer for an item. */
    public List<Annotation> answers(long itemId) {
        return store.read(session -> session.annotations().listByItem(itemId));
    }

    /** Returns an item's resolution. */
    public Optional<Resolution> resolution(long itemId) {
        return store.read(session -> session.resolutions().findByItem(itemId));
    }
}
