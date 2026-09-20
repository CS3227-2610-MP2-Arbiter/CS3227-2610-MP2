package arbiter.model;

/** Whether an account may be used. PENDING is the extension point for later verification. */
public enum AccountStatus {
    ACTIVE,
    PENDING,
    DISABLED
}
