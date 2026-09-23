package arbiter.data.json;

/** Reports a workspace data-store failure without hiding its cause. */
public class JsonStoreException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Creates a failure with a message suitable for display. */
    public JsonStoreException(String message) {
        super(message);
    }

    /** Creates a failure with its underlying cause. */
    public JsonStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
