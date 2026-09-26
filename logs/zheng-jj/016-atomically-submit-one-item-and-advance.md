# Atomically submit one item and advance

Status: Awaiting human verification.

## Original request

- Part of the owner's "start what you can" after [#13] merged: [#17] followed [#14] in the same session, stacked on its branch.

## Follow-ups, corrections, and reflection

- [#17] must run automatic resolution ([#27]) in the same transaction as the kth answer, but [#27] belongs to Whimsyturtle and does not exist yet. The first draft added a `SubmissionHook` for [#27] to plug into and flagged it as a shared contract. The owner then said to ignore [#27] and ship only what needs nothing from the adjudicator track, so the hook and its two tests were removed. [#27] will add resolution to `submit`'s action itself.
- `submit` reads the annotator's answers to place the queue, so it joined `forCurrentUser` as a trusted entry point in the blindness test, as `context/architecture.md` anticipated. Without that, the walk rightly flags it.
- The lesson from #79's review carried over: a changed source is refused with a message that does not name the file, because the resolver's message includes the path.
- The plan was implemented before approval, and the work stays uncommitted under the delivery rule.

## Agent responses and outcomes

- Used `write-plan`, `implement-feature`, `write-test`, `review`, `maintain-docs` and `log`. The plan is `plans/submit-and-advance.md`, on branch `feat/submit-and-next` stacked on `feat/annotation-ui`.
- Added `AnnotationService.submit`, `Taxonomy.accepts(Answer)` and a cause constructor on `ProjectException`. `QueueScreen` submits and redraws.
- Updated the blindness test's trusted entry points, `context/architecture.md` and `docs/UserGuide.md`.

## Verification

- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 380 JUnit tests, 13 of them new, with 3 existing case-sensitivity tests skipped on macOS.
- A mutation run failed the targeted tests: dropping both duplicate guards, skipping the status update, and removing `submit` from the trusted list. All were reverted.
- Not verified: submission in a running app, which needs [#26] or a seeded workspace. The plan lists three human checks.
- The owner approved the plan and delivery on 26 September 2026, without the hook.
- Pending: human acceptance, teammate review and CI.

[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
