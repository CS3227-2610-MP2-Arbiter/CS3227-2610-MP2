package arbiter.service;

/** Reports an authentication or account setup failure without exposing credentials. */
public class AuthException extends RuntimeException {
    /** Creates an authentication failure with a message suitable for the UI. */
    public AuthException(String message) {
        super(message);
    }

    /** Creates an authentication failure caused by invalid stored data. */
    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
