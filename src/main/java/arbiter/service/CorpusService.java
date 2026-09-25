package arbiter.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.project.Item;
import arbiter.model.project.Split;
import arbiter.model.project.SplitItem;
import arbiter.workspace.ResolvedSource;
import arbiter.workspace.SourceException;
import arbiter.workspace.SourceResolver;
import arbiter.workspace.WorkspacePaths;

/**
 * Lists, registers and unregisters a project's items (#25), and generates, lists and deletes its splits (#28).
 *
 * <p>Every method that reads or writes items or splits first requires the signed-in adjudicator through
 * {@link AuthService#requireAdjudicator}, so anyone else gets its {@link AuthException}. Registration
 * only reads source files, through {@link SourceResolver}, and nothing here writes under {@code media/}
 * (rule 21).
 */
public final class CorpusService {
    private static final Pattern ITEMS_PER_SPLIT_PATTERN = Pattern.compile("0*[1-9][0-9]*");
    private static final Pattern SPLIT_NAME_PATTERN = Pattern.compile("Split ([0-9]{1,9})");

    private final JsonStore store;
    private final AuthService auth;
    private final WorkspacePaths paths;
    private final SourceResolver resolver;

    /** Manages the items and splits of one workspace's projects on behalf of whoever is signed in to {@code auth}. */
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
     * {@link arbiter.data.project.ItemRepository#deleteById} names (rule 5), and deletes its split too if it
     * was that split's last item, so no split is left empty (#28).
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
            Optional<SplitItem> membership = session.splitItems().findByItem(itemId);
            session.items().deleteById(itemId);
            if (membership.isPresent() && session.splitItems().countBySplit(membership.get().getSplitId()) == 0) {
                session.splits().deleteById(membership.get().getSplitId());
            }
            return null;
        });
    }

    /**
     * Returns a project's splits in creation order, read from one snapshot.
     *
     * @throws AuthException if the caller is not the signed-in adjudicator
     */
    public List<SplitSummary> listSplits(long projectId) {
        auth.requireAdjudicator();
        return store.read(session -> session.splits().listByProject(projectId).stream()
                .sorted(Comparator.comparing(Split::getId))
                .map(split -> summarize(session, split))
                .toList());
    }

    /**
     * Returns the sizes, in order, of the splits that generating this many items per split would now make
     * from the project's available items, which are those in no split.
     *
     * @param itemsPerSplitText the items per split as the adjudicator typed it, which must be a positive whole
     *     number once surrounding whitespace is stripped
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if that text breaks this rule, no project has this identifier, it has no
     *     available items, or it has fewer than that many
     */
    public List<Integer> previewSplits(long projectId, String itemsPerSplitText) {
        auth.requireAdjudicator();
        int itemsPerSplit = parseItemsPerSplit(itemsPerSplitText);
        return store.read(session -> {
            List<Long> available = requireAvailableItemIds(session, projectId, itemsPerSplit);
            // The sizes do not depend on the seed.
            return sizes(allocate(available, itemsPerSplit, 0));
        });
    }

    /**
     * Generates splits from the project's available items in one committed action, as rule 7 describes,
     * provided they still split into the previewed sizes.
     *
     * <p>The app picks the seed. Each new split records it, the requested items per split and one creation
     * time, and is named "Split n", numbered on from the project's highest split number. Every check runs
     * inside that action, so a stale project page cannot bypass them (#28).
     *
     * @param itemsPerSplitText the items per split, as {@link #previewSplits} takes it
     * @param previewed the sizes {@link #previewSplits} returned for this text
     * @return the stored splits in creation order
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if {@link #previewSplits} would reject this request, or the available items
     *     no longer split into the previewed sizes; nothing is stored
     */
    public List<Split> generateSplits(long projectId, String itemsPerSplitText, List<Integer> previewed) {
        auth.requireAdjudicator();
        int itemsPerSplit = parseItemsPerSplit(itemsPerSplitText);
        return store.write(session -> {
            long seed = new Random().nextLong();
            List<Long> available = requireAvailableItemIds(session, projectId, itemsPerSplit);
            List<List<Long>> allocation = allocate(available, itemsPerSplit, seed);
            if (!sizes(allocation).equals(previewed)) {
                throw new ProjectException("The files available to split have changed since the preview");
            }
            int number = highestSplitNumber(session, projectId);
            Instant createdAt = Instant.now();
            List<Split> stored = new ArrayList<>();
            for (List<Long> itemIds : allocation) {
                number++;
                Split split = new Split();
                split.setProjectId(projectId);
                split.setName("Split " + number);
                split.setSeed(seed);
                split.setRequestedBatchSize(itemsPerSplit);
                split.setCreatedAt(createdAt);
                Split saved = session.splits().save(split);
                for (int index = 0; index < itemIds.size(); index++) {
                    SplitItem membership = new SplitItem();
                    membership.setSplitId(saved.getId());
                    membership.setItemId(itemIds.get(index));
                    membership.setSequence(index + 1);
                    session.splitItems().save(membership);
                }
                stored.add(saved);
            }
            return stored;
        });
    }

    /**
     * Deletes a split in one committed action, removing the records
     * {@link arbiter.data.project.SplitRepository#deleteById} names, so its items become available again
     * (rule 14).
     *
     * <p>Both checks run inside that action, so a stale project page cannot bypass them (#28).
     *
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if no split has this identifier, or it has had its first assignment
     *     ({@link FirstAssignment}); nothing is changed
     */
    public void deleteSplit(long splitId) {
        auth.requireAdjudicator();
        store.write(session -> {
            if (session.splits().findById(splitId).isEmpty()) {
                throw new ProjectException("This split no longer exists");
            }
            if (FirstAssignment.reachedSplit(session, splitId)) {
                throw new ProjectException("A split cannot be deleted after its first assignment");
            }
            session.splits().deleteById(splitId);
            return null;
        });
    }

    /**
     * Shuffles the item identifiers with {@link Collections#shuffle} and a {@link Random} made from this
     * seed, then cuts them into splits of {@code itemsPerSplit} (rule 7), the last of which may be smaller
     * (#28). The same identifiers, items per split and seed always give the same splits.
     *
     * @param itemIds the available items' identifiers in registration order
     * @return each split's item identifiers in saved order
     */
    static List<List<Long>> allocate(List<Long> itemIds, int itemsPerSplit, long seed) {
        List<Long> shuffled = new ArrayList<>(itemIds);
        Collections.shuffle(shuffled, new Random(seed));
        List<List<Long>> splits = new ArrayList<>();
        for (int start = 0; start < shuffled.size(); start += itemsPerSplit) {
            splits.add(List.copyOf(shuffled.subList(start, Math.min(shuffled.size(), start + itemsPerSplit))));
        }
        return splits;
    }

    private static int parseItemsPerSplit(String text) {
        String stripped = text == null ? "" : text.strip();
        if (!ITEMS_PER_SPLIT_PATTERN.matcher(stripped).matches()) {
            throw new ProjectException("Files per split must be a positive whole number");
        }
        try {
            return Integer.parseInt(stripped);
        } catch (NumberFormatException e) {
            // The pattern leaves only a number too large for an int. No project has that many items, so the
            // capped number is still rejected as too many.
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Returns the identifiers of the project's items in no split, in registration order, requiring the
     * project to exist and to have at least {@code itemsPerSplit} of them.
     */
    private static List<Long> requireAvailableItemIds(RepositorySession session, long projectId,
            int itemsPerSplit) {
        if (session.projects().findById(projectId).isEmpty()) {
            throw new ProjectException("This project no longer exists");
        }
        List<Long> available = session.items().listByProject(projectId).stream()
                .map(Item::getId)
                .filter(itemId -> session.splitItems().findByItem(itemId).isEmpty())
                .sorted()
                .toList();
        if (available.isEmpty()) {
            throw new ProjectException("There are no registered files outside a split");
        }
        if (itemsPerSplit > available.size()) {
            throw new ProjectException(available.size() == 1
                    ? "Only 1 file is available to split"
                    : "Only " + available.size() + " files are available to split");
        }
        return available;
    }

    private static List<Integer> sizes(List<List<Long>> splits) {
        return splits.stream().map(List::size).toList();
    }

    private static int highestSplitNumber(RepositorySession session, long projectId) {
        return session.splits().listByProject(projectId).stream()
                .map(split -> SPLIT_NAME_PATTERN.matcher(split.getName()))
                .filter(Matcher::matches)
                .mapToInt(name -> Integer.parseInt(name.group(1)))
                .max()
                .orElse(0);
    }

    private static SplitSummary summarize(RepositorySession session, Split split) {
        List<Long> itemIds = session.splitItems().listBySplit(split.getId()).stream()
                .map(SplitItem::getItemId)
                .toList();
        return new SplitSummary(split.getId(), split.getName(), itemIds,
                FirstAssignment.reachedSplit(session, split.getId()));
    }
}
