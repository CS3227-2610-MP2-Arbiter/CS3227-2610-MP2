package arbiter.data;

import java.util.List;

import arbiter.model.Flag;

/** Stores reports that an item's source material is unusable. */
public interface FlagRepository {
    /** Inserts or updates a flag and returns the stored copy. */
    Flag save(Flag flag);

    /** Returns the flags raised against an answer. */
    List<Flag> findByAnnotation(long annotationId);

    /** Returns the flags raised against an item, from every annotator. */
    List<Flag> findByItem(long itemId);

    /** Returns a project's flags, which the review screen lists. */
    List<Flag> findByProject(long projectId);
}
