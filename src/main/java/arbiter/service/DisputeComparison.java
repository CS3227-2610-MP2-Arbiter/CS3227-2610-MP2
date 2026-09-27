package arbiter.service;

import java.util.List;

/**
 * One item in a project's dispute list (#34) as the adjudicator compares and decides it: its text, or why its
 * source cannot be read (rule 21), and its submitted labels. It names no annotator and no time.
 *
 * @param path the item's stored path
 * @param text the file's text, or null if its source cannot be read
 * @param failure the resolver's message saying why the source cannot be read, which names the file, or null if its
 *     text was read
 * @param answerKeys the keys of the labels in the item's k submitted answers, in taxonomy order
 * @param decisionKey the key of the label the adjudicator decided on, or null if it is undecided
 * @param taxonomy the project's taxonomy, whose labels a decision is chosen from
 */
public record DisputeComparison(String path, String text, String failure, List<String> answerKeys, String decisionKey,
        TaxonomySummary taxonomy) {
    /** Returns whether the file's text was read, so a decision can be saved. */
    public boolean readable() {
        return failure == null;
    }
}
