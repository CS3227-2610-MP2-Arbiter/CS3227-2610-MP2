package arbiter.blindness.fixture.ui.annotator;

import arbiter.data.json.JsonStore;

/** Leaks a resolved result, read inside a lambda. */
public final class ResolvedLabelScreen {
    private final JsonStore store;

    /** Creates the screen. */
    public ResolvedLabelScreen(JsonStore store) {
        this.store = store;
    }

    /** Shows an item's resolved label. */
    public String show(long itemId) {
        return store.read(session -> session.resolutions().findByItem(itemId)
                .map(resolution -> String.valueOf(resolution.getLabelId())).orElse(""));
    }
}
