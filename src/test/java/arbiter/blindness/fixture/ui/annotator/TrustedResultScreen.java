package arbiter.blindness.fixture.ui.annotator;

import arbiter.blindness.fixture.service.ScopedAnswers;

/** Leaks through a trusted entry point whose signature returns a resolved result. */
public final class TrustedResultScreen {
    private final ScopedAnswers answers;

    /** Creates the screen. */
    public TrustedResultScreen(ScopedAnswers answers) {
        this.answers = answers;
    }

    /** Shows whether an item is resolved. */
    public boolean show(long itemId) {
        return answers.forCurrentUser(itemId).isPresent();
    }
}
