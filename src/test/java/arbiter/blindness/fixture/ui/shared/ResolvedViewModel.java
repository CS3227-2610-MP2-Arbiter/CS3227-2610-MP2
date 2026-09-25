package arbiter.blindness.fixture.ui.shared;

import arbiter.model.resolution.Resolution;

/** Leaks from shared UI: a view model that accepts a resolved result, whichever role hands it one. */
public final class ResolvedViewModel {
    private boolean shown;

    /** Shows a resolution. */
    public void show(Resolution resolution) {
        shown = resolution != null;
    }

    /** Returns whether a resolution is shown. */
    public boolean isShown() {
        return shown;
    }
}
