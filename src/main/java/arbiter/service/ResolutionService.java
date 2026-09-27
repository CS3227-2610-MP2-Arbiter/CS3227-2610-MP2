package arbiter.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.annotation.Annotation;
import arbiter.model.project.Item;
import arbiter.model.project.Split;
import arbiter.model.project.SplitItem;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.resolution.Resolution;
import arbiter.model.resolution.ResolutionMethod;
import arbiter.workspace.SourceException;
import arbiter.workspace.SourceResolver;
import arbiter.workspace.WorkspacePaths;

/**
 * Settles classification items from their submitted answers (rule 10): automatically as an item's kth answer is
 * submitted (#27), and by the adjudicator's manual resolution of a SINGLE item's dispute (#34).
 *
 * <p>Every public method first requires the signed-in adjudicator through {@link AuthService#requireAdjudicator},
 * so anyone else gets its {@link AuthException}. They read every annotator's answers, so only adjudicator screens
 * may call them (rule 1).
 */
public final class ResolutionService {
    private static final String NOT_LISTED = "This file is not one of the project's disputes";

    private final JsonStore store;
    private final AuthService auth;
    private final SourceResolver sources;

    /** Resolves one workspace's disputes on behalf of whoever is signed in to {@code auth}. */
    public ResolutionService(JsonStore store, AuthService auth, WorkspacePaths paths) {
        this.store = Objects.requireNonNull(store, "store");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.sources = new SourceResolver(Objects.requireNonNull(paths, "paths"));
    }

    /**
     * Returns a project's dispute list (#34), read from one snapshot: its undecided disputes, then the items the
     * adjudicator has decided, each group by split in creation order and then by position. A SCALE project has
     * none.
     *
     * @throws AuthException if the caller is not the signed-in adjudicator
     */
    public List<DisputeSummary> disputes(long projectId) {
        auth.requireAdjudicator();
        return store.read(session -> {
            List<DisputeSummary> summaries = new ArrayList<>();
            List<Split> splits = session.splits().listByProject(projectId).stream()
                    .sorted(Comparator.comparing(Split::getId))
                    .toList();
            for (Split split : splits) {
                List<SplitItem> members = session.splitItems().listBySplit(split.getId());
                for (int index = 0; index < members.size(); index++) {
                    // Counted in saved order, as the project page counts it, because unregistering a file before
                    // the first assignment leaves a gap in the stored sequence.
                    int position = index + 1;
                    long itemId = members.get(index).getItemId();
                    find(session, itemId).ifPresent(listed -> summaries.add(new DisputeSummary(itemId,
                            listed.item().getPath(), split.getName(), position, decisionKey(session, listed))));
                }
            }
            // A stable sort, so each group keeps the order of the splits and their positions.
            summaries.sort(Comparator.comparing(DisputeSummary::decided));
            return summaries;
        });
    }

    /**
     * Returns one item of the dispute list as the adjudicator compares and decides it (#34): its submitted labels,
     * its decision and its project's taxonomy, read from one snapshot, and then its text. A file whose source is
     * missing, unreadable or changed is returned with the resolver's message instead of its text (rule 21).
     *
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if the item is not in its project's dispute list
     */
    public DisputeComparison comparison(long itemId) {
        auth.requireAdjudicator();
        Snapshot snapshot = store.read(session -> {
            ListedItem listed = require(session, itemId);
            TaxonomySummary taxonomy = CorpusService.taxonomyOf(session, listed.item().getProjectId());
            List<String> answerKeys = taxonomy.labels().stream()
                    .flatMap(label -> listed.answers().stream()
                            .filter(answer -> label.getId().equals(answer.getLabelId()))
                            .map(answer -> label.getKey()))
                    .toList();
            return new Snapshot(listed.item(), answerKeys, decisionKey(session, listed), taxonomy);
        });
        Item item = snapshot.item();
        String text = null;
        String failure = null;
        // The file is read outside the store's action, which holds the workspace's data, not its media.
        try {
            text = sources.resolve(item.getPath(), item.getContentHash()).text();
        } catch (SourceException e) {
            failure = e.getMessage();
        }
        return new DisputeComparison(item.getPath(), text, failure, snapshot.answerKeys(), snapshot.decisionKey(),
                snapshot.taxonomy());
    }

    /**
     * Saves the adjudicator's decision on an item of the dispute list in one committed action (#34): an
     * {@code ADJUDICATED} resolution with this label, the adjudicator and the current time, which replaces the
     * item's earlier decision in the same record. Its submitted answers are never changed (rule 13).
     *
     * <p>Every check runs inside that action, and any refusal stores nothing, so an out-of-date comparison cannot
     * bypass them.
     *
     * @throws AuthException if the caller is not the signed-in adjudicator
     * @throws ProjectException if the item is not in its project's dispute list, the label is not one of its
     *     project's labels, or its source cannot be read
     */
    public void adjudicate(long itemId, long labelId) {
        long adjudicatorId = auth.requireAdjudicator().id();
        store.write(session -> {
            ListedItem listed = require(session, itemId);
            TaxonomySummary taxonomy = CorpusService.taxonomyOf(session, listed.item().getProjectId());
            if (!taxonomy.accepts(new Answer.LabelChoice(labelId))) {
                throw new ProjectException("Choose one of this project's labels");
            }
            requireReadable(listed.item());
            Resolution resolution = listed.resolution() == null ? new Resolution() : listed.resolution();
            resolution.setItemId(itemId);
            resolution.setLabelId(labelId);
            resolution.setMethod(ResolutionMethod.ADJUDICATED);
            resolution.setDecidedByUserId(adjudicatorId);
            resolution.setDecidedAt(Instant.now());
            session.resolutions().save(resolution);
            return null;
        });
    }

    /**
     * Stores the automatic resolution (#27) of this answer's item, decided when the answer was submitted, if the
     * item has none yet and {@link #decide} gives one for its submitted answers. Call it inside the write action
     * that stores the answer, right after storing it.
     */
    static void resolveAutomatically(RepositorySession session, Annotation stored) {
        long itemId = stored.getItemId();
        if (session.resolutions().findByItem(itemId).isPresent()) {
            return;
        }
        long splitId = session.splitItems().findByItem(itemId).orElseThrow().getSplitId();
        Split split = session.splits().findById(splitId).orElseThrow();
        TaxonomyKind kind = session.taxonomySettings().findByProject(split.getProjectId()).orElseThrow().getKind();
        decide(kind, split.getAnnotationsPerItem(), session.annotations().listByItem(itemId), stored.getSubmittedAt())
                .ifPresent(session.resolutions()::save);
    }

    /**
     * Returns the automatic resolution rule 10 gives one item's submitted answers: {@code MAJORITY} to a SINGLE
     * item's strict-majority label, or {@code AUTO_SCALE} to a SCALE item's unrounded mean. It is decided by no
     * user.
     *
     * @param k the item's split's annotations per item
     * @param answers the item's submitted answers
     * @param decidedAt when the kth of those answers was submitted
     * @return the resolution, or empty if there are not exactly k answers or a SINGLE item has no strict majority
     */
    static Optional<Resolution> decide(TaxonomyKind kind, int k, List<Annotation> answers, Instant decidedAt) {
        if (answers.size() != k) {
            return Optional.empty();
        }
        Resolution resolution = new Resolution();
        resolution.setItemId(answers.getFirst().getItemId());
        resolution.setDecidedAt(decidedAt);
        if (kind == TaxonomyKind.SCALE) {
            resolution.setMethod(ResolutionMethod.AUTO_SCALE);
            resolution.setScaleValue(answers.stream().mapToInt(Annotation::getScaleValue).average().orElseThrow());
            return Optional.of(resolution);
        }
        return strictMajority(k, answers).map(labelId -> {
            resolution.setMethod(ResolutionMethod.MAJORITY);
            resolution.setLabelId(labelId);
            return resolution;
        });
    }

    /** Returns the label that more than half of a SINGLE item's k answers chose, if one did (rule 10). */
    private static Optional<Long> strictMajority(int k, List<Annotation> answers) {
        Map<Long, Long> votes = answers.stream()
                .collect(Collectors.groupingBy(Annotation::getLabelId, Collectors.counting()));
        for (Map.Entry<Long, Long> vote : votes.entrySet()) {
            if (2 * vote.getValue() > k) {
                return Optional.of(vote.getKey());
            }
        }
        return Optional.empty();
    }

    /**
     * Returns an item as its project's dispute list holds it (#34), if it does: a SINGLE item with k submitted
     * answers whose resolution is {@code ADJUDICATED}, or which has none and no strict-majority label, by the same
     * test as {@link #decide}. So an item that reached k answers with a strict majority but no resolution, as
     * before #27, is left out.
     */
    private static Optional<ListedItem> find(RepositorySession session, long itemId) {
        Optional<SplitItem> membership = session.splitItems().findByItem(itemId);
        if (membership.isEmpty()) {
            return Optional.empty();
        }
        Split split = session.splits().findById(membership.get().getSplitId()).orElseThrow();
        TaxonomyKind kind = session.taxonomySettings().findByProject(split.getProjectId()).orElseThrow().getKind();
        Integer k = split.getAnnotationsPerItem();
        List<Annotation> answers = session.annotations().listByItem(itemId);
        if (kind != TaxonomyKind.SINGLE || k == null || answers.size() != k) {
            return Optional.empty();
        }
        Resolution resolution = session.resolutions().findByItem(itemId).orElse(null);
        boolean listed = resolution == null ? strictMajority(k, answers).isEmpty()
                : resolution.getMethod() == ResolutionMethod.ADJUDICATED;
        return listed ? Optional.of(new ListedItem(session.items().findById(itemId).orElseThrow(), answers,
                resolution)) : Optional.empty();
    }

    /** Returns whether an item is in the undecided part of #34's dispute list in the current read snapshot. */
    static boolean isUnresolvedDispute(RepositorySession session, long itemId) {
        return find(session, itemId).filter(listed -> listed.resolution() == null).isPresent();
    }

    private static ListedItem require(RepositorySession session, long itemId) {
        return find(session, itemId).orElseThrow(() -> new ProjectException(NOT_LISTED));
    }

    /** Returns the key of the label a listed item was decided with, or null if it is undecided. */
    private static String decisionKey(RepositorySession session, ListedItem listed) {
        return listed.resolution() == null ? null
                : session.labels().findById(listed.resolution().getLabelId()).orElseThrow().getKey();
    }

    /** Refuses a file whose source no longer matches, with the resolver's message, which names the file. */
    private void requireReadable(Item item) {
        try {
            sources.resolve(item.getPath(), item.getContentHash());
        } catch (SourceException e) {
            throw new ProjectException(e.getMessage(), e);
        }
    }

    /**
     * An item of its project's dispute list: an undecided dispute, or an item the adjudicator has decided.
     *
     * @param item the item's record
     * @param answers its k submitted answers
     * @param resolution its {@code ADJUDICATED} resolution, or null if it is undecided
     */
    private record ListedItem(Item item, List<Annotation> answers, Resolution resolution) {
    }

    /** What {@link #comparison} reads from one snapshot, before it reads the file's text. */
    private record Snapshot(Item item, List<String> answerKeys, String decisionKey, TaxonomySummary taxonomy) {
    }
}
