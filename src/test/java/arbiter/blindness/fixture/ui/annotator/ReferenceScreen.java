package arbiter.blindness.fixture.ui.annotator;

import java.util.List;
import java.util.function.LongFunction;

import arbiter.data.annotation.AnnotationRepository;
import arbiter.model.annotation.Annotation;

/** Leaks answers through a method reference rather than a call. */
public final class ReferenceScreen {
    private final AnnotationRepository answers;

    /** Creates the screen. */
    public ReferenceScreen(AnnotationRepository answers) {
        this.answers = answers;
    }

    /** Returns a loader for every answer under an assignment. */
    public LongFunction<List<Annotation>> loader() {
        return answers::listByAssignment;
    }
}
