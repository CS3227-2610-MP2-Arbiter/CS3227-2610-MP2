package arbiter.blindness.fixture.service;

import java.util.List;

import arbiter.data.json.JsonStore;
import arbiter.model.project.Assignment;

/** A service with one annotator-safe read and one adjudicator-only read. */
public final class Progress {
    private final JsonStore store;

    /** Creates the service over a store. */
    public Progress(JsonStore store) {
        this.store = store;
    }

    /** Returns one annotator's own assignments. */
    public List<Assignment> own(long annotatorId) {
        return store.read(session -> session.assignments().listByAnnotator(annotatorId));
    }

    /** Returns everyone's assignments on a split, which only the adjudicator may see. */
    public List<Assignment> team(long splitId) {
        return tally(splitId);
    }

    private List<Assignment> tally(long splitId) {
        return store.read(session -> session.assignments().listBySplit(splitId));
    }
}
