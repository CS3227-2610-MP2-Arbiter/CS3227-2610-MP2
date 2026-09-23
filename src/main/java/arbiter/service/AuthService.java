package arbiter.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import arbiter.data.json.JsonStore;
import arbiter.data.json.RepositorySession;
import arbiter.model.user.AccountStatus;
import arbiter.model.user.Role;
import arbiter.model.user.User;

/** Owns workspace owner setup and the in-memory login session. */
public final class AuthService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("[A-Za-z0-9]{8,}");
    private static final String LOGIN_FAILED = "Invalid username or password";
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
            User owner = new User();
            owner.setUsername(username);
            owner.setPasswordHash(stored.hash());
            owner.setPasswordSalt(stored.salt());
            owner.setRole(Role.ADJUDICATOR);
            owner.setAccountStatus(AccountStatus.ACTIVE);
            owner.setCreatedAt(Instant.now());
            session.users().save(owner);
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
                throw new AuthException("Account is disabled");
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
            throw new AuthException("Username must be 1 to 64 ASCII letters, digits, dots, underscores or hyphens");
        }
    }

    private static void validatePassword(String password) {
        if (!validPassword(password)) {
            throw new AuthException("Password must be at least 8 characters using only ASCII letters and digits");
        }
    }

    private static boolean validPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    private static CurrentUser identity(User user) {
        return new CurrentUser(user.getId(), user.getUsername(), user.getRole());
    }
}
