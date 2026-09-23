package arbiter.model.user;

import java.time.Instant;

/** An account that can sign in. */
public class User {
    /** Persistent identifier. */
    private Long id;

    /** Login name. */
    private String username;

    /** Salted hash of the password, never the password itself. */
    private String passwordHash;

    /** Per-user salt stored beside the hash. */
    private String passwordSalt;

    /** Annotator or adjudicator. */
    private Role role;

    /** Whether the account may be used. */
    private AccountStatus accountStatus;

    /** When the account was created. */
    private Instant createdAt;

    /** Creates an empty User. */
    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
