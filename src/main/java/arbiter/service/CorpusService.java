package arbiter.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import arbiter.data.json.JsonStore;
import arbiter.model.project.Item;
import arbiter.workspace.ResolvedSource;
import arbiter.workspace.SourceException;
import arbiter.workspace.SourceResolver;
import arbiter.workspace.WorkspacePaths;

/**
 * Lists, registers and unregisters a project's items (#25).
 *
 * <p>Every method that reads or writes items first requires the signed-in adjudicator through
 * {@link AuthService#requireAdjudicator}, so anyone else gets its {@link AuthException}. Registration
 * only reads source files, through {@link SourceResolver}, and nothing here writes under {@code media/}
 * (rule 21).
 */
public final class CorpusService {
    private final JsonStore store;
    private final AuthService auth;
    private final WorkspacePaths paths;
    private final SourceResolver resolver;

    /** Manages the items of one workspace's projects on behalf of whoever is signed in to {@code auth}. */
    public CorpusService(JsonStore store, AuthService auth, WorkspacePaths paths) {
        this.store = Objects.requireNonNull(store, "store");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.paths = Objects.requireNonNull(paths, "paths");
        this.resolver = new SourceResolver(paths);
    }

    /** Returns the workspace's {@code media/} folder, the only place files are registered from (rule 21). */
    public Path mediaDirectory() {
        return paths.mediaDirectory();
    }

    /**
     * Returns a project's items in registration order.
     *
     * @throws AuthException if the caller is not the signed-in adjudicator
     */
    public List<Item> list(long projectId) {
        auth.requireAdjudicator();
        return store.read(session -> session.items().listByProject(projectId).stream()
                .sorted(Comparator.comparing(Item::getId))
                .toList());
    }

    /**
     * Registers the chosen files as a project's items in one committed action, in the order given and
     * stamped with one import time.
     *
     * <p>Every check runs inside that action, so a stale project page cannot bypass them. The first file
     * that fails stops the whole set, so either every file is registered or none is (#25).
     *
     * @param files the chosen files, each checked and given its stored path by
     *     {@link SourceResolver#resolveForImport}
     * @return the stored items
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if no project has this identifier, it has had its first assignment
     *     ({@link FirstAssignment}), or a file's path is already registered in it, or its content is
     *     already in it or earlier in the selection; nothing is stored
     * @throws SourceException if {@link SourceResolver#resolveForImport} rejects a file; nothing is stored
     */
    public List<Item> register(long projectId, List<Path> files) {
        auth.requireAdjudicator();
        return store.write(session -> {
            FirstAssignment.requireNotReached(session, projectId,
                    "Files cannot be registered after the project's first assignment");
            Map<String, Item> registered = new HashMap<>();
            for (Item item : session.items().listByProject(projectId)) {
                registered.put(item.getPath(), item);
            }
            Map<String, String> chosen = new HashMap<>();
            Instant importedAt = Instant.now();
            List<Item> stored = new ArrayList<>();
            for (Path file : files) {
                ResolvedSource source = resolver.resolveForImport(file);
                String path = source.storedPath();
                String hash = source.contentHash();
                Item atPath = registered.get(path);
                if (atPath != null) {
                    throw new ProjectException(atPath.getContentHash().equals(hash)
                            ? path + " is already registered in this project"
                            : path + " has changed since this project registered it");
                }
                String earlier = chosen.putIfAbsent(hash, path);
                if (earlier != null) {
                    throw new ProjectException(earlier.equals(path)
                            ? path + " is selected more than once"
                            : path + " has the same content as " + earlier + ", which is also selected");
                }
                Optional<Item> sameContent = session.items().findByProjectAndHash(projectId, hash);
                if (sameContent.isPresent()) {
                    throw new ProjectException(path + " has the same content as " + sameContent.get().getPath()
                            + ", which is already registered in this project");
                }
                Item item = new Item();
                item.setProjectId(projectId);
                item.setPath(path);
                item.setContentHash(hash);
                item.setImportedAt(importedAt);
                stored.add(session.items().save(item));
            }
            return stored;
        });
    }

    /**
     * Unregisters one item in one committed action, removing the records
     * {@link arbiter.data.project.ItemRepository#deleteById} names (rule 5).
     *
     * <p>Both checks run inside that action, so a stale project page cannot bypass them (#25).
     *
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if no item has this identifier, or its project has had its first assignment
     *     ({@link FirstAssignment}); nothing is changed
     */
    public void unregister(long itemId) {
        auth.requireAdjudicator();
        store.write(session -> {
            Optional<Item> item = session.items().findById(itemId);
            if (item.isEmpty()) {
                throw new ProjectException("This item is no longer registered");
            }
            FirstAssignment.requireNotReached(session, item.get().getProjectId(),
                    "An item cannot be unregistered after its project's first assignment");
            session.items().deleteById(itemId);
            return null;
        });
    }
}
