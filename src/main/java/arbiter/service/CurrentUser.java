package arbiter.service;

import arbiter.model.user.Role;

/** The signed-in identity exposed to application code, without stored credentials. */
public record CurrentUser(long id, String username, Role role) {
}
