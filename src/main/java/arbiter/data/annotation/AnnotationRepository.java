package arbiter.data.annotation;

import java.util.List;
import java.util.Optional;

import arbiter.model.annotation.Annotation;

/** Stores annotators' answers. */
public interface AnnotationRepository {
    /** Inserts one submitted answer and returns the stored copy. Existing answers are immutable. */
    Annotation insert(Annotation annotation);

    /** Returns the answer with this identifier, if any. */
    Optional<Annotation> findById(long id);

    /** Returns every answer recorded for an item. */
    List<Annotation> listByItem(long itemId);

    /** Returns one annotator's answer for an item, if any, never anyone else's. */
    Optional<Annotation> findByItemAndAnnotator(long itemId, long annotatorId);

    /** Returns every submitted answer recorded under an assignment. */
    List<Annotation> listByAssignment(long assignmentId);
}
