# Blind forward-only text queue

Status: Awaiting human verification.

## Original request

- Part of the owner's request to "work through the issues" after [#32] merged: [#13] followed [#12] in the same session, stacked on its branch.

## Follow-ups, corrections, and reflection

- As with [#12], the plan was implemented before approval, and the work stayed uncommitted under the delivery rule.
- [#13] cannot move forward without [#17]'s Submit & next, so it shows one file and derives the position from stored answers each time. The tests stand in for a submission by inserting an answer directly.
- Two choices keep the queue blind and honest. Another annotator's assignment gets the same refusal as a missing one, so an identifier cannot be probed. A source error is returned alongside the position rather than thrown, so the annotator still sees where they are.
- The architecture already required one shared `ItemView`, so the file display went into `arbiter.ui.shared` with plain-value inputs, ready for the adjudicator's dispute screen ([#34]).

- Whimsyturtle's review of #12's PR (#78) applied here too. The queue now takes its next file from the same helper as the home screen, and decides "finished" from the stored status rather than from the absence of a next file, so both screens agree.

- Whimsyturtle's review of #79 found four problems:
  - two definitions of "finished"
  - the current file stored twice
  - file paths shown to annotators, which could hint at labels
  - annotator wording inside the shared `ItemView`

  All four were fixed by giving each fact one owner. Finished is the stored status. The current file lives only in the queue. Annotators get a failure reason, never a path. `ItemView` shows only text. The path issue is the notable one: blindness also covers hints in the data itself, not just other annotators' answers.

## Agent responses and outcomes

- Used `write-plan`, `implement-feature`, `write-test`, `review`, `maintain-docs` and `log`. The plan is `plans/text-queue.md`, on branch `feat/text-queue` stacked on `feat/annotator-home`.
- Added `AnnotationService.forCurrentUser(assignmentId)`, `QueueView`, `QueueItem`, `ItemView` and `QueueScreen`. `MySplitsScreen` now opens the queue, replacing #12's placeholder.
- Updated the "My splits" section of `docs/UserGuide.md`.

## Verification

- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 351 JUnit tests, with 3 existing case-sensitivity tests skipped on macOS.
- `AnnotationServiceTest` adds 9 queue tests. Removing the ownership check and letting source errors escape made 3 of them fail, and both changes were reverted.
- Not verified: the screen in a running app. The plan lists four human checks.
- The owner approved the plan and delivery on 26 September 2026.
- The owner approved delivery of the review fixes on 26 September 2026.
- The owner approved delivery of the second review's fixes on 26 September 2026.
- Pending: human acceptance, teammate review and CI.

[#12]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/12
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#32]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/32
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
