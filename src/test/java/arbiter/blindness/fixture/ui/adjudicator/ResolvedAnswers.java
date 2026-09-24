package arbiter.blindness.fixture.ui.adjudicator;

import java.util.List;

import arbiter.blindness.fixture.service.AnswerSource;
import arbiter.data.json.JsonStore;
import arbiter.model.annotation.Annotation;

/** An adjudicator-only answer source, which runs only after routing to the adjudicator. */
public final class ResolvedAnswers implements AnswerSource {
    private final JsonStore store;

    /** Creates the source over a store. */
    public ResolvedAnswers(JsonStore store) {
        this.store = store;
    }

    @Override
    public List<Annotation> load(long itemId) {
        store.read(session -> session.resolutions().findByItem(itemId));
        return store.read(session -> session.annotations().listByItem(itemId));
    }
}
