package arbiter.data.annotation;

import java.util.List;

import arbiter.model.annotation.Flag;

/** Stores reports that an item's source material is unusable. */
public interface FlagRepository {
    /** Inserts or updates a flag and returns the stored copy. */
    Flag save(Flag flag);

    /** Returns the flags raised against an answer. */
    List<Flag> listByAnnotation(long annotationId);

    /** Returns the flags raised against an item, from every annotator. */
    List<Flag> listByItem(long itemId);

    /** Returns a project's flags, which the review screen lists. */
    List<Flag> listByProject(long projectId);
}
