# Annotator home: view assigned splits

**Issue:** [#12] - Annotator home: view assigned splits
**Branch:** `feat/annotator-home` (from `main` after [#32] merged)
**Status:** Plan approved by the owner on 26 September 2026, after it was implemented at their request to "work through the issues". Verified locally; human acceptance and teammate review are pending.

What the feature does is in [#12], and what each class means is in its Javadoc. This plan records what was decided and what is still open.

## Goal and scope

Replace the annotator's "My splits" placeholder ([#5]) with a list of exactly the signed-in annotator's assignments, each showing its project, split, status and submitted/total files, opening the split's queue ([#13]) at its first unanswered file.

Non-goals, from [#12]: the queue and answer controls ([#13], [#14]), statistics, charts, agreement figures, and anything about another annotator.

## Proposed changes

- **`AnnotationService`** is created in `arbiter.service`, as `context/architecture.md` already names it. Its first method, `forCurrentUser()`, returns the signed-in annotator's `AssignmentProgress` records. It is the annotator-scoped read path that the blindness test ([#11]) already trusts by name, so no trusted entry point is added.
  - It requires a signed-in annotator through a new `AuthService.requireAnnotator()`, the counterpart of `requireAdjudicator()`.
  - It reads only `assignments().listByAnnotator(me)` and this annotator's own answers, through `findByItemAndAnnotator(item, me)`, so another annotator's work cannot change what it returns.
- **`AssignmentProgress`** is a record: assignment identifier, project name, split name, stored status, submitted count, total files, and the next file to answer, which is the first file in saved split order without this annotator's answer (null when every file is answered). [#13] opens the queue at that file.
- **Status is shown as stored.** [#17] keeps it (`NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED`); this screen neither derives nor changes it. Counts come from the annotator's own answers.
- **Order:** unfinished assignments first, then submitted ones, each in the order they were assigned (assigned time, then identifier). This is deterministic, and it puts the work the annotator can still do at the top.
- **`MySplitsScreen`** in `arbiter.ui.annotator` shows one card per assignment:
  - Each card shows the project and split, the status and "k of n files submitted", all in wrapping `Components` labels (the "no text is cut off" rule).
  - An unfinished card has a Start or Continue button. A submitted card says it is finished and offers no button, so its files are never reopened.
  - With no assignments, the screen shows an empty state. A failed load shows the error convention's dialog ([#8]).
  - A new `Styles.CARD` class styles the cards.
- **Opening a card** hands its `AssignmentProgress` to a callback that `Arbiter` wires. Until [#13] lands, the callback shows a placeholder naming the file the queue will open at. [#13] replaces it with the queue.
- **`Arbiter`** creates `AnnotationService` beside the other services and registers `MySplitsScreen` for the annotator role in place of the placeholder.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| `AnnotationService.forCurrentUser()` | A method on `AssignmentService` | `AssignmentService` is adjudicator-only and reads every annotator's assignments. The architecture reserves `forCurrentUser` for the annotator's scoped reads, and the blindness test already trusts it |
| Counts from the annotator's own answers | Counting through `listBySplit` or `listByAssignment` | Per-item reads keyed on the signed-in annotator cannot pick up anyone else's work, whatever identifier is passed |
| Stored status | Deriving status from counts | [#17] owns status changes; deriving it here would give two sources of truth |
| Cards | A table like the adjudicator's project list | [#12] asks for cards, and a card's text wraps, where a table cell truncates |

## Risks and open decisions

- **Order** is a product choice: unfinished first, then by assignment time. [#12] asks only that it be deterministic.
- **Until [#13] merges, opening a card shows a placeholder.** If both land together, the placeholder never ships.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| My Splits lists exactly the signed-in annotator's assignments | `AnnotationServiceTest`: two annotators sharing a split, and one with a second split, each see only their own assignments |
| Each card shows project, split, status and submitted/total counts | `AnnotationServiceTest` checks the fields, including counts that ignore another annotator's answers on the same files. Human check of the cards, with the longest realistic project and split names |
| Opening an unfinished assignment resumes at its first unanswered file in saved split order | `AnnotationServiceTest`: the next file follows saved split order when it differs from item identifier order, and skips answered files |
| A SUBMITTED assignment shows completion without reopening | The next file is null once every file is answered. Human check: the card shows finished and has no button |
| Statuses are the three stored values; #17 sets SUBMITTED | The screen shows `AssignmentProgress.status`, which is read, never written |
| No reward, statistic, agreement figure or other annotator's state | Review of `AssignmentProgress`'s fields. `AnnotatorBlindnessTest` walks `MySplitsScreen` |
| The empty state renders for an annotator with no assignments | `AnnotationServiceTest`: an empty list. Human check of the empty state |
| Tests prove A cannot see B's assignments or progress | The isolation and count tests above; the adjudicator, and anyone signed out, is rejected |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar` |

## Implementation and verification

- Added `AnnotationService.forCurrentUser()`, `AssignmentProgress`, `AuthService.requireAnnotator()`, `MySplitsScreen`, and two kit pieces it needed: `Components.card` (with `Styles.CARD`) and `Components.scrollingPage`, so a long list scrolls instead of clipping. `Arbiter` registers the screen for annotators. Opening a card shows a placeholder naming the file the queue will open at, until [#13].
- The blindness test needed no change. It walks `MySplitsScreen`, which reads only through the already-trusted `AnnotationService.forCurrentUser`, and still passes.
- `AnnotationServiceTest` (9 tests) covers isolation between annotators, counts that ignore another annotator's answers, saved split order when it differs from identifier order, finished assignments, the empty list, order, and rejection of the adjudicator, a signed-out session and an account disabled after sign-in. Counting any answer on a file, instead of only the signed-in annotator's, made the isolation test fail.
- `docs/UserGuide.md` gains a "My splits" section and no longer calls the annotator's home a placeholder.
- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 342 JUnit tests. The 3 skipped are existing case-sensitivity tests that skip on macOS's case-insensitive file system.

### Human acceptance checks

1. Sign in as an annotator with no assignments: My splits shows the empty state.
2. Assign that annotator to two splits (one in a project with a long name), then sign in again. Both cards show project, split, "Not started" and "0 of n files submitted", with the long name wrapping, not cut off.
3. Sign in as a second annotator on one of those splits: only their own card appears.
4. Choose Start: the placeholder names file 1 of n. (After [#13], the queue opens there.)

[#5]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5
[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#12]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/12
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#32]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/32
