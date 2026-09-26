package arbiter.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import arbiter.data.json.JsonStore;
import arbiter.model.user.AccountStatus;
import arbiter.model.user.Role;
import arbiter.model.user.User;
import arbiter.testing.ClassificationWorkflow;
import arbiter.testing.TestWorkspace;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Integration checks for first-owner setup, shared login and account management (#31, #23). */
class AuthServiceTest {
    private static final String PASSWORD = "password8";
    private static final String NEW_PASSWORD = "newpass8";
    private static final String[] INVALID_PASSWORDS = {null, "1234567", "pass word8", "password!", "p\u00e4ssword8"};
    private static final String USERNAME_RULE =
            "Username must be 1 to 64 ASCII letters, digits, dots, underscores or hyphens";
    private static final String PASSWORD_RULE =
            "Password must be at least 8 characters using only ASCII letters and digits";
    private static final String LOGIN_FAILED = "Invalid username or password";
    private static final String ACCOUNT_DISABLED = "Account is disabled";
    private static final JsonMapper JSON = JsonMapper.builder().build();

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
        assertEquals(LOGIN_FAILED, wrongPassword.getMessage());
        assertEquals(ACCOUNT_DISABLED, disabled.getMessage());
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

    @Test
    void listAccounts_ownerAndAnnotators_ownerFirstThenCreationOrderWithRoleAndStatus() {
        TestWorkspace workspace = workspace();
        AuthService owner = workspace.signInOwner();
        AccountSummary ownerRow = new AccountSummary(owner.requireAdjudicator().id(), TestWorkspace.OWNER,
                Role.ADJUDICATOR, AccountStatus.ACTIVE);
        assertEquals(List.of(ownerRow), owner.listAccounts());

        AccountSummary zed = owner.createAnnotator("zed", PASSWORD);
        AccountSummary amy = owner.createAnnotator("Amy", PASSWORD);
        owner.deactivateAnnotator(zed.id());

        assertEquals(List.of(ownerRow,
                new AccountSummary(zed.id(), "zed", Role.ANNOTATOR, AccountStatus.DISABLED),
                new AccountSummary(amy.id(), "Amy", Role.ANNOTATOR, AccountStatus.ACTIVE)), owner.listAccounts());
    }

    @Test
    void manageAccounts_signedOut_refusedWithDataUnchanged() {
        TestWorkspace workspace = workspace();
        long amyId = ClassificationWorkflow.single("yes").annotator("amy").seed(workspace).annotatorId("amy");
        byte[] before = workspace.dataFileBytes();

        assertAllRefused(new AuthService(workspace.store()), amyId, "Sign in as the adjudicator");

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void manageAccounts_annotatorSignedIn_refusedWithDataUnchanged() {
        TestWorkspace workspace = workspace();
        ClassificationWorkflow work = ClassificationWorkflow.single("yes").annotator("amy").annotator("bob")
                .seed(workspace);
        AuthService amy = workspace.signIn("amy");
        byte[] before = workspace.dataFileBytes();

        // Neither another annotator nor the caller's own account, so there is no self-service reset.
        for (String target : new String[] {"bob", "amy"}) {
            assertAllRefused(amy, work.annotatorId(target), "Only the adjudicator can perform this action");
        }

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void createAnnotator_validCredentials_activeAnnotatorStoredAsTypedThatCanSignIn() {
        TestWorkspace workspace = workspace();
        AuthService owner = workspace.signInOwner();
        String password = "Abcd1234";

        AccountSummary created = owner.createAnnotator("New.Annotator_1-x", password);

        assertEquals("New.Annotator_1-x", created.username());
        assertEquals(Role.ANNOTATOR, created.role());
        assertEquals(AccountStatus.ACTIVE, created.status());
        JsonStore reopened = JsonStore.open(workspace.paths());
        User stored = reopened.read(session -> session.users().findById(created.id())).orElseThrow();
        assertEquals("New.Annotator_1-x", stored.getUsername());
        assertEquals(Role.ANNOTATOR, stored.getRole());
        assertEquals(AccountStatus.ACTIVE, stored.getAccountStatus());
        assertTrue(PasswordHasher.verify(password, stored.getPasswordHash(), stored.getPasswordSalt()));
        assertFalse(dataFileText(workspace).contains(password));
        assertEquals(1, reopened.<Integer>read(session -> session.users().listByRole(Role.ADJUDICATOR).size()));
        assertEquals(new CurrentUser(created.id(), "New.Annotator_1-x", Role.ANNOTATOR),
                new AuthService(reopened).login("new.annotator_1-X", password));
    }

    @Test
    void createAnnotator_invalidUsernameOrPassword_refusedWithNothingStored() {
        TestWorkspace workspace = workspace();
        AuthService owner = workspace.signInOwner();
        byte[] before = workspace.dataFileBytes();

        // A leading space is refused rather than stripped.
        for (String username : new String[] {null, "", " amy", "amy!", "caf\u00e9", "a".repeat(65)}) {
            assertEquals(USERNAME_RULE, refusalMessage(() -> owner.createAnnotator(username, PASSWORD)));
        }
        for (String password : INVALID_PASSWORDS) {
            assertEquals(PASSWORD_RULE, refusalMessage(() -> owner.createAnnotator("amy", password)));
        }

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void createAnnotator_oneAndSixtyFourCharacterUsernames_accepted() {
        AuthService owner = workspace().signInOwner();

        for (String username : new String[] {"b", "B".repeat(61) + "._-"}) {
            assertEquals(username, owner.createAnnotator(username, PASSWORD).username());
        }
    }

    @Test
    void createAnnotator_usernameTakenInAnyCaseIncludingDisabled_refusedWithNothingStored() {
        TestWorkspace workspace = workspace();
        ClassificationWorkflow.single("yes").annotator("amy").annotator("zed").disabled("zed").seed(workspace);
        AuthService owner = workspace.signInOwner();
        byte[] before = workspace.dataFileBytes();

        for (String username : new String[] {"amy", "Owner", "ZED"}) {
            String refusal = refusalMessage(() -> owner.createAnnotator(username, NEW_PASSWORD));
            assertEquals("An account with this username already exists", refusal);
        }

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void deactivateAnnotator_inProgressAnnotatorWithWork_changesOnlyTheirStatus() {
        TestWorkspace workspace = workspace();
        long partialId = seedWork(workspace).annotatorId("partial");
        AuthService owner = workspace.signInOwner();
        byte[] before = workspace.dataFileBytes();

        owner.deactivateAnnotator(partialId);

        assertOnlyAccountChanged(before, workspace, partialId, "accountStatus");
    }

    @Test
    void deactivateAnnotator_annotatorSignedIn_sessionEndsAndLoginRefusedAfterReopen() {
        TestWorkspace workspace = workspace();
        long amyId = ClassificationWorkflow.single("yes").annotator("amy").seed(workspace).annotatorId("amy");
        AuthService amy = workspace.signIn("amy");

        workspace.signInOwner().deactivateAnnotator(amyId);

        assertTrue(amy.currentUser().isEmpty());
        AuthService reopened = new AuthService(JsonStore.open(workspace.paths()));
        assertEquals(ACCOUNT_DISABLED, refusalMessage(() -> reopened.login("amy", TestWorkspace.PASSWORD)));
    }

    @Test
    void changeAccount_disabledAnnotator_refusedAndStaysDisabled() {
        TestWorkspace workspace = workspace();
        long amyId = ClassificationWorkflow.single("yes").annotator("amy").seed(workspace).annotatorId("amy");
        AuthService owner = workspace.signInOwner();
        owner.deactivateAnnotator(amyId);
        byte[] before = workspace.dataFileBytes();

        assertChangeRefused(owner, amyId, ACCOUNT_DISABLED);

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void changeAccount_owner_refusedWithDataUnchanged() {
        TestWorkspace workspace = workspace();
        AuthService owner = workspace.signInOwner();
        byte[] before = workspace.dataFileBytes();

        assertChangeRefused(owner, owner.requireAdjudicator().id(), "The workspace owner's account cannot be changed");

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void changeAccount_missingAccount_refusedWithDataUnchanged() {
        TestWorkspace workspace = workspace();
        long projectId = ClassificationWorkflow.single("yes").seed(workspace).projectId();
        AuthService owner = workspace.signInOwner();
        byte[] before = workspace.dataFileBytes();

        // An identifier never issued, and one issued to a record that is not an account.
        for (long missing : new long[] {0, projectId}) {
            assertChangeRefused(owner, missing, "This account does not exist");
        }

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    @Test
    void resetAnnotatorPassword_inProgressAnnotatorWithWork_changesOnlyHashAndSalt() {
        TestWorkspace workspace = workspace();
        long partialId = seedWork(workspace).annotatorId("partial");
        AuthService owner = workspace.signInOwner();
        byte[] before = workspace.dataFileBytes();

        owner.resetAnnotatorPassword(partialId, NEW_PASSWORD);

        assertOnlyAccountChanged(before, workspace, partialId, "passwordHash", "passwordSalt");
        User stored = workspace.store().read(session -> session.users().findById(partialId)).orElseThrow();
        assertTrue(PasswordHasher.verify(NEW_PASSWORD, stored.getPasswordHash(), stored.getPasswordSalt()));
        assertFalse(dataFileText(workspace).contains(NEW_PASSWORD));
    }

    @Test
    void resetAnnotatorPassword_afterReopen_oldPasswordFailsAndNewOneWorks() {
        TestWorkspace workspace = workspace();
        long amyId = ClassificationWorkflow.single("yes").annotator("amy").seed(workspace).annotatorId("amy");
        workspace.signInOwner().resetAnnotatorPassword(amyId, NEW_PASSWORD);

        AuthService reopened = new AuthService(JsonStore.open(workspace.paths()));

        assertEquals(LOGIN_FAILED, refusalMessage(() -> reopened.login("amy", TestWorkspace.PASSWORD)));
        assertEquals(new CurrentUser(amyId, "amy", Role.ANNOTATOR), reopened.login("amy", NEW_PASSWORD));
    }

    @Test
    void resetAnnotatorPassword_invalidPassword_refusedWithOldPasswordKept() {
        TestWorkspace workspace = workspace();
        long amyId = ClassificationWorkflow.single("yes").annotator("amy").seed(workspace).annotatorId("amy");
        AuthService owner = workspace.signInOwner();
        byte[] before = workspace.dataFileBytes();

        for (String password : INVALID_PASSWORDS) {
            assertEquals(PASSWORD_RULE, refusalMessage(() -> owner.resetAnnotatorPassword(amyId, password)));
        }

        assertArrayEquals(before, workspace.dataFileBytes());
    }

    private TestWorkspace workspace() {
        return TestWorkspace.create(temporary.resolve("accounts"));
    }

    /**
     * Seeds three items for k = 3: "first" and "second" have submitted, "partial" is in progress, item 0 is
     * resolved by majority and item 1 by the owner.
     */
    private static ClassificationWorkflow seedWork(TestWorkspace workspace) {
        return ClassificationWorkflow.single("yes", "no", "maybe").items(3)
                .assign("first", "yes", "yes", "no")
                .assign("second", "yes", "no", "yes")
                .assign("partial", "yes", "maybe")
                .majority(0, "yes")
                .adjudicated(1, "no")
                .seed(workspace);
    }

    /** Asserts that all four account-management methods refuse this caller with this message. */
    private static void assertAllRefused(AuthService caller, long annotatorId, String message) {
        assertEquals(message, refusalMessage(caller::listAccounts));
        assertEquals(message, refusalMessage(() -> caller.createAnnotator("newcomer", PASSWORD)));
        assertEquals(message, refusalMessage(() -> caller.deactivateAnnotator(annotatorId)));
        assertEquals(message, refusalMessage(() -> caller.resetAnnotatorPassword(annotatorId, NEW_PASSWORD)));
    }

    /** Asserts that deactivating this account and resetting its password are both refused with this message. */
    private static void assertChangeRefused(AuthService owner, long accountId, String message) {
        assertEquals(message, refusalMessage(() -> owner.deactivateAnnotator(accountId)));
        assertEquals(message, refusalMessage(() -> owner.resetAnnotatorPassword(accountId, NEW_PASSWORD)));
    }

    /** Asserts that the call is refused with an {@link AuthException}, and returns its message. */
    private static String refusalMessage(Executable call) {
        return assertThrows(AuthException.class, call).getMessage();
    }

    /**
     * Asserts that the data file, parsed as JSON, differs from {@code before} only in these fields of this
     * account, and that each of them changed.
     */
    private static void assertOnlyAccountChanged(byte[] before, TestWorkspace workspace, long accountId,
            String... fields) {
        JsonNode expected = JSON.readTree(before);
        JsonNode actual = JSON.readTree(workspace.dataFileBytes());
        ObjectNode expectedAccount = account(expected, accountId);
        ObjectNode actualAccount = account(actual, accountId);
        for (String field : fields) {
            assertNotEquals(expectedAccount.get(field), actualAccount.get(field), field);
            expectedAccount.set(field, actualAccount.get(field));
        }
        assertEquals(expected, actual);
    }

    private static ObjectNode account(JsonNode snapshot, long accountId) {
        JsonNode users = snapshot.get("users");
        for (int index = 0; index < users.size(); index++) {
            if (users.get(index).get("id").asLong() == accountId) {
                return (ObjectNode) users.get(index);
            }
        }
        throw new AssertionError("The data file has no account " + accountId);
    }

    private static String dataFileText(TestWorkspace workspace) {
        return new String(workspace.dataFileBytes(), StandardCharsets.UTF_8);
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
