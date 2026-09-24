package arbiter.blindness.fixture.ui.shared;

import arbiter.model.resolution.Resolution;

/** Leaks from shared UI: a view model that accepts a resolved result, whichever role hands it one. */
public final class ResolvedViewModel {
    private String label = "";

    /** Shows a resolution's label. */
    public void show(Resolution resolution) {
        label = String.valueOf(resolution.getLabelId());
    }

    /** Returns the label shown. */
    public String label() {
        return label;
    }
}
