# View basic annotation progress

Status: Awaiting human verification.

## Original request

- The last part of the owner's "start what you can" in this session: [#18] followed [#17], stacked on its branch.

## Follow-ups, corrections, and reflection

- Most of [#18] already existed, because [#12], [#13] and [#17] had built the counts, status and transitions. The work was the missing remaining count and one source for the progress wording on both screens. Checking what earlier issues already delivered kept this change small.
- The plan was implemented before approval, and the work stays uncommitted under the delivery rule.

## Agent responses and outcomes

- Used `write-plan`, `implement-feature`, `write-test`, `review`, `maintain-docs` and `log`. The plan is `plans/annotation-progress.md`, on branch `feat/annotation-progress` stacked on `feat/submit-and-next`.
- Added `AssignmentProgress.remaining()` and `position()` and `ProgressText`, and showed the progress line in the queue.

## Verification

- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 383 JUnit tests, 3 of them new, with 3 existing case-sensitivity tests skipped on macOS.
- The transition test checks that the home screen and the queue report identical progress at every step, and that another annotator's submission changes nothing.
- Not verified: the running app. The plan lists two human checks.
- The owner approved the plan and delivery on 26 September 2026.
- Pending: human acceptance, teammate review and CI.

[#12]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/12
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#18]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/18
