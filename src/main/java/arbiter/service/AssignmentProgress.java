package arbiter.service;

import arbiter.model.project.AssignmentStatus;

/**
 * One of the signed-in annotator's assignments, as their home screen shows it (#12). It holds nothing about
 * any other annotator (rule 1).
 *
 * @param assignmentId the assignment's identifier
 * @param projectName the name of the split's project
 * @param splitName the split's name
 * @param status the assignment's status as stored, which submission (#17) keeps
 * @param submitted the split's files this annotator has answered
 * @param total the split's files
 * @param nextItemId the first file in saved split order this annotator has not answered, where the queue
 *     (#13) opens, or null once every file is answered
 */
public record AssignmentProgress(long assignmentId, String projectName, String splitName, AssignmentStatus status,
        int submitted, int total, Long nextItemId) {
    /** Returns whether the assignment is finished, so its files are never reopened. */
    public boolean finished() {
        return status == AssignmentStatus.SUBMITTED;
    }
}
