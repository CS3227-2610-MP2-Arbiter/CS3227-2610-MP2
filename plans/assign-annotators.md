# Assign annotators

**Issue:** [#32] - Assign annotators to splits
**Branch:** `feat/assign-annotators`, from `main` after [#31] (PR [#76]) merged
**Status:** Approved by the owner on 26 September 2026. Implemented as proposed, tested and reviewed; `check shadowJar` passes. The owner passed acceptance testing on 26 September 2026. Deviation: once *k* is saved, check and assign ignore the typed *k*, since the form shows it read-only. Acceptance testing found the assign confirmation cut off, so `Dialogs` in `arbiter.ui.shared` now lets every dialog grow to fit its message, and the assign form's annotator lines wrap. To catch this earlier, `context/architecture.md` now says no text is cut off, and the `review` skill checks it and asks for a longest-text human check. After a later review, the owner approved on 26 September 2026 that every form scrolls rather than squeezing its text, *k* is prefilled with 1 when no annotator is active, and `JsonIntegrity` rejects stored assignments that break rule 19; the owner repeated the affected acceptance tests and passed them. The User Guide is updated.

What the feature does is in [#32], and the rules it relies on are rules 3, 14 and 19 in [UserFlows](../docs/UserFlows.md#3-rules-both-tracks-share). This plan records how the code will meet them and what is decided.

## Goal and scope

On the project page, let the adjudicator assign active annotators to a split's free places, setting *k* with its first assignment, after seeing each annotator's load and confirming.

Non-goals: everything under "Out" in [#32], and checking the taxonomy, which [#26] owns along with its freeze.

## Decided

Decided by the owner on 26 September 2026:

- *k*'s range and prefill are as in [#32]. *k* is a field on the assign form, saved by the split's first assignment and read-only afterwards; there is no separate "Set *k*" action.
- The splits table gains **Assign** and an "Annotators: n of k" column. **Assign** opens an on-page form, like Reset password on Accounts, with *k*, a checklist of annotators showing their load, and the current assignees, then asks to confirm.
- Load is an annotator's unfinished (not `SUBMITTED`) assignments across all projects, and the total files in them.
- A read-only check runs before the confirmation, as Generate splits previews, so errors show inline and the dialog names a valid *k*; the write repeats every check.

Assumptions accepted by the owner:

- One commit assigns one or more annotators, all or nothing, never more than the free places; the first need not fill all *k*.
- The checklist offers only active annotators not already on the split, in account creation order, never the owner. Current assignees are listed with their account status, disabled ones included.
- *k* is parsed as files per split is ([#28]), with an inline error.
- Fewer active annotators than free places is allowed, leaving the split partly filled. With no active annotator to offer, the form points to Accounts.
- The confirmation names the split, *k* and the annotators, and says assignments cannot be removed, moved or replaced and a disabled account keeps its place. On the project's first assignment it adds that files and taxonomy freeze and the project can no longer be deleted.
- One commit's assignments share one assigned time. Places and deletion otherwise follow rules 14 and 19.

Also accepted by the owner: **Assign** is disabled on a full split, and the column reads "None" before the first assignment.

## Current state

- The model and repositories already hold assignments and *k*. Nothing creates an assignment, and the `AssignmentService` the architecture context names does not exist.
- `FirstAssignment` derives the project freeze and split lock from a split having any assignment, and every setup change already checks it, so saving an assignment is the freeze.
- `ProjectPage` takes the freeze from the project list's cached count, assuming assignments cannot change on that page, and `SplitSummary` says only whether a split is assigned.

## Proposed changes

- **A new `AssignmentService`**, as the architecture context names it, built like `CorpusService`: each method requires the signed-in adjudicator and rejects with `ProjectException`.
  - Options returns a split's saved *k*, the active annotator count, its assignees with their status, and the annotators it may offer with their load, in records like `SplitSummary`.
  - Check validates a request without writing and returns the *k* to confirm, as the split preview does.
  - Assign repeats the checks in one write, saves *k* if `FirstAssignment` shows the split has no assignment yet, and saves the new assignments.
  - The default *k* ([Glossary](../docs/Glossary.md)) is a service constant the form prefills.
- **`SplitSummary`** gains its assignment count and *k*, and `CorpusService` shares its count parsing so *k* follows the same rules.
- **`ProjectPage`** gains the column, **Assign**, the form and the confirmation, handling errors as its existing actions do, and derives the freeze from the splits it loads.
- **`ProjectsScreen` and `Arbiter`** pass the new service through.
- **Documentation**, in the Accept step: the User Guide says how to assign and drops its note that assignment is not released.

The model and repositories do not change.

## Risks

- **Without [#26], a project freezes with whatever taxonomy it has,** so a project assigned during acceptance testing cannot later get labels. Test on a throwaway workspace.
- **No annotator screen exists yet ([#12]),** so ownership is checked through the repository in tests only.
- **The page needs JavaFX, which CI lacks,** so it is covered by human checks.

## Verification

Automated coverage goes in a new `AssignmentServiceTest`, with accounts made through `AuthService` and other assignments seeded with `ClassificationWorkflow`. Every new method rejects a signed-out or annotator caller.

| Acceptance criterion | How it is verified |
| --- | --- |
| *k* is 1 to the active annotators, prefilled with 2, fixed afterwards | Test: blank, `0`, `-1`, `2.5`, `abc`, non-ASCII digits and one above the active count are rejected with nothing stored; the default is 2, or 1 with one active annotator; a later assign keeps the first *k*. Human: a bad *k* shows inline with no dialog, and *k* is read-only after the first assignment |
| Up to *k* distinct active annotators, with load and consequences shown first | Test: options offers only the annotators "Decided" names, with their load across projects; one assign can add several, and a partial first commit leaves places to fill later. Human: the checklist shows load, and the dialog says what "Decided" lists |
| Duplicates and a (*k* + 1)th assignment are rejected atomically | Test: a current assignee, one annotator picked twice, no pick, a disabled, unknown or owner account, and too many picks are each rejected with the data file unchanged, even beside valid picks |
| Cancel creates nothing; afterwards nothing is unassigned, deleted, transferred or replaced | Human: Cancel on the form or dialog changes nothing. Review: no method removes an assignment or changes its split or annotator |
| Disabled and `NOT_STARTED` assignments keep their places | Test: a deactivated assignee is still listed, the free places are unchanged, and a replacement beyond *k* is rejected |
| The freeze is persisted atomically and survives restart and deactivation | Test: after assign, registering, unregistering and deleting the split or project are rejected, also on a reopened store and after deactivation; a rejected assign freezes nothing. Human: after the first assignment, Add files, Unregister and both Deletes are disabled |
| Listed only for its original annotator | Test: each new assignment is listed for its annotator and no other, also after deactivation |
| New assignments start `NOT_STARTED` | Test: each is `NOT_STARTED`, sharing one assigned time |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar`, including `UiConventionTest` and `AnnotatorBlindnessTest` over the changed page |

[#12]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/12
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
[#28]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/28
[#31]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/31
[#32]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/32
[#76]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/pull/76
