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
     * Counts an annotator's valid answers, as {@code Annotation.isValidAnswer} defines them, for items
     * that are not retired.
     *
     * <p>This is the lifetime annotation count, not handled-work progress. Progress separately counts
     * every submitted annotation as handled ({@code Annotation.isSubmitted}), report-only outcomes
     * included, while retirement removes an item from the outstanding work without inventing a
     * submission.
     */
    long countValidSubmittedByAnnotator(long annotatorId);
}
