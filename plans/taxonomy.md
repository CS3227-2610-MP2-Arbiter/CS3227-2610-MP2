# Taxonomy

**Issue:** [#26] - Configure single-label or scale taxonomy
**Branch:** `feat/taxonomy`, from `main` after [#32] (PR [#77]) merged, and brought up to `main` after PR #79. `AnnotationServiceTest`'s one-label fixtures then gained a second label, since a project now needs two before its first assignment.
**Status:** Approved by the owner on 26 September 2026. Implemented, tested and reviewed on 26 September 2026, the review numbering a new label one past the highest stored position so it goes last even in a hand-edited file. Accepted by the owner on 26 September 2026, with every human check passing, and the User Guide and Glossary updated.

What the feature does is in [#26], and the rules it relies on are rules 3, 4 and 5 in [UserFlows](../docs/UserFlows.md#3-rules-both-tracks-share). This plan records how the code will meet them and what is decided.

## Goal and scope

On the project page, let the adjudicator set up a SINGLE project's labels or a SCALE project's range before the first assignment, and refuse a first assignment whose taxonomy is not ready.

Non-goals: everything under "Out" in [#26], and scale resolution, which [#27] owns.

## Decided

Decided by the owner on 26 September 2026:

- **Taxonomy** on the project page opens an on-page view with Back, like Assign. SINGLE shows a labels table with Up and Down (disabled at the ends), Edit and Delete, and one form (key, optional description, Save and Cancel) for adding and editing. SCALE shows min and max fields with Save. Neither kind shows the other's controls.
- The project's first assignment is refused unless SINGLE has at least two labels, or SCALE has a saved range with min < max. Later assignments skip this.
- A label key follows the project name rule ([#24]), is unique within its project ignoring case, and may change only its own case.
- A SCALE range is whole numbers from −10 to 10, with min < max.
- The `Label` Javadoc now, and the Glossary during Accept, say "A SINGLE project's labels, or a SCALE project's range, form its *taxonomy*". The `Label.key` Javadoc says "fixed once the project is frozen (rule 3)" instead of "stable".

Assumptions shown to the owner, with no objection:

- An empty taxonomy is allowed during setup; only the first assignment checks it.
- Keys are trimmed; a blank description is stored as null and any other is trimmed.
- No label limit. A new label goes last, and order is renumbered 1 to n after a move or delete.
- The range can be saved again any number of times before the freeze, and cannot be cleared.
- Delete asks "Delete label ⟨key⟩?". A label an answer or resolution uses is refused with a message, and nothing cascades.
- After the freeze the view is read-only with a hint, and the service rejects every write regardless of the screen.
- `JsonIntegrity` also rejects duplicate keys in a project (ignoring case) and a stored range that is half set, has min ≥ max or is out of bounds, as [#32] did for rule 19.

## Current state

- The model and repositories already hold labels and the range, and `LabelRepository.deleteById` already refuses a label an answer or resolution uses. Nothing writes either.
- `FirstAssignment` checks the freeze, but only throws for a project; `AssignmentService` checks only the split.
- `ClassificationWorkflow` seeds one-label projects with assignees and `scale(3, 3)`, states the app will no longer reach.

## Proposed changes

- **`CorpusService`**, which the architecture context gives the taxonomy, gains a read returning the kind, labels in order, range and whether the project is frozen, and writes to add, edit, move, and delete a label and to save the range. Each write runs in one action that requires the adjudicator, the right kind and no first assignment. It also gains a static check of whether a taxonomy is ready, shared with `AssignmentService` as `parseCount` is.
- **`ProjectService`** shares its name check so keys follow it.
- **`FirstAssignment`** gains a boolean project check, which `requireNotReached` then uses.
- **`AssignmentService`** check and assign run the readiness check when the project has had no assignment.
- **`TaxonomySettings`** holds the scale bounds as constants, the one place both the service and `JsonIntegrity` can reach.
- **`JsonIntegrity`** gains the checks above.
- **`ProjectPage`** gains **Taxonomy**, and a new `TaxonomyView` beside `AssignForm` shows the view, with key and range errors inline and other failures through `ProjectPage.commit`.
- **`ClassificationWorkflow`** refuses a range the app cannot save, and assignees on a project with fewer than two labels; affected fixtures gain a second label or a wider range.
- **Documentation**, in the Accept step: the User Guide says how to set up the taxonomy and drops its note that this is not released, and the Glossary follows decision 2.

## Deviations

- The view's Save shows every `ProjectException` inline, including a freeze or missing-record rejection, because no read-only check separates them from key and range errors; Up, Down and Delete use `ProjectPage.commit`.
- The readiness check is `CorpusService.requireTaxonomyReady`, which throws with a message for the project's kind instead of returning a boolean.

## Risks

- **Tables cut long text off,** so the owner decided on 26 September 2026 that the Key and Description columns wrap, through a new `Components.wrappingColumn`. After review, the owner also decided that the labels table does not sort, and that wrapped labels in `Components` keep their full height, so a table shrinks instead. That also fixes `ProjectPage`, and zheng-jj needs to review it as a shared change.
- **The page needs JavaFX, which CI lacks,** so it is covered by human checks.

## Verification

Automated coverage goes in a new `CorpusServiceTaxonomyTest`, with more cases in `AssignmentServiceTest` and `JsonStoreIntegrityTest`. Every new method rejects a signed-out or annotator caller, and every rejection leaves the data file unchanged.

| Acceptance criterion | How it is verified |
| --- | --- |
| SINGLE labels can be created, edited, reordered and deleted before the first assignment | Test: add appends; edit changes key and description; moves swap and renumber, and a move past an end is rejected; delete renumbers; trimming and null description as assumed; label writes on a SCALE project are rejected. Human: each action in the view, with Up and Down disabled at the ends |
| SCALE range is valid, inclusive and set before the first assignment | Test: a range within the bounds saves and can be saved again; blank, `abc`, `2.5`, non-ASCII digits, min = max, min > max and each side out of bounds are rejected; range writes on a SINGLE project are rejected. Human: each kind shows only its own controls |
| Keys are unique and the taxonomy is valid before assignment | Test: keys breaking the name rule and duplicates ignoring case are rejected on add and edit, while a case-only change of a label's own key saves; check and assign refuse a first assignment with zero or one label or no range, allow one with two labels or a saved range, and skip this once frozen; `JsonIntegrity` rejects duplicate keys and each bad range. Human: assigning a one-label project shows the error inline |
| Deletion needs confirmation and never cascades | Test: deleting a label a seeded resolution uses is refused. Human: Cancel keeps the label, and confirming deletes it |
| After the first assignment, every write is rejected, after restart and from stale screens | Test: after an assign, every write is rejected, also on a reopened store. Human: the view is read-only with its hint |
| SCALE resolves under [#27] without tolerance or end labels | Not verifiable here: this stores only min and max, and [#27] tests resolution |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar`, including `UiConventionTest` and `AnnotatorBlindnessTest` over the new view |
| No text is cut off (`review` skill) | Human: a 100-character key and a long description show in full in the table, the form and the delete dialog |

[#24]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/24
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#32]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/32
[#77]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/pull/77
