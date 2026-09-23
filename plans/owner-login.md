# Workspace owner and login

**Issue:** [#7](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/7)

**Branch:** `feat/owner-login`

**Status:** Approved by the owner on 23 September 2026 and revised to require ASCII alphanumeric passwords during review. Local verification passed; human acceptance remains pending.

## Goal and boundary

Implement #7 on the merged JSON store from #6. This supplies the authentication foundation for #5, #31, and #23. Account management, password replacement, and full role navigation remain with those issues.

## Decisions for review

- Use one small JDK-only helper with two operations: hash a new password and verify a login. It uses `PBKDF2WithHmacSHA256` and a fresh `SecureRandom` salt, following the option in `context/architecture.md`. Store the result in the existing `User.passwordHash` and `User.passwordSalt` fields. Fix the parameters at the [OWASP PBKDF2 recommendation](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html) of 600,000 iterations, a 16-byte salt, and a 256-bit derived key; compare derived hashes without an early-exit byte comparison. Add no dependency, authentication framework, configurable crypto options, tokens, or reset flow.
- The owner chose a minimum password length of 8 characters without a required character mix on 23 September 2026, then restricted passwords to ASCII during review to avoid Unicode handling and finally to letters and digits only. Do not trim or fold case. Use the same validation and hashing entry point for later #31/#23 operations; see #7 for the resulting password rule.
- Accept usernames matching `[A-Za-z0-9._-]{1,64}` and compare them without case sensitivity. Display the rule beside account entry. The existing JSON store enforces case-insensitive uniqueness at commit.
- `AuthService` owns bootstrap, login, logout, and the current user's ID in memory. Its public session view contains only ID, username, and role. Service authorization checks reload the account's current role and status from the store, so a disabled account cannot keep using a stale session in later services.
- During review, the owner chose to show disabled status after password verification; the login behavior is recorded in #7.
- Run the bootstrap eligibility check and owner insert inside one `JsonStore.write` action. A fresh snapshot has no accounts and has never allocated an ID; after a successful bootstrap, a missing, disabled, or extra owner is an error rather than permission to bootstrap again. The store has no user-delete operation; the initial-snapshot check also detects the obvious manually damaged owner-only snapshot whose ID counter was retained. No new snapshot schema field or migration is proposed.
- Keep the UI for #7 small: use the existing workspace wizard to collect the first owner's credentials, then show a shared login screen and a signed-in placeholder with Logout. #5 can replace the placeholder with its role-based shell. The owner chose this boundary on 23 September 2026.

## Verification plan

| #7 acceptance criterion | Evidence planned |
| --- | --- |
| 1: first owner and retry | Integration tests on temporary JSON workspaces check one ACTIVE owner, validation/publish failure, and successful retry. |
| 2: no repeat or invalid owner | Tests reopen the store, race two bootstrap callers, and seed invalid owner states; exactly one bootstrap can succeed. |
| 3: no self-registration or role selection | Review the service API and the visible setup/login controls; #31 remains the only later creation path. |
| 4: login, logout, and status | Tests cover both roles, disabled annotator login with correct and incorrect passwords, in-memory logout, and login after process-style service reconstruction. Manual acceptance covers the UI flow. |
| 5: usernames and generic failure | Validation boundary tests and JSON integration tests check ASCII input, case-insensitive lookup/duplicates, and one response for unknown name and wrong password. |
| 6: salted storage | Unit tests check password verification, wrong passwords, different salts for the same password, and no plaintext in the saved snapshot. |
| 7–8: tests and build | Run focused auth tests, then `.\gradlew.bat check` with JDK 25. |

## Risks and open decisions

- #5 owns the final shared shell and role routing; the #7 screen needs a small handoff point for that later replacement.
- Issue #7 says usernames must be ASCII but does not define the allowed punctuation or length. The owner approved the simple regex above with this plan.
- #6 serializes actions inside one process; [#61](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/61) owns the workspace lock across app instances. The #7 concurrency test covers competing calls in one process; cross-process writer exclusion awaits #61.
- The earlier storage-guide contradiction recorded in [#6's plan](json-persistence.md) was corrected during documentation follow-up; human review remains pending.

## Implementation and verification

- Added `AuthService` for owner setup, login/logout, session status, and the adjudicator authorization boundary. `RepositorySession.isPristine()` distinguishes a new JSON snapshot from an initialized workspace with missing owner records without a schema change.
- Added one JDK-only PBKDF2 helper and a minimal shared owner setup/login/logout screen. The owner setup screen confirms the password because owner recovery is not available under #7.
- Added focused auth and password tests, including same-process competing bootstrap calls, invalid owner state, case-insensitive login, disabled accounts, stale-session denial, password validation boundaries, and retry after an injected JSON publication failure.
- JDK 25 `.\gradlew.bat check shadowJar --offline --no-daemon` passed on 23 September 2026: JUnit, both Checkstyle tasks, and the release jar. An earlier incremental compile failed to locate existing `SnapshotPublisher.java`; `clean compileJava` passed. The first full check found three formatting violations, which were corrected before the passing rerun.
- Human acceptance remains: create a workspace and its owner, log out/in, reopen and log in again, and verify the visible error and retry for invalid setup credentials. The annotator UI login can be checked with a seeded account until #31 adds account creation.
- During owner acceptance testing, the username validation error was truncated with an ellipsis. `AuthScreen` now wraps form labels so the full message remains visible; JDK 25 `.\gradlew.bat check shadowJar --offline --no-daemon` passed again. The owner still needs to confirm the visual retest.
- The owner then restricted password input to printable ASCII. Setup and login now reject other characters, and the UI states the rule. The updated tests cover Unicode and control-character rejection, ASCII boundary cases, and generic login errors. `.\gradlew.bat check shadowJar --offline --no-daemon` passed after one test indentation correction.
- After the owner refined disabled-login feedback, the focused auth tests passed and `.\gradlew.bat check shadowJar --offline --no-daemon` passed. The first full run found two Checkstyle line-wrap violations in the new assertions; both were corrected before the passing rerun.
- The owner then chose ASCII letters and digits only for passwords. Validation uses one compiled pattern, and the setup screen states the new rule. Focused auth tests and `.\gradlew.bat check shadowJar --offline --no-daemon` passed.

## Review order

1. Password storage: `AuthException`, `PasswordHasher`, and `PasswordHasherTest`. Committed as `e1f9ef3` after the owner's first review.
2. Authentication behavior and storage boundary: `CurrentUser`, `AuthService`, `RepositorySession`, `AuthServiceTest`, and `AuthBootstrapPublicationTest`. Committed as `7b0f319` after the owner's review.
3. Minimal JavaFX flow: `AuthScreen` and `Arbiter`. Committed as `037976e` after the owner's UI review.
4. Task record: this plan and `logs/Whimsyturtle/014-plan-owner-login.md`. Staged for the owner's review.
