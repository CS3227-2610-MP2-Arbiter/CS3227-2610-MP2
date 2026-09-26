# Manage annotator accounts and reset passwords

Status: Human verified.

## Original request

- The human asked the agent to start issue #31 following `context/swe.md`, keep it simple, use the repo's skills as needed and ask about important decisions.

## Follow-ups, corrections, and reflection

- Clarifying #31 found that it included #23's password reset although #23 was a separate issue blocked by #31, and that it did not say whether a disabled account could be reactivated. The human decided:
  - #23 is delivered with #31 on one branch.
  - Deactivation is permanent, with no reactivation.
  - A password can be reset only for an active annotator, rather than any annotator as the agent had proposed.
  - The adjudicator gets an Accounts screen with the agent's proposed table, forms and confirmations.
- The human approved `plans/account-management.md`. They chose to show every reset refusal inline, with Cancel reloading the list, and to keep `TestAccounts` rather than switch fixtures to the new service.
- The human asked for this log before acceptance testing.
- The human then asked for a thorough second review. It found no correctness defects, only low-severity points, and the human chose nine of them to fix. Those left include sharing the invalid-input test lists and adding failed-save tests for creating and deactivating.
- The human asked the agent to help stage the branch in groups, then passed acceptance testing. They asked whether the empty workspace log was a bug; it was not, since only failures shown in an error dialog are logged. They then asked the agent to update the docs and open the pull request, leaving commits to them.

## Agent responses and outcomes

- The agent updated #31 and #23's bodies with the decisions and created `feat/account-management` from `main`.
- The auth service can list accounts, create annotators, deactivate them and reset an active annotator's password. It refuses signed-out and annotator callers, the owner and disabled accounts. A new Accounts screen follows Projects in the adjudicator's navigation.
- `TestAccounts` and `plans/test-harness.md` dropped their note about switching fixtures to the service.
- Review found only low-severity points, all fixed: rule citations in a Javadoc, a duplicated "Passwords do not match" message and two redundant test lines.
- The second review's fixes:
  - Two misleading comments on the Accounts and Projects screens are corrected.
  - The password fields and Cancel button are shared across forms.
  - The forms' username and password hints are now the service's refusal messages, so their wording changed on owner setup and New annotator.
  - A disabled account is refused with "Account is disabled" everywhere.
  - A new test covers the shortest and longest usernames.
- The agent grouped the changes into service and tests, shared form helpers, the Accounts screen, and plans and log.
- The User Guide gains an Accounts section, linked from its getting-started steps. The other guides needed no change.
- The agent ticked #31 and #23's criteria and opened PR #76.

## Verification

- `AuthServiceTest` gains 15 tests. `AuthBootstrapPublicationTest` became `AuthPublicationTest` with a failed-reset case. They found no production defects.
- After the second review's fixes, `.\gradlew.bat check shadowJar` passed on JDK 25: 311 tests, no failures, and six skips from existing tests that Windows cannot run.
- CI passed on Linux, macOS and Windows for the pushed branch.
- The human passed every acceptance scenario by hand, including the owner setup and New project forms the fixes touched. Keeping a disabled annotator's assignment is covered only by tests on seeded data until #32.
