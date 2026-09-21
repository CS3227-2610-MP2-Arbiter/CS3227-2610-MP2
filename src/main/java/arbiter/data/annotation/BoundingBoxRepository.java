package arbiter.data.annotation;

import java.util.List;

import arbiter.model.annotation.BoundingBox;

/** Stores boxes drawn on detection items. */
public interface BoundingBoxRepository {
    /** Inserts or updates a box and returns the stored copy. */
    BoundingBox save(BoundingBox box);

    /** Returns an answer's boxes in sequence order. */
    List<BoundingBox> listByAnnotation(long annotationId);

    /**
     * Removes every box of a draft answer in an incomplete project, so autosave can replace them.
     * Submitted boxes are permanent (rule 18).
     */
    void deleteByAnnotation(long annotationId);
}
