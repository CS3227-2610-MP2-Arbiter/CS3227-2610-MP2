package arbiter.blindness.fixture.ui.annotator;

import arbiter.blindness.fixture.service.Progress;
import arbiter.blindness.fixture.service.ScopedAnswers;
import arbiter.data.json.JsonStore;

/**
 * Blind: reads only through the trusted path and annotator-safe methods, so nothing is reported.
 * It constructs {@code Progress} itself, which must not make that service's adjudicator-only method
 * reachable.
 */
public final class BlindScreen {
    private final ScopedAnswers answers;
    private final JsonStore store;

    /** Creates the screen. */
    public BlindScreen(ScopedAnswers answers, JsonStore store) {
        this.answers = answers;
        this.store = store;
    }

    /** Shows the annotator's own answer, own assignments and one assignment by identifier. */
    public int show(long itemId, long annotatorId, long assignmentId) {
        boolean assigned = store.read(session -> session.assignments().findById(assignmentId)).isPresent();
        return answers.forCurrentUser(itemId, annotatorId).size() + new Progress(store).own(annotatorId).size()
                + (assigned ? 1 : 0);
    }
}
