package arbiter.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.user.AccountStatus;
import arbiter.model.user.Role;
import arbiter.model.user.User;

/**
 * Owns workspace owner setup, the in-memory login session, and the adjudicator's management of annotator
 * accounts (#31) and their passwords (#23).
 *
 * <p>Owner setup, annotator creation and password replacement share one username and password check and
 * one salted hashing. Every account-management method first requires the signed-in adjudicator through
 * {@link #requireAdjudicator}.
 */
public final class AuthService {
    /** States the username rule, both when refusing a username and as the hint on every form that sets one. */
    public static final String USERNAME_RULE =
            "Username must be 1 to 64 ASCII letters, digits, dots, underscores or hyphens";

    /** States the password rule, both when refusing a password and as the hint on every form that sets one. */
    public static final String PASSWORD_RULE =
            "Password must be at least 8 characters using only ASCII letters and digits";

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("[A-Za-z0-9]{8,}");
    private static final String LOGIN_FAILED = "Invalid username or password";
    private static final String ACCOUNT_DISABLED = "Account is disabled";
    private static final String INVALID_OWNER = "The workspace owner records are invalid";

    private final JsonStore store;
    private Long currentUserId;

    /** Creates authentication for one workspace. A new instance starts signed out. */
    public AuthService(JsonStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    /** Returns whether this workspace still needs its first owner account. */
    public boolean needsBootstrap() {
        return store.read(AuthService::bootstrapNeeded);
    }

    /** Creates the sole active adjudicator in one committed action. */
    public void bootstrapOwner(String username, String password) {
        validateUsername(username);
        validatePassword(password);
        PasswordHasher.StoredPassword stored = PasswordHasher.hash(password);
        store.write(session -> {
            if (!bootstrapNeeded(session)) {
                throw new AuthException("This workspace already has an owner");
            }
            session.users().save(newAccount(username, stored, Role.ADJUDICATOR));
            return null;
        });
    }

    /** Signs in an active account, revealing disabled status only after password verification. */
    public CurrentUser login(String username, String password) {
        currentUserId = null;
        if (username == null || !USERNAME_PATTERN.matcher(username).matches() || !validPassword(password)) {
            throw new AuthException(LOGIN_FAILED);
        }
        CurrentUser signedIn = store.read(session -> {
            if (bootstrapNeeded(session)) {
                throw new AuthException("Set up the workspace owner before signing in");
            }
            User user = session.users().findByUsername(username).orElse(null);
            if (user == null || !PasswordHasher.verify(password, user.getPasswordHash(), user.getPasswordSalt())) {
                throw new AuthException(LOGIN_FAILED);
            }
            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new AuthException(ACCOUNT_DISABLED);
            }
            return identity(user);
        });
        currentUserId = signedIn.id();
        return signedIn;
    }

    /** Clears the current session. */
    public void logout() {
        currentUserId = null;
    }

    /** Returns the current identity only while its stored account remains active. */
    public Optional<CurrentUser> currentUser() {
        Long id = currentUserId;
        if (id == null) {
            return Optional.empty();
        }
        Optional<CurrentUser> active = store.read(session -> {
            bootstrapNeeded(session);
            return session.users().findById(id)
                    .filter(user -> user.getAccountStatus() == AccountStatus.ACTIVE)
                    .map(AuthService::identity);
        });
        if (active.isEmpty()) {
            currentUserId = null;
        }
        return active;
    }

    /** Requires a current active adjudicator for later owner-only services. */
    public CurrentUser requireAdjudicator() {
        CurrentUser user = currentUser().orElseThrow(() -> new AuthException("Sign in as the adjudicator"));
        if (user.role() != Role.ADJUDICATOR) {
            throw new AuthException("Only the adjudicator can perform this action");
        }
        return user;
    }

    /** Requires a current active annotator for the annotator's own services. */
    public CurrentUser requireAnnotator() {
        CurrentUser user = currentUser().orElseThrow(() -> new AuthException("Sign in as an annotator"));
        if (user.role() != Role.ANNOTATOR) {
            throw new AuthException("Only an annotator can perform this action");
        }
        return user;
    }

    /**
     * Returns every account in identifier order, which is creation order with the owner first.
     *
     * @throws AuthException if the caller is not the signed-in adjudicator
     */
    public List<AccountSummary> listAccounts() {
        requireAdjudicator();
        return store.read(session -> session.users().listAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(AuthService::summarize)
                .toList());
    }

    /**
     * Creates an active annotator in one committed action, keeping the username as typed.
     *
     * @return the stored account's summary
     * @throws AuthException if the caller is not the signed-in adjudicator, the username or password fails
     *     owner setup's check, or any account, disabled ones included, has this username in any letter case;
     *     nothing is stored
     */
    public AccountSummary createAnnotator(String username, String password) {
        requireAdjudicator();
        validateUsername(username);
        validatePassword(password);
        PasswordHasher.StoredPassword stored = PasswordHasher.hash(password);
        return store.write(session -> {
            if (session.users().findByUsername(username).isPresent()) {
                throw new AuthException("An account with this username already exists");
            }
            return summarize(session.users().save(newAccount(username, stored, Role.ANNOTATOR)));
        });
    }

    /**
     * Permanently disables an active annotator in one committed action, keeping their work and assignments
     * (rule 19). Only the account's status changes.
     *
     * @throws AuthException if the caller is not the signed-in adjudicator, or the account is missing, the
     *     owner or already disabled; nothing is changed
     */
    public void deactivateAnnotator(long accountId) {
        requireAdjudicator();
        store.write(session -> {
            User annotator = requireActiveAnnotator(session, accountId);
            annotator.setAccountStatus(AccountStatus.DISABLED);
            session.users().save(annotator);
            return null;
        });
    }

    /**
     * Replaces an active annotator's password in one committed action. Only the account's hash and salt
     * change.
     *
     * @throws AuthException if the caller is not the signed-in adjudicator, the password fails owner setup's
     *     check, or the account is missing, the owner or disabled; nothing is changed
     */
    public void resetAnnotatorPassword(long accountId, String password) {
        requireAdjudicator();
        validatePassword(password);
        PasswordHasher.StoredPassword stored = PasswordHasher.hash(password);
        store.write(session -> {
            User annotator = requireActiveAnnotator(session, accountId);
            annotator.setPasswordHash(stored.hash());
            annotator.setPasswordSalt(stored.salt());
            session.users().save(annotator);
            return null;
        });
    }

    private static User requireActiveAnnotator(RepositorySession session, long accountId) {
        User user = session.users().findById(accountId)
                .orElseThrow(() -> new AuthException("This account does not exist"));
        if (user.getRole() != Role.ANNOTATOR) {
            throw new AuthException("The workspace owner's account cannot be changed");
        }
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AuthException(ACCOUNT_DISABLED);
        }
        return user;
    }

    private static User newAccount(String username, PasswordHasher.StoredPassword stored, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(stored.hash());
        user.setPasswordSalt(stored.salt());
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        return user;
    }

    /** Returns an account without its credentials. */
    static AccountSummary summarize(User user) {
        return new AccountSummary(user.getId(), user.getUsername(), user.getRole(), user.getAccountStatus());
    }

    private static boolean bootstrapNeeded(RepositorySession session) {
        List<User> users = session.users().listAll();
        if (session.isPristine()) {
            if (!users.isEmpty()) {
                throw new AuthException(INVALID_OWNER);
            }
            return true;
        }
        List<User> owners = session.users().listByRole(Role.ADJUDICATOR);
        if (owners.size() != 1 || owners.getFirst().getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AuthException(INVALID_OWNER);
        }
        return false;
    }

    private static void validateUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new AuthException(USERNAME_RULE);
        }
    }

    private static void validatePassword(String password) {
        if (!validPassword(password)) {
            throw new AuthException(PASSWORD_RULE);
        }
    }

    private static boolean validPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    private static CurrentUser identity(User user) {
        return new CurrentUser(user.getId(), user.getUsername(), user.getRole());
    }
}
