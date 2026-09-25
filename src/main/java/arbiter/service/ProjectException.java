package arbiter.service;

/** Reports a project request the service rejected, with a message suitable for the UI. */
public class ProjectException extends RuntimeException {
    /** Creates a rejection whose message is shown to the user. */
    public ProjectException(String message) {
        super(message);
    }
}
