package arbiter.workspace;

/** Reports that a registered source could not be read, naming the affected path and the reason. */
public class SourceException extends WorkspaceException {
    private static final long serialVersionUID = 1L;

    private final String storedPath;
    private final SourceFailure reason;

    /**
     * Creates a source failure.
     *
     * @param storedPath the workspace-relative path that was resolved, null when none was recorded
     * @param reason why the source could not be read
     * @param message a message shown to the user
     */
    public SourceException(String storedPath, SourceFailure reason, String message) {
        super(message);
        this.storedPath = storedPath;
        this.reason = reason;
    }

    /**
     * Creates a source failure caused by an underlying file-system error.
     *
     * @param storedPath the workspace-relative path that was resolved, null when none was recorded
     * @param reason why the source could not be read
     * @param message a message shown to the user
     * @param cause the failure that caused this one
     */
    public SourceException(String storedPath, SourceFailure reason, String message, Throwable cause) {
        super(message, cause);
        this.storedPath = storedPath;
        this.reason = reason;
    }

    /** Returns the affected workspace-relative path, or null when none was recorded. */
    public String storedPath() {
        return storedPath;
    }

    /** Returns why the source could not be read. */
    public SourceFailure reason() {
        return reason;
    }
}
