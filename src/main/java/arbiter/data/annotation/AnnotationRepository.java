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

    /** Returns one annotator's answer for an item, never anyone else's. */
    List<Annotation> listByItemAndAnnotator(long itemId, long annotatorId);

    /** Returns every answer recorded under an assignment. */
    List<Annotation> listByAssignment(long assignmentId);

    /**
     * Counts an annotator's submitted answers. Progress is derived from this.
     *
     * <p>Earnings are not: they also exclude retired items and depend on the project being marked
     * complete, so they are computed in {@code EarningsService} rather than from this count.
     */
    long countSubmittedByAnnotator(long annotatorId);

    /** Removes an answer, which adjudicators may do to any answer. */
    void deleteById(long id);
}
