package arbiter.data;

import java.util.List;
import java.util.Optional;

import arbiter.model.Annotation;

/** Stores annotators' answers. */
public interface AnnotationRepository {
    /** Inserts or updates an answer and returns the stored copy. */
    Annotation save(Annotation annotation);

    /** Returns the answer with this identifier, if any. */
    Optional<Annotation> findById(long id);

    /** Returns every answer recorded for an item. */
    List<Annotation> findByItem(long itemId);

    /** Returns one annotator's answer for an item, never anyone else's. */
    List<Annotation> findByItemAndAnnotator(long itemId, long annotatorId);

    /** Returns every answer recorded under an assignment. */
    List<Annotation> findByAssignment(long assignmentId);

    /** Counts an annotator's submitted answers, which is what progress and earnings are derived from. */
    long countByAnnotatorAndSubmitted(long annotatorId);

    /** Removes an answer, which adjudicators may do to any answer. */
    void deleteById(long id);
}
