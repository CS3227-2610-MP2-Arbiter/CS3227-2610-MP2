package arbiter.data;

import java.util.List;
import java.util.Optional;

import arbiter.model.Role;
import arbiter.model.User;

/** Stores accounts. */
public interface UserRepository {
    /** Inserts or updates an account and returns the stored copy. */
    User save(User user);

    /** Returns the account with this identifier, if any. */
    Optional<User> findById(long id);

    /** Returns the account with this email, compared case-insensitively. */
    Optional<User> findByEmail(String email);

    /** Returns every account. */
    List<User> findAll();

    /** Returns every account with this role. */
    List<User> findByRole(Role role);

    /** Counts accounts with this role, used to protect the last adjudicator. */
    long countByRole(Role role);
}
