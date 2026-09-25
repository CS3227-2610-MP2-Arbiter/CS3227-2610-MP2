package arbiter.ui.shared;

/**
 * Decides what a user reads about a failure.
 *
 * <p>An exception declared in an {@code arbiter} package carries a message written for the user, so
 * it is shown as it is. Such a message is also logged, so it must never hold a credential, password
 * hash, salt or source text. Any other exception is a programming error, so the user gets a generic
 * message and the details go to {@link DiagnosticLog}.
 */
public final class ErrorMessages {
    /** What the user reads when a failure's own message is not meant for them. */
    public static final String UNEXPECTED = "Arbiter could not finish that. The details were written to the log.";

    private ErrorMessages() {
    }

    /** Returns whether this failure's message is written for the user. */
    public static boolean isUserFacing(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getName().startsWith("arbiter.") && message != null && !message.isBlank();
    }

    /** Returns the message to show the user for this failure. */
    public static String of(Throwable error) {
        return isUserFacing(error) ? error.getMessage() : UNEXPECTED;
    }
}
