# Split generation

**Issue:** [#28] - Generate count-based work batches
**Branch:** `feat/split-generation`, from `main` after [#25] (PR [#74]) merged
**Status:** Approved by the owner on 25 September 2026. Implemented, tested and reviewed; `check shadowJar` passes. The owner passed acceptance testing on 25 September 2026; PR [#75]. After review, the owner had the confirmation dialog summarise the sizes and the split UI say "files". Deviations: the count is stripped of surrounding whitespace before the digit check, as project names are; generate takes the previewed sizes and refuses if the available items no longer split into them, which for the same count is the count comparison; and every preview rejection, including an empty pool, shows inline under the count.

What the feature does is in [#28], and the rules it relies on are rules 3, 5, 7 and 14 in [UserFlows](../docs/UserFlows.md#3-rules-both-tracks-share). This plan records how the code will meet them and what is decided.

## Goal and scope

On the project page, let the adjudicator split the items not yet in a split by an items-per-split count after confirming the sizes, see the splits and each item's place in one, and delete a split before its first assignment.

Non-goals: everything under "Out" in [#28], including *k* and assignment, which [#32] owns.

## Decided

Decided by the owner on 25 September 2026:

- The preview is a confirmation dialog showing only the split sizes. The seed is picked at confirm, and confirm is refused if the available-item count changed since the preview.
- The seed is random and recorded; there is no seed field. Determinism is tested at service level.
- New splits are named "Split n", continuing after the project's highest existing number.
- The project page gains a splits table (name, item count, **Delete**) and a column on the items table giving each item's split and position.

Assumptions accepted by the owner:

- Locking is per split (rule 14), so a never-assigned split may be deleted and regenerated after the project's first assignment; with the corpus frozen (rule 3), only its items are then available.
- Available items are taken in registration order before the shuffle, and the chunks become the next splits in shuffled order.
- The count must be a positive whole number in ASCII digits, or the page shows an inline error. A count equal to the pool makes one split, and one above it, however large, is rejected (owner, after review), and an empty pool is reported with nothing created.
- Generation leaves *k* unset for [#32]. No split is ever empty: generation never creates one, and unregistering a split's last item deletes the split too (owner, after review).
- A split's lock derives from its assignments, as in `FirstAssignment` and `RepositorySession.deleteSplit`.
- The page says "split", Delete asks for confirmation, and the seed is stored but not shown.
- Unregistering an item from a split already works from [#25]; this task adds regression tests on generated splits.

This plan also assumes that positions count from 1 in saved order, so a gap left by an unregister does not show, and that deleting the highest-numbered split frees its number.

## Current state

- `Split` holds the name, seed, requested count, *k*, creation time and an `assigned` flag that only test fixtures set. `JsonIntegrity` requires all of them except *k* and the flag. `SplitItem` holds an item's position.
- The split repositories can save, list a split's memberships in order, find an item's membership and delete. Split delete also removes the memberships and refuses a split with assignments; item delete already removes the item's membership.
- `CorpusService` lists, registers and unregisters items, and `ProjectPage` shows them. Nothing creates or deletes a split, and `FirstAssignment` checks only a whole project.
- Tests seed splits and assignments through `Records`, `ClassificationWorkflow` and `TestWorkspace`.

## Proposed changes

- **`CorpusService`** gains four methods. Each first requires the signed-in adjudicator, and rejections throw `ProjectException`.
  - List splits returns the project's splits in creation order, each with its item ids in saved order and whether it has an assignment.
  - Preview reads the count text and returns the sizes the available items would split into. It rejects a bad count, a missing project and an empty pool.
  - Generate repeats those checks in one write and refuses if the available count differs from the previewed one. It then picks a seed, shuffles the available items and saves each chunk as the next "Split n", with the seed, the requested count, one creation time and memberships numbered from 1.
  - Delete split, in one write, rejects a split that no longer exists or has any assignment, whatever its status or annotator, and then uses the existing split delete, returning its items to the pool.
  - The shuffle and chunking is one package-private static method using `Collections.shuffle` with `new Random(seed)`, so a test can check the saved splits against the recorded seed.
- **A `SplitSummary` record** in `arbiter.service`, like `ProjectSummary`, carries what list splits returns.
- **`Split`** loses the `assigned` flag, since nothing reads it and it could disagree with the assignments, and the fixtures stop setting it. Existing snapshots still open, as `JsonStore` ignores unknown properties. The `seed` Javadoc stops implying a user-chosen seed, and `SplitRepository.deleteById`'s Javadoc names the memberships it removes.
- **`ProjectPage`** gains the count field and **Generate splits** with an inline error, the confirmation dialog, the splits table with **Delete** disabled for an assigned split, and a **Split** column that is blank for an available item. It handles errors and reloads as its existing actions do.
- **Architecture context:** services check one split's lock (rule 14) with `FirstAssignment` inside the write, as they do a project's.
- **Documentation**, in the Accept step: the User Guide says how to generate and delete splits.

`Arbiter` and `JsonIntegrity` do not change, and the repositories only lose the unused `SplitItemRepository.deleteBySplit`, which skipped the split delete's assignment check.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| Split methods on `CorpusService` | A new `SplitService` | The architecture context gives splits to `CorpusService` |
| A static allocation method tested with a known seed | A seed source injected into the service | The constructor and `Arbiter` stay unchanged |
| `Collections.shuffle` with `java.util.Random` | A hand-written shuffle | Both algorithms are fixed by their Javadoc, so a recorded seed gives the same order on any JDK |
| Remove `Split.assigned` | Keep it for [#32] to set | Whether a split is assigned keeps one home |

## Risks

- **The preview check compares counts,** so swapping one available item for another would pass. Nothing can do that while the modal dialog is open, and the workspace lock ([#61]) keeps other instances out.
- **Removing `Split.assigned` changes a shared model class** and fixtures the annotator track uses, and [#32] then records a split's lock only by saving its assignment.
- **The app cannot yet create assignments or disable accounts** ([#32], [#31]), so the lock and later assignment are covered by automated tests on seeded data only.
- **The page needs JavaFX, which CI lacks,** so it is covered by human checks.

## Verification

Automated coverage goes in `CorpusServiceTest`, with items registered through the service or seeded with `Records`, and assignments seeded with `Records` or `ClassificationWorkflow`.

| Acceptance criterion | How it is verified |
| --- | --- |
| Count-based generation is the only allocation mode; zero, negative and non-integer counts are rejected | Test: blank, `0`, `-1`, `2.5`, `abc` and non-ASCII digits are each rejected by preview and generate, with nothing stored. Human: the page offers only the count, and a bad count shows an inline error and no dialog |
| 120 available items at 50 per split produce 50, 50 and 20, with every input exactly once; an empty pool creates none | Test: 120 items give 50, 50 and 20, every available item is in exactly one new split, numbered from 1; a count equal to the pool makes one split and one above it is rejected; a later generation takes only items not yet in a split; an empty pool is rejected with the data file unchanged. Human: 5 items at 2 per split give 2, 2 and 1 |
| Identical stable inputs, count and seed produce identical membership/order | Test: the allocation method returns the same result for the same ids, count and seed, and the saved splits equal its result for the available ids in registration order, the requested count and the recorded seed |
| Generation is confirmed after a preview of the split sizes; the saved splits have those sizes and record the seed and requested count, and a change in available items since the preview is refused | Test: the saved sizes equal the preview; each new split records the same seed, the requested count and no *k*, and is named after the highest existing number; registering or unregistering an item after the preview makes generate refuse with nothing stored. Human: the dialog shows the sizes, Cancel creates nothing, and confirming adds the splits and fills the Split column |
| Restart and later assignment use saved membership/order without reshuffling | Test: after reopening the store, and after an assignment is seeded on one split, list splits returns the same membership and order, and a further generation leaves existing splits unchanged. Review: only generate runs the allocation. Seeded data only until [#32] |
| Before first assignment, a split may be deleted and setup-time item unregister removes its membership | Test: deleting a generated split removes it and its memberships and makes its items available, leaving the rest unchanged; unregistering an item from a generated split removes its membership, and unregistering its last item deletes the split; after another split's assignment, a never-assigned split can still be deleted and its items regenerated. Human: Delete asks for confirmation, Cancel keeps the split, and confirming removes it and clears its items' Split column |
| From a split's first assignment, its definition cannot change or be deleted, including after account deactivation or restart | Test: with an assignment that is not started, or whose annotator is disabled, deleting the split is rejected with the data file unchanged, including through a new service on a reopened store and after a list read taken before the assignment existed; its items stay impossible to unregister. A signed-out or annotator caller is rejected by every new method. Seeded data only until [#31] and [#32], after which a human checks that Delete is disabled |
| No reward, manual allocation, retired-item, flag-exclusion, completion-seal or post-assignment refill behavior is added | Review: the change adds none of these, and the page cannot pick or move items or add items to an existing split |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar`, including `UiConventionTest` and `AnnotatorBlindnessTest` over the changed page |

[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#28]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/28
[#31]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/31
[#32]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/32
[#61]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/61
[#74]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/pull/74
[#75]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/pull/75
