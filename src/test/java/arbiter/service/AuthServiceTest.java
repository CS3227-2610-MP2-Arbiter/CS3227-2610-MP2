package arbiter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.data.json.JsonStore;
import arbiter.model.user.AccountStatus;
import arbiter.model.user.Role;
import arbiter.model.user.User;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;

/** Integration checks for first-owner setup and shared login. */
class AuthServiceTest {
    private static final String PASSWORD = "password8";

    @TempDir
    Path temporary;

    @Test
    void bootstrapOwner_firstSetup_persistsOneActiveOwnerWithoutSigningIn() throws Exception {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        JsonStore store = JsonStore.initializeNew(paths);
        AuthService auth = new AuthService(store);

        assertTrue(auth.needsBootstrap());
        auth.bootstrapOwner("Owner", PASSWORD);

        assertFalse(auth.needsBootstrap());
        assertTrue(auth.currentUser().isEmpty());
        User owner = JsonStore.open(paths).read(session -> session.users().listAll().getFirst());
        assertEquals("Owner", owner.getUsername());
        assertEquals(Role.ADJUDICATOR, owner.getRole());
        assertEquals(AccountStatus.ACTIVE, owner.getAccountStatus());
        assertNotNull(owner.getId());
        assertNotEquals(PASSWORD, owner.getPasswordHash());
        assertFalse(Files.readString(paths.dataFile()).contains(PASSWORD));
        assertTrue(PasswordHasher.verify(PASSWORD, owner.getPasswordHash(), owner.getPasswordSalt()));
        assertFalse(new AuthService(JsonStore.open(paths)).needsBootstrap());
    }

    @Test
    void bootstrapOwner_invalidCredentials_allowsValidRetry() {
        JsonStore store = freshStore("workspace");
        AuthService auth = new AuthService(store);

        for (String username : new String[] {null, "", "bad name", "bad!", "caf\u00e9", "a".repeat(65)}) {
            assertThrows(AuthException.class, () -> auth.bootstrapOwner(username, PASSWORD));
            assertTrue(auth.needsBootstrap());
        }
        for (String password : new String[] {null, "", "1234567", "        ", " password8",
            "pass word8", "password8 ", "password!", "pass.word8", "pass\tword",
            "pass\nword", "password\u007f"}) {
            assertThrows(AuthException.class, () -> auth.bootstrapOwner("owner", password));
            assertTrue(auth.needsBootstrap());
        }

        auth.bootstrapOwner("a", PASSWORD);
        assertEquals(1, store.<Integer>read(session -> session.users().listAll().size()));
        assertEquals("a", auth.login("a", PASSWORD).username());
        assertThrows(AuthException.class, () -> auth.login("a", ""));
    }

    @Test
    void bootstrapOwner_maximumLengthUsernameAndLongerPassword_accepted() {
        JsonStore store = freshStore("workspace");
        AuthService auth = new AuthService(store);
        String username = "A".repeat(61) + "._-";

        auth.bootstrapOwner(username, "Abcd123456");

        assertEquals(username, auth.login(username.toLowerCase(), "Abcd123456").username());
    }

    @Test
    void bootstrapOwner_eightLettersOrDigits_accepted() {
        AuthService letters = new AuthService(freshStore("letters"));
        letters.bootstrapOwner("owner", "abcdefgh");
        assertEquals("owner", letters.login("owner", "abcdefgh").username());

        AuthService digits = new AuthService(freshStore("digits"));
        digits.bootstrapOwner("owner", "12345678");
        assertEquals("owner", digits.login("owner", "12345678").username());
    }

    @Test
    void bootstrapOwner_unicodePassword_rejectedRegardlessOfLength() {
        AuthService auth = new AuthService(freshStore("workspace"));
        String emoji = "\uD83D\uDE00";

        assertThrows(AuthException.class, () -> auth.bootstrapOwner("owner", emoji.repeat(4)));
        assertTrue(auth.needsBootstrap());
        assertThrows(AuthException.class, () -> auth.bootstrapOwner("owner", emoji.repeat(8)));
        assertTrue(auth.needsBootstrap());
    }

    @Test
    void bootstrapOwner_afterOwnerAlreadyExists_rejectedAcrossReopen() {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        AuthService first = new AuthService(JsonStore.initializeNew(paths));
        first.bootstrapOwner("owner", PASSWORD);
        AuthService reopened = new AuthService(JsonStore.open(paths));

        assertThrows(AuthException.class, () -> reopened.bootstrapOwner("another", PASSWORD));
        assertEquals(1, JsonStore.open(paths).<Integer>read(session -> session.users().listAll().size()));
    }

    @Test
    void bootstrapOwner_whenAnotherAccountExists_rejected() {
        JsonStore store = freshStore("workspace");
        store.write(session -> session.users().save(user("annotator", Role.ANNOTATOR, AccountStatus.ACTIVE)));
        AuthService auth = new AuthService(store);

        assertThrows(AuthException.class, auth::needsBootstrap);
        assertThrows(AuthException.class, () -> auth.bootstrapOwner("owner", PASSWORD));
        assertEquals(1, store.<Integer>read(session -> session.users().listAll().size()));
    }

    @Test
    void bootstrapOwner_whenOwnerDisabledOrExtraOwnerExists_rejected() {
        JsonStore disabledStore = freshStore("disabled");
        disabledStore.write(session -> session.users().save(user("owner", Role.ADJUDICATOR,
                AccountStatus.DISABLED)));
        assertThrows(AuthException.class, () -> new AuthService(disabledStore).bootstrapOwner("new", PASSWORD));

        JsonStore extraStore = freshStore("extra");
        extraStore.write(session -> {
            session.users().save(user("owner", Role.ADJUDICATOR, AccountStatus.ACTIVE));
            session.users().save(user("extra", Role.ADJUDICATOR, AccountStatus.ACTIVE));
            return null;
        });
        assertThrows(AuthException.class, () -> new AuthService(extraStore).bootstrapOwner("new", PASSWORD));
        assertEquals(2, extraStore.<Integer>read(session -> session.users().listAll().size()));
    }

    @Test
    void bootstrapOwner_concurrentCallers_onlyOneSucceeds() throws Exception {
        JsonStore store = freshStore("workspace");
        AuthService first = new AuthService(store);
        AuthService second = new AuthService(store);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> firstResult = executor.submit(() -> bootstrapAfter(start, first, "first"));
            Future<Boolean> secondResult = executor.submit(() -> bootstrapAfter(start, second, "second"));
            start.countDown();

            int successes = (firstResult.get(30, TimeUnit.SECONDS) ? 1 : 0)
                    + (secondResult.get(30, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
        }
        assertEquals(1, store.<Integer>read(session -> session.users().listAll().size()));
    }

    @Test
    void login_ownerCaseInsensitiveAndNewService_restoresOnlyOwnSession() {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve("workspace"));
        AuthService first = new AuthService(JsonStore.initializeNew(paths));
        first.bootstrapOwner("Owner", PASSWORD);
        AuthService reopened = new AuthService(JsonStore.open(paths));

        CurrentUser current = reopened.login("oWnEr", PASSWORD);

        assertEquals("Owner", current.username());
        assertEquals(Role.ADJUDICATOR, current.role());
        assertEquals(current, reopened.currentUser().orElseThrow());
        assertEquals(current, reopened.requireAdjudicator());
        assertTrue(first.currentUser().isEmpty());
        reopened.logout();
        assertTrue(reopened.currentUser().isEmpty());
        assertThrows(AuthException.class, reopened::requireAdjudicator);
    }

    @Test
    void login_annotatorAndDisabledAccount_enforcesRoleAndStatus() {
        JsonStore store = freshStore("workspace");
        AuthService auth = new AuthService(store);
        auth.bootstrapOwner("owner", PASSWORD);
        PasswordHasher.StoredPassword stored = PasswordHasher.hash("annotator8");
        long annotatorId = store.write(session -> {
            User annotator = user("annotator", Role.ANNOTATOR, AccountStatus.ACTIVE);
            annotator.setPasswordHash(stored.hash());
            annotator.setPasswordSalt(stored.salt());
            return session.users().save(annotator).getId();
        });

        assertEquals(Role.ANNOTATOR, auth.login("ANNOTATOR", "annotator8").role());
        assertThrows(AuthException.class, auth::requireAdjudicator);
        store.write(session -> {
            User annotator = session.users().findById(annotatorId).orElseThrow();
            annotator.setAccountStatus(AccountStatus.DISABLED);
            session.users().save(annotator);
            return null;
        });

        assertTrue(auth.currentUser().isEmpty());
        assertThrows(AuthException.class, auth::requireAdjudicator);
        AuthException wrongPassword = assertThrows(AuthException.class, () -> auth.login("annotator", "incorrect"));
        AuthException disabled = assertThrows(AuthException.class, () -> auth.login("annotator", "annotator8"));
        assertEquals("Invalid username or password", wrongPassword.getMessage());
        assertEquals("Account is disabled", disabled.getMessage());
        assertTrue(auth.currentUser().isEmpty());
    }

    @Test
    void login_unknownUsernameAndWrongPassword_shareGenericFailure() {
        AuthService auth = new AuthService(freshStore("workspace"));
        auth.bootstrapOwner("owner", PASSWORD);

        AuthException unknown = assertThrows(AuthException.class, () -> auth.login("missing", PASSWORD));
        AuthException wrong = assertThrows(AuthException.class, () -> auth.login("owner", "incorrect"));
        AuthException unicodeName = assertThrows(AuthException.class, () -> auth.login("own\u00e9r", PASSWORD));
        String nonAsciiPassword = PASSWORD + "\u00e9";
        AuthException unicodePassword = assertThrows(AuthException.class, () -> auth.login("owner", nonAsciiPassword));

        assertEquals(unknown.getMessage(), wrong.getMessage());
        assertEquals(unknown.getMessage(), unicodeName.getMessage());
        assertEquals(unknown.getMessage(), unicodePassword.getMessage());
        assertTrue(auth.currentUser().isEmpty());
    }

    @Test
    void login_passwordWithSpacesOrPunctuation_returnsGenericFailure() {
        AuthService auth = new AuthService(freshStore("workspace"));
        auth.bootstrapOwner("owner", PASSWORD);
        AuthException unknown = assertThrows(AuthException.class, () -> auth.login("missing", PASSWORD));

        for (String password : new String[] {"        ", " password8", "pass word8", "password8 ",
            "password!", "pass.word8"}) {
            AuthException failure = assertThrows(AuthException.class, () -> auth.login("owner", password));
            assertEquals(unknown.getMessage(), failure.getMessage());
            assertTrue(auth.currentUser().isEmpty());
        }
    }

    @Test
    void requireAdjudicator_ownerDisabledAfterLogin_denied() {
        JsonStore store = freshStore("workspace");
        AuthService auth = new AuthService(store);
        auth.bootstrapOwner("owner", PASSWORD);
        long ownerId = auth.login("owner", PASSWORD).id();
        store.write(session -> {
            User owner = session.users().findById(ownerId).orElseThrow();
            owner.setAccountStatus(AccountStatus.DISABLED);
            session.users().save(owner);
            return null;
        });

        assertThrows(AuthException.class, auth::requireAdjudicator);
    }

    private JsonStore freshStore(String folder) {
        WorkspacePaths paths = new WorkspaceService().create(temporary.resolve(folder));
        return JsonStore.initializeNew(paths);
    }

    private static boolean bootstrapAfter(CountDownLatch start, AuthService auth, String username)
            throws InterruptedException {
        start.await();
        try {
            auth.bootstrapOwner(username, PASSWORD);
            return true;
        } catch (AuthException expected) {
            return false;
        }
    }

    private static User user(String username, Role role, AccountStatus status) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("synthetic-hash");
        user.setPasswordSalt("synthetic-salt");
        user.setRole(role);
        user.setAccountStatus(status);
        user.setCreatedAt(Instant.parse("2026-09-23T00:00:00Z"));
        return user;
    }
}
