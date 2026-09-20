package arbiter.workspace;

/** Thrown when workspace metadata is missing, malformed or in an unsupported format. */
public class WorkspaceMetadataException extends WorkspaceException {
    private static final long serialVersionUID = 1L;

    /** Creates an exception with a message shown to the user. */
    public WorkspaceMetadataException(String message) {
        super(message);
    }

    /** Creates an exception with a message shown to the user and an underlying cause. */
    public WorkspaceMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
