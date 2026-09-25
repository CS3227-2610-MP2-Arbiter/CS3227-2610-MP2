package arbiter.blindness.fixture.ui.annotator;

import arbiter.blindness.fixture.service.LoadAnswersTask;
import arbiter.data.json.JsonStore;

/** Leaks through a JavaFX task it constructs and hands to a thread, which calls it. */
public final class TaskScreen {
    /** Starts loading an item's answer count in the background. */
    public void load(JsonStore store, long itemId) {
        new Thread(new LoadAnswersTask(store, itemId)).start();
    }
}
