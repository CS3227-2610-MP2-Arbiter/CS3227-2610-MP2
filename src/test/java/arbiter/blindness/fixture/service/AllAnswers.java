package arbiter.blindness.fixture.service;

import java.util.List;

import arbiter.data.json.JsonStore;
import arbiter.model.annotation.Annotation;

/** An answer source that returns every annotator's answers. */
public final class AllAnswers implements AnswerSource {
    private final JsonStore store;

    /** Creates the source over a store. */
    public AllAnswers(JsonStore store) {
        this.store = store;
    }

    @Override
    public List<Annotation> load(long itemId) {
        return store.read(session -> session.annotations().listByItem(itemId));
    }
}
