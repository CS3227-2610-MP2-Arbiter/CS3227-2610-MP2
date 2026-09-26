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
 */
public record AssignmentProgress(long assignmentId, String projectName, String splitName, AssignmentStatus status,
        int submitted, int total) {
    /**
     * Returns whether the assignment is finished, so its files are never reopened. This is the one definition,
     * taken from the stored status, which submission (#17) sets once every file has an answer.
     */
    public boolean finished() {
        return status == AssignmentStatus.SUBMITTED;
    }
}
