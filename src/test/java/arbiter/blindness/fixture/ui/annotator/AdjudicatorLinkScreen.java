package arbiter.blindness.fixture.ui.annotator;

import arbiter.blindness.fixture.ui.adjudicator.DisputeScreen;

/** Leaks by opening one of the adjudicator's screens. */
public final class AdjudicatorLinkScreen {
    /** Opens the dispute screen. */
    public Object open() {
        return new DisputeScreen(null);
    }
}
