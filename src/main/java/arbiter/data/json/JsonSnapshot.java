package arbiter.data.json;

import java.util.ArrayList;
import java.util.List;

import arbiter.model.annotation.Annotation;
import arbiter.model.project.Assignment;
import arbiter.model.project.Item;
import arbiter.model.project.Label;
import arbiter.model.project.Project;
import arbiter.model.project.Split;
import arbiter.model.project.SplitItem;
import arbiter.model.project.TaxonomySettings;
import arbiter.model.resolution.Resolution;
import arbiter.model.user.User;

/** One committed version of all workspace records. */
record JsonSnapshot(int schemaVersion, long nextId, List<User> users, List<Project> projects,
        List<TaxonomySettings> taxonomySettings, List<Label> labels, List<Item> items,
        List<Split> splits, List<SplitItem> splitItems, List<Assignment> assignments,
        List<Annotation> annotations, List<Resolution> resolutions) {
    static final int CURRENT_VERSION = 1;

    static JsonSnapshot empty() {
        return new JsonSnapshot(CURRENT_VERSION, 1L, new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }
}
