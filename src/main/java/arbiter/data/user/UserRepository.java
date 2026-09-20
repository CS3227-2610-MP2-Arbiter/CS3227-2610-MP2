package arbiter.data.user;

import java.util.List;
import java.util.Optional;

import arbiter.model.user.Role;
import arbiter.model.user.User;

/** Stores accounts. */
public interface UserRepository {
    /** Inserts or updates an account and returns the stored copy. */
    User save(User user);

    /** Returns the account with this identifier, if any. */
    Optional<User> findById(long id);

    /** Returns the account with this username, compared case-insensitively. */
    Optional<User> findByUsername(String username);

    /** Returns every account, enabled or not. */
    List<User> listAll();

    /** Returns every account with this role, enabled or not. */
    List<User> listByRole(Role role);

    /**
     * Counts enabled accounts with this role. The last-adjudicator guard must ignore disabled
     * accounts, or two adjudicators could disable each other out of the workspace.
     */
    long countActiveByRole(Role role);
}
