package arbiter.blindness.fixture.service;

import java.util.List;
import java.util.Optional;

import arbiter.data.json.JsonStore;
import arbiter.model.annotation.Annotation;
import arbiter.model.resolution.Resolution;

/** Stands in for {@code AnnotationService}, whose {@code forCurrentUser} reads are trusted. */
public final class ScopedAnswers {
    private final JsonStore store;

    /** Creates the service over a store. */
    public ScopedAnswers(JsonStore store) {
        this.store = store;
    }

    /** Returns one annotator's answers for an item; it reads every answer to scope them, which is trusted. */
    public List<Annotation> forCurrentUser(long itemId, long annotatorId) {
        return store.read(session -> session.annotations().listByItem(itemId).stream()
                .filter(answer -> answer.getAnnotatorId() == annotatorId).toList());
    }

    /** A trusted entry point that wrongly hands back a resolved result, which its signature shows. */
    public Optional<Resolution> forCurrentUser(long itemId) {
        return store.read(session -> session.resolutions().findByItem(itemId));
    }
}
