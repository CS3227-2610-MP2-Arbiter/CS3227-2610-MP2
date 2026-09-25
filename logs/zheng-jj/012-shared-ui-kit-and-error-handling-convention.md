# Shared UI kit and error-handling convention

Status: Human verified.

## Original request

- After PR #69 (#11) and PR #67 (#5) merged, the owner asked which issue came next. The agent recommended [#8], because #5 was its only blocker and it blocks #12, #13, #24 and #31 on both tracks. The owner replied "sure, work through everything u can".

## Follow-ups, corrections, and reflection

- `main` had just gained a delivery rule in `context/swe.md` (aad6e82): commits, pushes and pull request changes need explicit human approval, and approving the plan or implementation does not grant it. "Work through everything" was read as permission to plan, implement, test and review, not to deliver. The work is therefore uncommitted, with a ready pull request draft.
- The plan was implemented before the owner approved it, on the same instruction. On 25 September 2026 the owner approved the plan, accepted the 1 MiB × 3 log size, and gave delivery approval for the three commits, the push, the pull request and the review request.
- Two findings shaped the design. The workspace already creates a `logs/` folder described as holding application logs, which nothing wrote to. And CI's Linux runner has no display, so the logic lives in plain classes testable without JavaFX, while the JavaFX pieces are held to the convention by ArchUnit rules.
- Deciding which exceptions reach the user by package (declared in `arbiter.*`) rather than by a list, or a new base class, means future service exceptions need no registration and existing exception hierarchies stay untouched.

- Whimsyturtle's review of PR #71 raised four points, all valid. Two were defects: an uncaught error during layout or animation showed no dialog, because JavaFX forbids `showAndWait` there, and a startup failure exited with no dialog and no workspace log. Two were one-home violations: the Developer Guide restated what `ErrorMessages` and `DiagnosticLog` guarantee. The lesson: a handler that reports errors needs its own failure modes checked, and the design doc should explain why, not restate what the Javadoc already says.

## Agent responses and outcomes

- Used `write-plan`, `implement-feature`, `write-test`, `review`, `maintain-docs` and `log`. `clarify-requirements` was skipped because #8's criteria were already agreed. `create-pull-request` waited at a draft until the owner approved delivery.
- Branch `feat/ui-kit` from `main` at 6a03158. The plan is `plans/ui-kit.md`.
- Added `Styles` with `arbiter.css`, `Components`, `Dialogs`, `ErrorMessages` and `DiagnosticLog` in `arbiter.ui.shared`, and moved `Arbiter`, `AppShell`, `AuthScreen` and `WorkspaceSetupDialog` onto them.
- `Arbiter` applies the stylesheet, attaches the workspace log while holding the lock, and installs uncaught-exception handlers.
- Documented the convention in `docs/DeveloperGuide.md` and `context/architecture.md`, and the log's location in `docs/UserGuide.md`.

## Verification

- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 208 JUnit tests, 18 new, none skipped, both Checkstyle tasks, and the release jar, which includes the stylesheet.
- A mutation run made all six targeted tests fail, and every mutation was then reverted:
  - A temporary class set an inline style, built an `Alert`, printed to `System.out` and used `java.util.logging`, failing all four convention rules.
  - A stray stylesheet rule failed the stylesheet check.
  - A login message temporarily including the password failed the password check.
- Not verified: the GUI. The seven human acceptance checks are listed in the plan.
- After the review fixes: 209 JUnit tests, 19 new, none skipped. A mutation that recorded the uncaught failure only after the dialog call failed `DialogsTest`.
- CI on the first revision (0efebea) passed on every platform.
- The owner approved delivery of the fix on 25 September 2026.
- CI on the fix (2fcef2c) passed on every platform. Whimsyturtle approved PR #71, and it merged on 25 September 2026.
- Pending: the eight human acceptance checks in `plans/ui-kit.md`.
- The owner marked this summary human verified on 25 September 2026.

[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
