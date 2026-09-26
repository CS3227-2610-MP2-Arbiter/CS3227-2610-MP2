# Account management

**Issues:** [#31] - Manage annotator accounts, and [#23] - Reset an annotator password directly, delivered together
**Branch:** `feat/account-management`, from `main` after [#28] (PR [#75]) merged
**Status:** Approved by the owner on 25 September 2026. Implemented as proposed, tested and reviewed; `check shadowJar` passes. The owner passed acceptance testing on 26 September 2026; PR [#76].

What the features do is in [#31] and [#23], and the rules they rely on are rules 5, 12 and 19 in [UserFlows](../docs/UserFlows.md#3-rules-both-tracks-share). This plan records how the code will meet them and what is decided.

## Goal and scope

Give the adjudicator an Accounts screen that lists every account, creates annotators, deactivates them and resets an active annotator's password.

Non-goals: everything under "Out" in [#31] and [#23]. Counting a disabled annotator's assignment toward *k* (rule 19) belongs to [#32].

## Decided

Decided by the owner on 25 September 2026, beyond what the issues record:

- The adjudicator's navigation gains **Accounts** after Projects. Its table lists Username, Role and Status in creation order, owner first, with **Reset password** and **Deactivate** buttons that are disabled on the owner's row and on disabled rows.
- **New annotator** is a form with username, password and confirmation, like owner setup. Errors show inline, a username taken in any letter case is rejected, and the username keeps the case it was typed in.
- **Deactivate** asks for confirmation, saying that it is permanent and that the annotator's work and assignments are kept.
- **Reset password** is a form on the screen with the new password and its confirmation, both masked.
- Acting on an out-of-date row shows the service's error, and then the list reloads. On the reset form every refusal shows inline and **Cancel** reloads, with no `AuthException` subclass to tell a bad password from a refusal about the account; the screen reloads after every action and the workspace lock ([#61]) keeps other instances out.
- `TestAccounts` keeps building fixture accounts, because `ClassificationWorkflow` seeds everything, disabled accounts included, in one write without a signed-in owner. Its note about switching to the service once [#31] lands is dropped, with the matching clause in the [test harness plan](test-harness.md).

Assumptions accepted by the owner:

- Usernames are not stripped, as in owner setup, so surrounding spaces break the username rule rather than being removed.
- A disabled account's username stays taken.
- A reset ends with a success message naming the account, since the list does not change; creating and deactivating show their result in the reloaded list.

## Current state

- `AuthService` bootstraps the owner, signs accounts in and out, refusing a disabled account after its password is verified, and has `requireAdjudicator()` and `currentUser()`, which ends a session whose account is no longer active. Its username and password checks are private, and `PasswordHasher` is package-private.
- `UserRepository` saves, finds by identifier or case-insensitive username, lists all or by role, and counts active accounts by role; nothing deletes an account. `countActiveByRole` is used only by a repository test and is left for [#32]. `JsonIntegrity` already refuses two usernames equal in any case.
- Nothing lists, creates, deactivates or resets accounts. `ClassificationWorkflow` seeds annotators built by `TestAccounts`, with assignments, submissions and resolutions, and `disabled(...)` seeds a disabled one.
- `Arbiter.showShell` registers one adjudicator route, Projects.

## Proposed changes

- **`AuthService`** gains four methods. Each first calls `requireAdjudicator()`, and every rejection throws `AuthException` with nothing stored.
  - `List<AccountSummary> listAccounts()` returns every account in identifier order, which is creation order with the owner first, since bootstrap needs an empty workspace.
  - `AccountSummary createAnnotator(String username, String password)` applies owner setup's username and password checks and hashing, then in one write refuses a username taken in any letter case and saves an ACTIVE annotator with the username as typed.
  - `void deactivateAnnotator(long accountId)` in one write refuses a missing account, the owner or a disabled account, then sets the status to DISABLED and changes nothing else.
  - `void resetAnnotatorPassword(long accountId, String password)` applies the same password check and hashing, then in one write makes the same refusals and replaces only the hash and salt.
  - The class Javadoc covers accounts and password replacement.
- **An `AccountSummary` record** in `arbiter.service`, like `ProjectSummary`, carries an account's identifier, username, role and status, never its credentials.
- **`AccountsScreen`** in `arbiter.ui.adjudicator`, modelled on `ProjectsScreen`, shows the table, the New annotator and reset forms, and the Deactivate confirmation. The forms check that the two passwords match, as `AuthScreen` does, and show rejections inline with `ErrorMessages.of`. Deactivate shows a rejection with `Dialogs.showError` and reloads; a store failure is shown without reloading, as on the project list.
- **`Arbiter.showShell`** registers the Accounts route after Projects.
- **`AuthScreen`**'s username and password hints become public constants that the new forms reuse.
- **`TestAccounts`** and the test harness plan drop their note about switching to the service.
- **Documentation**, in the Accept step: the User Guide gains an Accounts section, and its getting-started steps link to it. The architecture context does not change, as it already gives these rules to `AuthService`.

The model, repositories, `JsonStore` and `JsonIntegrity` do not change.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| Account methods on `AuthService` | A new `AccountService` | The architecture context gives accounts and password replacement to `AuthService`, whose checks and hashing are private to it |
| Return `AccountSummary` | Return `User` | `User` carries the hash and salt, which [#23] says are never shown |
| Rejections are `AuthException` | A new exception type | Owner setup already reports the same checks this way, and the forms show them the same way |

## Risks

- **The app cannot yet assign work** ([#32]), so preservation is tested on data seeded with `ClassificationWorkflow`, and [#32] must still count a disabled annotator's assignment toward *k*.
- **Refusals of an out-of-date row are reachable only in tests,** since the screen reloads after every action.
- **The screen needs JavaFX, which CI lacks,** so it is covered by human checks.

## Verification

Automated coverage goes in `AuthServiceTest`, with work seeded by `ClassificationWorkflow` into a `TestWorkspace` and callers signed in through it. "Changes only X" means the data file, parsed as JSON before and after, differs only in that field of that account. `AuthBootstrapPublicationTest` becomes `AuthPublicationTest` and gains the reset case.

| Acceptance criterion | How it is verified |
| --- | --- |
| [#31]: accounts are listed with role and ACTIVE/DISABLED status | Test: the owner first, then annotators in creation order, each with role and status, a disabled one included. Human: the columns and order, and both buttons disabled on the owner's and disabled rows |
| [#31]: only the authenticated adjudicator can create ACTIVE annotators and deactivate annotators | Test: signed-out and annotator callers are refused by all four methods with the data file unchanged; a created account is an ACTIVE annotator that can sign in. Human: an annotator's navigation has no Accounts entry |
| [#31]: usernames are unique case-insensitively, and passwords follow [#7]'s validation and hashing without plaintext storage or logging | Test: a username taken in another case, by the owner or by a disabled account, is refused; owner setup's invalid usernames and passwords are refused with nothing stored; the stored username keeps its case, the hash verifies, and neither the data file nor any refusal message contains the password. Human: a mismatch, an invalid entry and a taken username show inline, and Cancel creates nothing |
| [#31]: deactivation preserves assignments, submissions, resolutions and attribution, and prevents login | Test: on a project with a submitted and an in-progress assignment and a majority and an adjudicated resolution, deactivating the in-progress annotator changes only their status; they are then refused with "Account is disabled", and a session already signed in as them ends, including after reopening the store. Human: the dialog's wording, Cancel changes nothing, and confirming shows DISABLED and blocks their login |
| [#31]: disabling does not free a place or transfer or redistribute unfinished work | Test: the same deactivation leaves every assignment, split, *k* and item as it was and creates nothing. [#32] owns counting the place |
| [#31]: deactivation is permanent | Test: deactivating or resetting a disabled account is refused and it stays DISABLED. Review: nothing sets an existing account ACTIVE |
| [#31]: the owner cannot be disabled, deleted, replaced or demoted, and no second adjudicator can be created | Test: deactivating or resetting the owner is refused with the data file unchanged, and after creating annotators there is still exactly one adjudicator. Review: nothing takes or changes a role or deletes an account, and owner setup already refuses a second owner |
| [#23]: only the authenticated adjudicator can reset an ACTIVE annotator's password; annotators, the owner and disabled annotators are refused | Test: signed-out and annotator callers, the owner, a disabled annotator and a missing identifier are refused, with the data file unchanged and the old password still working |
| [#23]: the replacement uses [#7]'s validation and salted hashing and commits atomically | Test: owner setup's invalid passwords are refused with nothing stored; the new salt differs and the new hash verifies; in `AuthPublicationTest`, a failed publication leaves the old password working after reopening |
| [#23]: after restart, the old password fails and the replacement works | Test: a new service on the reopened store refuses the old password with the generic failure and accepts the new one. Human: the same after restarting Arbiter |
| [#23]: reset preserves identity, status, assignments, submissions, resolutions and attribution, and cannot reactivate | Test: on the seeded project, a reset changes only that account's hash and salt; a disabled account is refused, as above |
| [#23]: credentials and hashes are not exposed or logged | Test: neither the data file nor any refusal message contains the new password. Review: `AccountSummary` has no credential field, and nothing logs a password. Human: both fields are masked, and the workspace log holds no password after the checks above |
| [#23]: no email, code or token, self-service or owner-recovery flow is added | Review: the change adds none, and the login screen and annotator shell are unchanged |
| `./gradlew check` passes (both issues) | JDK 25 `./gradlew check shadowJar`, including `UiConventionTest` and `AnnotatorBlindnessTest` |

## Implementation

- Owner setup and annotator creation now build the account with one private helper in `AuthService`.
- The test harness plan's open question about `TestAccounts` was the same note, so it now says it is settled here.
- `AuthBootstrapPublicationTest` became `AuthPublicationTest`, with a failed-reset case, as planned.
- Review found only low-severity points, all fixed: rule citations in `AuthService`'s Javadoc, a shared "Passwords do not match" constant and two redundant test lines.
- A second review's fixes replace `AuthScreen`'s public hint constants: each form's username and password hint is now `AuthService`'s refusal message, so each rule is worded once, and the password fields and Cancel button are shared `ui.shared` helpers. A disabled account is refused with "Account is disabled" everywhere.

[#7]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/7
[#23]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/23
[#28]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/28
[#31]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/31
[#32]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/32
[#61]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/61
[#75]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/pull/75
[#76]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/pull/76
