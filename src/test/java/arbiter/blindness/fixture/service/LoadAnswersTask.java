package arbiter.blindness.fixture.service;

import arbiter.data.json.JsonStore;
import javafx.concurrent.Task;

/** A background load that counts every annotator's answers; JavaFX, not the caller, runs it. */
public final class LoadAnswersTask extends Task<Integer> {
    private final JsonStore store;
    private final long itemId;

    /** Creates the task for one item. */
    public LoadAnswersTask(JsonStore store, long itemId) {
        this.store = store;
        this.itemId = itemId;
    }

    @Override
    protected Integer call() {
        return store.read(session -> session.annotations().listByItem(itemId)).size();
    }
}
