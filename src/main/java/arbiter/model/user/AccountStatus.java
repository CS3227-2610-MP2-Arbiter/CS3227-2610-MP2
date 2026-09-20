package arbiter.model.user;

/**
 * Whether an account may be used. Deactivation is a soft delete, so a disabled account's
 * annotations remain history.
 */
public enum AccountStatus {
    ACTIVE,
    DISABLED
}
