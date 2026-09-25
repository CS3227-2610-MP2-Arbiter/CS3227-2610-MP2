package arbiter.blindness.fixture.ui.annotator;

import arbiter.blindness.fixture.service.Progress;

/** Leaks cross-annotator progress, read two calls deep inside a service. */
public final class TeamProgressScreen {
    private final Progress progress;

    /** Creates the screen. */
    public TeamProgressScreen(Progress progress) {
        this.progress = progress;
    }

    /** Shows how many annotators are on a split. */
    public int show(long splitId) {
        return progress.team(splitId).size();
    }
}
