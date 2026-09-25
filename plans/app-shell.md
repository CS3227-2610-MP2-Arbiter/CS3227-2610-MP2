# App shell and role routing

**Issue:** [#5](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5)

**Branch:** `feat/app-shell` (from `main`)

**Status:** Approved by Whimsyturtle on 23 September 2026, with a My Splits placeholder for annotators. Implementation, local checks, and adjudicator GUI acceptance pass; PR review remains pending.

## Goal and boundary

Replace #7's signed-in placeholder with the shared JavaFX shell and provide a registration path for the screens in #12 and #24. The queue, project list, other feature screens, business rules, and #8's UI kit/error convention remain with their own issues. Keep the existing workspace setup, login, and one-Stage lifetime.

## Proposed changes

- Add a small route definition and registry in `arbiter.ui.shared`. A route supplies a stable ID, title, allowed role, and JavaFX content factory. Registering a route makes it available to navigation without adding a branch to a central role or route switch. Reject duplicate IDs and reject navigation to a route outside the current user's role before calling its factory. Keep registration explicit; this app does not need a plugin loader or framework.
- Add a shell view in `arbiter.ui.shared` with a top bar showing Arbiter, the signed-in user, and Logout; role-filtered navigation; and one content area that swaps the selected route's node. Keep the Stage and Scene created by `Arbiter`. Give the window a usable initial size and minimum size.
- Have `AuthScreen` hand a successful login to `Arbiter`, which constructs the shell with the authenticated role. Logout clears `AuthService`'s session and returns to the login screen in the same Stage. Preserve first-owner setup and login error behavior.
- Register a minimal My Splits placeholder for annotators and project-list placeholder for adjudicators as their initial destinations. #12 and #24 replace these placeholders with their feature screens. The placeholders contain no feature behavior or data access.
- Keep role packages separate when their screens arrive; role-specific content is registered through the shared route API, without one role package importing the other.

## Verification

| #5 acceptance criterion | Planned evidence |
| --- | --- |
| Shell renders with top bar and swappable content | Review the JavaFX composition; manually sign in and switch between two registered test routes where practical, then confirm Logout returns to login in the same window. |
| Role routing and cross-role denial | Focused JUnit tests for route lookup/navigation with both roles, denied cross-role IDs, and factory not invoked on denial; manually check the visible navigation for each role. |
| Screen added by registration alone | A focused test registers another route without changing routing logic and verifies it appears for its role and can be selected. |
| Build | Use JDK 25 to run focused tests, then `.\gradlew.bat check shadowJar` and inspect JUnit and Checkstyle results. |

## Risks and decisions

- #5 originally said an annotator lands on the queue, while #12 defines My Splits as the landing screen and #13 owns the queue. Whimsyturtle chose a My Splits placeholder on 23 September 2026, and #5's issue summary was updated to reflect it.
- The shell can restrict UI routes, but each future service must still enforce its own authorization; the route tests do not substitute for the blindness tests described in the architecture.

## Implementation and verification

- Added `ScreenRoute`, `ScreenRegistry`, and `AppShell` in `arbiter.ui.shared`. `AuthScreen` now hands successful login to `Arbiter`, which registers role-specific placeholders and returns to login through the existing Stage after Logout.
- The test subagent added `ScreenRegistryTest` for registration order, duplicate IDs, added routes, both cross-role denial directions, and unknown IDs. The independent reviewer found no correctness defect, but noted that visible content swapping and Logout still need a GUI check.
- JDK 25 focused route tests passed. `.\gradlew.bat check shadowJar --offline --no-daemon --console=plain` passed after correcting Checkstyle formatting in the initial runs. The final Gradle run reported `test UP-TO-DATE` because the complete test task had passed in the prior run; both Checkstyle tasks and the release jar passed.
- The computer-use skill prohibits automating a login dialog. Whimsyturtle confirmed on 23 September 2026 that adjudicator sign-in shows the Projects placeholder under the top bar and Logout returns to login in the same window. Before #31, the normal UI can create only the adjudicator; the role-filtering JUnit tests cover both roles.
- Updated `docs/UserGuide.md` to identify #5's destinations as placeholders until #12 and #24.
