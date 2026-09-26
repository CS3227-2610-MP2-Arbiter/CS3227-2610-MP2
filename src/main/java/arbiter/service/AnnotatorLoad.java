package arbiter.service;

/**
 * One active annotator a split's assignment form offers, with their current load (#32).
 *
 * @param id the annotator's account identifier
 * @param username the annotator's username
 * @param unfinishedAssignments the annotator's assignments not yet {@code SUBMITTED}, across every project
 * @param unfinishedFiles the files in those assignments' splits
 */
public record AnnotatorLoad(long id, String username, int unfinishedAssignments, long unfinishedFiles) {
}
