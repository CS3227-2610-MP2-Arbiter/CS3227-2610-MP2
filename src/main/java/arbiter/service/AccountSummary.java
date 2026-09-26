package arbiter.service;

import arbiter.model.user.AccountStatus;
import arbiter.model.user.Role;

/**
 * One account as the adjudicator's account list (#31) and assignment form (#32) show it, without its credentials
 * (#23).
 *
 * @param id the account's identifier
 * @param username the account's username, in the case it was typed
 * @param role the account's role
 * @param status whether the account may be used
 */
public record AccountSummary(long id, String username, Role role, AccountStatus status) {
    /** Returns whether this is an active annotator's account. */
    public boolean isActiveAnnotator() {
        return role == Role.ANNOTATOR && status == AccountStatus.ACTIVE;
    }
}
