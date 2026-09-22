package arbiter.workspace;

/** Thrown for any workspace-related failure. */
public class WorkspaceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Creates an exception with a message shown to the user. */
    public WorkspaceException(String message) {
        super(message);
    }

    /** Creates an exception with a message shown to the user and an underlying cause. */
    public WorkspaceException(String message, Throwable cause) {
        super(message, cause);
    }
}
