package arbiter.ui.annotator;

import arbiter.service.AssignmentProgress;

/**
 * Words an assignment's progress the same way on My splits and in its queue (#18): submitted, total and
 * remaining files, all from one {@link AssignmentProgress}.
 */
final class ProgressText {
    private ProgressText() {
    }

    /** Returns the progress line, such as "3 of 10 files submitted, 7 remaining". */
    static String of(AssignmentProgress assignment) {
        return assignment.submitted() + " of " + assignment.total() + " files submitted, " + assignment.remaining()
                + " remaining";
    }
}
