package arbiter.blindness.fixture.ui.annotator;

import arbiter.blindness.fixture.service.AnswerSource;

/** Leaks through an interface: one implementation returns every annotator's answers. */
public final class InterfaceScreen {
    private final AnswerSource source;

    /** Creates the screen. */
    public InterfaceScreen(AnswerSource source) {
        this.source = source;
    }

    /** Shows how many answers an item has. */
    public int show(long itemId) {
        return source.load(itemId).size();
    }
}
