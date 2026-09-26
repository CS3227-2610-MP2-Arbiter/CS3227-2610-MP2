package arbiter.service;

/** Reports a project request the service rejected, with a message suitable for the UI. */
public class ProjectException extends RuntimeException {
    /** Creates a rejection whose message is shown to the user. */
    public ProjectException(String message) {
        super(message);
    }

    /** Creates a rejection whose message can be shown, caused by a failure that is logged but not shown. */
    public ProjectException(String message, Throwable cause) {
        super(message, cause);
    }
}
