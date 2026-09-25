package arbiter.blindness.fixture.ui.queue;

import arbiter.data.json.JsonStore;

/** Leaks from a UI package the rule was never told about: it reads an answer outside the trusted path. */
public final class QueueScreen {
    private final JsonStore store;

    /** Creates the screen. */
    public QueueScreen(JsonStore store) {
        this.store = store;
    }

    /** Shows whether another annotator has answered an item. */
    public boolean show(long itemId, long otherAnnotatorId) {
        return store.read(session -> session.annotations().findByItemAndAnnotator(itemId, otherAnnotatorId))
                .isPresent();
    }
}
