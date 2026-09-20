package arbiter.data;

import java.util.List;

import arbiter.model.BoundingBox;

/** Stores boxes drawn on detection items. */
public interface BoundingBoxRepository {
    /** Inserts or updates a box and returns the stored copy. */
    BoundingBox save(BoundingBox box);

    /** Returns an answer's boxes in sequence order. */
    List<BoundingBox> findByAnnotation(long annotationId);

    /** Removes every box belonging to an answer. */
    void deleteByAnnotation(long annotationId);
}
