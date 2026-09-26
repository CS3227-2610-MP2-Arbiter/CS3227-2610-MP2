package arbiter.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import arbiter.data.json.RepositorySession;
import arbiter.model.annotation.Annotation;
import arbiter.model.project.Split;
import arbiter.model.project.TaxonomyKind;
import arbiter.model.resolution.Resolution;
import arbiter.model.resolution.ResolutionMethod;

/** Settles classification items from their submitted answers (rule 10). */
final class ResolutionService {
    private ResolutionService() {
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
        Map<Long, Long> votes = answers.stream()
                .collect(Collectors.groupingBy(Annotation::getLabelId, Collectors.counting()));
        for (Map.Entry<Long, Long> vote : votes.entrySet()) {
            if (2 * vote.getValue() > k) {
                resolution.setMethod(ResolutionMethod.MAJORITY);
                resolution.setLabelId(vote.getKey());
                return Optional.of(resolution);
            }
        }
        return Optional.empty();
    }
}
