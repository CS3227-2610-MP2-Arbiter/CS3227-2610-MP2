# View basic annotation progress

**Issue:** [#18] - View basic annotation progress
**Branch:** `feat/annotation-progress`, stacked on `feat/submit-and-next` ([#17])
**Status:** Plan approved by the owner on 26 September 2026, after it was implemented at their request to "start what you can". Verified locally; human acceptance and teammate review are pending.

What the feature does is in [#18], and what each class means is in its Javadoc. This plan records what was decided.

## Goal and scope

Show each of the annotator's assignments with truthful submitted, total and remaining counts and its status, on My splits ([#12]) and in its queue ([#13]), from the same numbers.

Non-goals, from [#18]: timing, averages, lifetime or project totals, charts, agreement figures, history and anyone else's progress.

## What already existed

[#12] shows submitted/total and the stored status on each card, and [#13] shows "File k of n". [#17] keeps the status, and its tests cover the transitions. The remaining gap was an explicit remaining count, and making sure both screens word progress from one source.

## Proposed changes

- **`AssignmentProgress.remaining()` and `position()`** derive the remaining files and the queue's 1-based position from the same `submitted` and `total` both screens already use.
- **`ProgressText`** in `arbiter.ui.annotator` words the line "k of n files submitted, r remaining" once, for the card and the queue. It lives in the annotator package because only annotator screens show it.
- **`QueueScreen`** shows that line under "File k of n", and on the completion screen. Each submission redraws from stored state, so the counts change immediately after a successful submission and never for an unsent choice.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| Truthful submitted, total and remaining counts plus status | `AnnotationServiceTest`: a split submitted file by file shows 0/3, 1/2, 2/1 and 3/0 with `NOT_STARTED`, `IN_PROGRESS` and `SUBMITTED` |
| A successful submission updates the counts immediately; an unsent selection does not | The same test reads progress after each `submit`. [#14]'s test shows that opening the queue writes nothing |
| The queue shows its position using the same counts | That test checks the home screen's and the queue's `AssignmentProgress` are equal at every step |
| The final submission marks SUBMITTED without confirmation | The same test, and [#17]'s tests |
| Only the signed-in annotator's progress is accessible | Another annotator's submission leaves the counts unchanged, and [#12]'s isolation tests |
| No timer, chart, lifetime statistic, agreement figure or history | Review: `AssignmentProgress` has no such field |
| Tests cover count and status transitions and blindness | The tests above, and `ProgressTextTest` |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar` |

## Implementation and verification

- Added `AssignmentProgress.remaining()` and `position()`, `ProgressText`, and the progress line in `QueueScreen`. My splits uses `ProgressText` too.
- `AnnotationServiceTest` adds 2 tests, and `ProgressTextTest` adds 1.
- `docs/UserGuide.md` mentions the remaining count.
- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 383 JUnit tests, with 3 existing case-sensitivity tests skipped on macOS.

### Human acceptance checks

These need a seeded workspace until [#26].

1. My splits and the split's screen show the same "k of n files submitted, r remaining".
2. After Submit & next, both update; choosing an answer without submitting changes neither.

[#12]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/12
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#18]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/18
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
