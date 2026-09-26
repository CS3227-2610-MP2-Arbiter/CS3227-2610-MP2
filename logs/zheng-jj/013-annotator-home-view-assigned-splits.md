# Annotator home: view assigned splits

Status: Awaiting human verification.

## Original request

- After Whimsyturtle completed [#32], the owner asked to pull and "work through the issues". [#12] was the only one of the owner's issues it unblocked; [#13] follows from it, and [#14] also waits on [#26].

## Follow-ups, corrections, and reflection

- The delivery rule in `context/swe.md` needs explicit approval before any commit, push or pull request, and "work through the issues" did not name those. So the work stayed uncommitted until approval.
- The plan was implemented before the owner approved it, on the same instruction.
- The architecture already reserved `AnnotationService.forCurrentUser(...)` as the annotator's scoped read, and the #11 blindness test already trusted that name. Putting the new read there meant no trusted entry point had to be added, so the blindness rule did not change.
- Whimsyturtle's new rule that no text may be cut off shaped the screen: cards of wrapping labels in a scrolling page, not a table.

## Agent responses and outcomes

- Used `write-plan`, `implement-feature`, `write-test`, `review`, `maintain-docs` and `log`. `clarify-requirements` was skipped because #12's criteria were agreed. The plan is `plans/annotator-home.md`, on branch `feat/annotator-home` from `main` at 2448350.
- Added `AnnotationService` with `forCurrentUser()`, `AssignmentProgress`, `AuthService.requireAnnotator()`, `MySplitsScreen`, `Components.card` and `Components.scrollingPage`, and registered the screen for annotators.
- Updated `docs/UserGuide.md` with a "My splits" section.

## Verification

- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 342 JUnit tests, with 3 existing case-sensitivity tests skipped on macOS.
- `AnnotationServiceTest` adds 9 tests. A mutation that counted any annotator's answer on a file failed the isolation test, and it was reverted.
- Not verified: the screen in a running app. The plan lists four human checks.
- The owner approved the plan and delivery on 26 September 2026.
- Pending: human acceptance, teammate review and CI.

[#12]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/12
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
[#32]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/32
