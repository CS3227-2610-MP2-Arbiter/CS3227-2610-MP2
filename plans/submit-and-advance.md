# Atomically submit one item and advance

**Issue:** [#17] - Atomically submit one item and advance
**Branch:** `feat/submit-and-next`, stacked on `feat/annotation-ui` ([#14])
**Status:** Plan approved by the owner on 26 September 2026, after it was implemented at their request to "start what you can". At their direction, the handoff to automatic resolution ([#27]) is left out, so nothing here depends on the adjudicator track. Verified locally; human acceptance and teammate review are pending.

What the feature does is in [#17], and what each class means is in its Javadoc. This plan records what was decided and what is still open.

## Goal and scope

When the annotator chooses **Submit & next**, validate their answer and, in one committed action, store it as an immutable submission with its time and update the assignment's status. The queue then shows the next file. Automatic resolution of an item's *k*th answer ([#27]) belongs in the same action; [#27] adds it there.

Non-goals, from [#17]: drafts, autosave, flags, Back, skipping, batch submit, correction, withdrawal and revision history.

## Proposed changes

- **`AnnotationService.submit(assignmentId, itemId, answer)`** does all of this inside one `JsonStore.write` action, and any check that fails throws before anything is stored (rule 2):
  - the caller is a signed-in annotator and the assignment is theirs (a foreign or missing one is refused alike, as in [#13])
  - the assignment is not finished
  - the item is their next file in saved split order (`position`), so answers can only go forward (rule 18)
  - the item has no answer from them yet (one immutable answer per annotator and item)
  - the answer follows the project's `Taxonomy` ([#14]): a label of the project for SINGLE, or a rating in range for SCALE
  - the file's source still matches its recorded hash (rule 21)
- **Storing:** one `Annotation` with the label or rating and the submission time. The assignment becomes `SUBMITTED` when every file has an answer, otherwise `IN_PROGRESS`, and `submit` returns the queue's new `QueueView`.
- **Retry and double-click cannot duplicate.** A second submission for the same file fails the "no answer yet" and "next file" checks and stores nothing. The screen disables **Submit & next** while it submits, and on any refusal it reloads the queue, which shows where the annotator really is.
- **No handoff to [#27].** An earlier draft added a `SubmissionHook` for [#27] to plug into. The owner directed that it be left out, because it was a contract the adjudicator track had not agreed. [#27] will add resolution to `submit`'s action itself.
- **Blindness:** `submit` joins `forCurrentUser` in the blindness test's trusted entry points, because it reads the annotator's answers to place the queue. It returns only their own `QueueView`, which the tests below check. `context/architecture.md` anticipated this addition.
- **`QueueScreen`:** **Submit & next** calls `submit` with the file on screen and the editor's answer, then shows the returned position. A refused submission shows the error convention's dialog ([#8]) and reloads, and the on-screen choice stays editable if the queue did not move.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| Refusing a second submission | Treating a repeat as a silent success | A repeat may carry a different answer. Refusing it and reloading shows the truth, and the disabled button prevents the common double-click |
| The screen names the item it showed | The service picking the next item | Naming the item makes a stale screen fail loudly instead of storing an answer against a different file |

## Open decisions

- **Adding `submit` to the trusted entry points** is a reviewed change to the blindness test.
- **[#17]'s criterion "performs any #27 resolution" is left to [#27]**, which will add resolution inside `submit`'s action.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| No annotation or draft exists before Submit & next | [#14]'s test: opening the queue writes nothing |
| A valid action records one answer and its time, updates status, performs resolution and advances in one transaction | `AnnotationServiceTest`: the stored answer and time, `IN_PROGRESS` then `SUBMITTED`, and the next file returned, all from one `JsonStore.write` action. Resolution is left to [#27] |
| Invalid input or a failed transaction records nothing and does not advance | Each refusal (foreign assignment, finished assignment, wrong file, answered file, bad label, bad rating, changed source) leaves `arbiter.json` byte-for-byte unchanged. A failing action publishes nothing, which the JSON store's transaction tests cover |
| Double-click, retry or restart cannot duplicate an answer or advancement | A repeated submission is refused and stores nothing. A fresh session resumes at the next file |
| Submitted answers cannot be changed or deleted, including from a stale screen | No method changes or deletes one, and a stale screen's submission is refused |
| The final item makes the assignment SUBMITTED | `AnnotationServiceTest` |
| Restart resumes at the first unanswered file | [#13]'s restart test, plus a fresh session after submitting |
| No export-format code on the annotator path | Review |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar` |

## Implementation and verification

- Added `AnnotationService.submit`, `Taxonomy.accepts(Answer)` and a message-and-cause constructor on `ProjectException`. The `SubmissionHook` of the first draft was removed at the owner's direction, along with its two tests. `QueueScreen` now submits and redraws from the stored state, and the placeholder notice is gone. `submit` is added to `AnnotatorBlindnessTest`'s trusted entry points, and `context/architecture.md`'s blindness rules name it.
- A file whose source changed is refused with a message that does not name it, because the resolver's own message includes the path (the same concern as #79's review).
- On a `ProjectException`, `QueueScreen` reloads to show where the queue really is. On an `AuthException` or `JsonStoreException`, nothing was stored, so the choice stays on screen to submit again.
- `AnnotationServiceTest` adds 11 submission tests, and `TaxonomyTest` adds 2 for `accepts(Answer)`. The submission tests cover:
  - storing and the next file, and the last file finishing the assignment
  - scale ratings
  - a repeat, a wrong file and a finished assignment, each storing nothing
  - a foreign assignment refused like a missing one
  - answers outside the taxonomy, including another project's label
  - a changed source refused without its name
  - restart
  - the adjudicator
- A mutation run made the targeted tests fail: dropping both duplicate guards, skipping the status update, and removing `submit` from the trusted list, which also failed the blindness test.
- `docs/UserGuide.md` describes Submit & next.
- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 380 JUnit tests, with 3 existing case-sensitivity tests skipped on macOS.

- After [#26] merged (PR #83), which refuses a SINGLE project's first assignment until it has two labels, the tests here seed two-label projects. The "no labels yet" test deletes both labels after assignment. No production code changed.
- After #80's review, `submit` switches over the sealed `Answer` (`LabelChoice` or `Rating`) and checks it with `TaxonomySummary.accepts`, which replaced `Taxonomy`. The two `accepts(Answer)` tests this plan added now live in `TaxonomySummaryTest`, leaving 412 JUnit tests.
### Human acceptance checks

These need a seeded workspace until [#26].

1. Choose a label and **Submit & next**: the next file shows with "File 2 of n", and restarting Arbiter keeps it there.
2. Submit the last file: the split shows it is finished, and My splits shows it as finished with no button.
3. Double-click **Submit & next**: only one answer is stored, and the queue moves one file.
A stale screen's submission cannot be produced in the running app, because the workspace lock allows one instance and the queue redraws from stored state after every action. The tests cover it at the service boundary.

[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
