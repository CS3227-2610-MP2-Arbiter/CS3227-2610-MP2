package arbiter.blindness.fixture.service;

import java.util.List;

import arbiter.model.annotation.Annotation;

/** Loads answers for an item; what it may return depends on the implementation. */
public interface AnswerSource {
    /** Returns answers for an item. */
    List<Annotation> load(long itemId);
}
