package arbiter.service;

import arbiter.model.user.AccountStatus;
import arbiter.model.user.Role;
import arbiter.model.user.User;
import arbiter.testing.Records;

/**
 * Builds annotator accounts whose password works with {@link AuthService#login}.
 *
 * <p>It lives in this package because {@link PasswordHasher} is package-private.
 */
public final class TestAccounts {
    private TestAccounts() {
    }

    /** Returns an unsaved annotator account with real salted credentials for this password. */
    public static User annotator(String username, String password, AccountStatus status) {
        PasswordHasher.StoredPassword stored = PasswordHasher.hash(password);
        User user = Records.user(username, Role.ANNOTATOR);
        user.setPasswordHash(stored.hash());
        user.setPasswordSalt(stored.salt());
        user.setAccountStatus(status);
        return user;
    }
}
