package arbiter.data.annotation;

import java.util.List;
import java.util.Optional;

import arbiter.model.annotation.Annotation;

/** Stores annotators' answers. */
public interface AnnotationRepository {
    /** Inserts or updates an answer and returns the stored copy. */
    Annotation save(Annotation annotation);

    /** Returns the answer with this identifier, if any. */
    Optional<Annotation> findById(long id);

    /** Returns every answer recorded for an item. */
    List<Annotation> listByItem(long itemId);

    /** Returns one annotator's answer for an item, if any, never anyone else's. */
    Optional<Annotation> findByItemAndAnnotator(long itemId, long annotatorId);

    /** Returns every answer recorded under an assignment. */
    List<Annotation> listByAssignment(long assignmentId);

    /**
     * Counts an annotator's valid answers, as {@code Annotation.isValidAnswer} defines them, for items
     * that are not retired. This is the lifetime annotation total (#19), not handled-work progress
     * (#18), which counts every {@code Annotation.isSubmitted}.
     */
    long countValidSubmittedByAnnotator(long annotatorId);
}
