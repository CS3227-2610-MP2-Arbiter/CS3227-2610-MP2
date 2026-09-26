# Blind forward-only text queue

**Issue:** [#13] - Blind forward-only text queue
**Branch:** `feat/text-queue`, stacked on `feat/annotator-home` ([#12])
**Status:** Plan approved by the owner on 26 September 2026, after it was implemented at their request to "work through the issues". Verified locally; human acceptance and teammate review are pending.

What the feature does is in [#13], and what each class means is in its Javadoc. This plan records what was decided and what is still open.

## Goal and scope

Open an unfinished assignment from My splits ([#12]) at its first file without the signed-in annotator's answer, in saved split order, and show that file's text and position. A finished assignment shows completion.

Non-goals, from [#13]: answer controls ([#14]), storing answers and moving forward ([#17]), drafts, Back, jumps, skips, reviews, timers and shortcuts.

## Proposed changes

- **`AnnotationService.forCurrentUser(assignmentId)`** returns a `QueueView`: the assignment's `AssignmentProgress` from [#12] and the current `QueueItem`, or none when every file is answered.
  - The position is derived from saved split order and stored answers each time, so a restart resumes at the first unanswered file and never restores an unsent choice (rule 18).
  - Another annotator's assignment gets the same refusal as a missing one, so the refusal cannot reveal what exists.
  - The file is read through `SourceResolver.resolve` ([#10]). A missing, unreadable or changed source becomes the `QueueItem`'s error message rather than an exception, so the queue still shows where the annotator is. [#17] will refuse an answer against it.
  - The service now takes `WorkspacePaths`, as `CorpusService` does.
- **`QueueView` and `QueueItem`** are records. A `QueueItem` holds the item identifier, stored path, and either the text or the source error.
- **`ItemView`** in `arbiter.ui.shared` shows one file's text, or its source error, from plain values. The architecture puts it there so the adjudicator's dispute screen ([#34]) reuses it rather than building a second one. The text is a wrapping label in a scroll pane, so long text scrolls instead of being cut off.
- **`QueueScreen`** in `arbiter.ui.annotator` shows the project and split, "File k of n", and the `ItemView`, with Back to My splits. A finished assignment shows a completion message instead. There is no control that moves between files.
- **`MySplitsScreen`** opens `QueueScreen` inside its own content area, as `ProjectsScreen` opens `ProjectPage`, replacing #12's placeholder dialog; Back reloads the list.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| Source errors returned in `QueueItem` | Throwing `SourceException` | The queue must still show its position and the error together, and [#17] needs to know the item cannot be answered |
| One refusal for missing and foreign assignments | Distinct messages | Distinct messages would tell an annotator which assignment identifiers belong to others |
| `ItemView` in `arbiter.ui.shared` | A queue-only text view | The architecture requires one `ItemView` for both roles |
| Position recomputed on every open | A cached cursor | Rule 18 derives position from saved order and submissions, and nothing about the position needs storing |

## Risks and open decisions

- **Nothing moves the queue forward until [#17].** Until then the queue shows one file, and a split's position changes only when its answers are stored.
- **A file's text is read when the queue opens,** not continuously. If the file changes afterwards, [#17]'s check at submission catches it.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| Shows only the first file without this annotator's answer, in saved order, with accurate position | `AnnotationServiceTest`: the current file, its text and "k of n", including when saved order differs from identifier order |
| Forward movement only after [#17] succeeds | Review: `QueueScreen` has no control that changes file |
| No Back, jump, skip, review, timer, shortcut, rationale or draft restoration | Review of `QueueScreen` |
| Restart resumes at the first unanswered file; answered files never reopen | `AnnotationServiceTest`: fresh sessions before and after an answer is stored |
| A finished assignment shows completion | `AnnotationServiceTest`: no current file once every file is answered. Human check of the completion message |
| Missing, unreadable or changed source shows [#10]'s error | `AnnotationServiceTest`: a changed file and a missing file each give the resolver's message and no text |
| Tests prove no cross-annotator leakage | `AnnotationServiceTest`: another annotator's answers do not move the position, and another annotator's assignment is refused exactly like a missing one. `AnnotatorBlindnessTest` walks `QueueScreen` and `ItemView` |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar` |

## Implementation and verification

- Added `AnnotationService.forCurrentUser(assignmentId)` with the `QueueView` and `QueueItem` records, `ItemView` in `arbiter.ui.shared`, and `QueueScreen` in `arbiter.ui.annotator`. `MySplitsScreen` now opens the queue in its own content area, and #12's placeholder dialog is gone. `AnnotationService` takes `WorkspacePaths` so it can resolve sources.
- The assignment's progress and its next file's record are read from one snapshot. The file itself is read afterwards, outside the store's action.
- The blindness test needed no change. It walks `QueueScreen` and `ItemView`, and `ItemView` takes only plain values.
- `AnnotationServiceTest` adds 9 queue tests: position and text, saved order, restart before and after an answer is stored, completion, another annotator's answers, refusing another annotator's assignment exactly like a missing one, a changed source, a missing source, and the adjudicator. Removing the ownership check and letting source errors escape made the refusal test and both source tests fail; both changes were reverted.
- `docs/UserGuide.md`'s "My splits" section now describes opening a split and unreadable files.
- After Whimsyturtle's review of PR #78, the queue uses the same two rules as the home screen: `AnnotationService.position` decides the next file, and the stored status decides whether the assignment is finished. A finished assignment shows completion even if a file lacks an answer, so it is never reopened. A new test covers that case, and it failed when the status check was removed.
- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 351 JUnit tests, with the same 3 existing case-sensitivity tests skipped on macOS.

### Human acceptance checks

1. Choose Start on a split: its first file's text shows with "File 1 of n", and a long file scrolls rather than being cut off.
2. Close Arbiter and reopen the split: the same file shows.
3. Edit that file in `media/`, then reopen the split: the error names the file and no text shows. Restoring the original bytes brings the text back.
4. Back to My splits returns to the list.

[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#12]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/12
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
