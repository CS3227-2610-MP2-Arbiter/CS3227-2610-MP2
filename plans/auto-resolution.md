# Automatic resolution

**Issue:** [#27] - Automatically resolve classification answers
**Branch:** `feat/auto-resolution`, from `main` after PR #82 merged
**Status:** Approved by the owner on 26 September 2026. Implemented with `ResolutionServiceTest`; a second review's findings are fixed and `check shadowJar` passes. The guides are updated and the owner's acceptance checks passed on 27 September 2026. Teammate review on PR #85 is pending.

What the feature does is in [#27], and the rule it applies is rule 10 in [UserFlows](../docs/UserFlows.md#3-rules-both-tracks-share). This plan records how the code will meet it and what is decided.

## Goal and scope

When [#17] stores an item's *k*th submitted answer, store its automatic resolution in the same action, if rule 10 gives one.

Non-goals: everything under "Out" in [#27].

## Decided

Decided by the owner on 26 September 2026:

1. A new **`ResolutionService`**, as the architecture context names it, gains a small static method that takes the open `RepositorySession`, which `AnnotationService.submit` calls inside its `store.write` action right after inserting the answer, as `CorpusService.taxonomyOf` is shared. [#34] extends the class later. No hook interface, which [the submit plan](submit-and-advance.md) rejected.
2. Resolution does not re-check for duplicate or out-of-range answers, because [#17] refuses them and *k* and the taxonomy are frozen. It counts the item's submitted answers (`AnnotationRepository.listByItem`) against *k* (`Split.annotationsPerItem`).
3. No backfill: an item that reached *k* answers before this feature stays unresolved.
4. No new UI. The only visible effect is the project list's existing Unresolved count.
5. An item that already has a resolution is left alone; nothing is overwritten.
6. Values: SINGLE resolves to `MAJORITY` with the label when 2 × its count > *k*, and otherwise stays unresolved for [#34]. SCALE resolves to `AUTO_SCALE` with the unrounded mean as a `double`. `decidedAt` is the *k*th answer's `submittedAt`, and `decidedByUserId` is null.
7. Wording: the `Resolution` Javadoc says "k submitted answers" instead of "valid", and `submit`'s Javadoc says resolution happens in its action instead of "belongs" there.

## Current state

- `Resolution`, `ResolutionMethod` and `ResolutionRepository` exist, and `ProjectService` already counts items without a resolution as Unresolved. Nothing writes a resolution yet.
- `AnnotationService.submit` stores the answer and the assignment's status in one `JsonStore.write` action ([#17]).

## Proposed changes

- **`ResolutionService`** (new, `arbiter.service`), static methods only for now:
  - `resolveAutomatically(session, answer)` reads the resolution of the answer's item, its split's *k*, its project's kind and its answers. If there is no resolution, it saves what `decide` returns for the answer's `submittedAt`; otherwise it does nothing.
  - `decide(kind, k, answers, decidedAt)` applies decision 6 and returns the resolution or empty, including when the answers do not number *k*. It needs no store, so it is unit tested directly.
- **`AnnotationService.submit`** passes the answer it stored to `resolveAutomatically` after `annotations().insert`, and its Javadoc follows decision 7. The class Javadoc names `submit` with `forCurrentUser` as the annotation calls annotator screens may make.
- **`Resolution`** Javadoc follows decision 7.
- **`context/architecture.md`**'s blindness bullet says resolution happens inside `submit`'s action, and **`AnnotatorBlindnessTest`**'s note on `submit` adds that it reads the item's answers for resolution but still returns only the annotator's queue. Wording only; the trusted entry points do not change.
- **Documentation**, in the Accept step: "How blindness is enforced" in the Developer Guide says the query boundary never *returns* another annotator's work or a resolved result to the annotator, instead of "Resolved labels are never loaded on that path". The User Guide's Projects section says automatic resolution lowers the Unresolved count.

## Risks

- **Blindness.** `submit` now reads other annotators' answers and the item's resolution inside its action, but returns only the annotator's `QueueView`, and its signature has no `Resolution`, so `AnnotatorBlindnessTest` should pass unchanged.
- **One resolution per item** rests on `resolveAutomatically`'s check inside the single-writer action. `JsonIntegrity` does not enforce it and is left alone.
- **Decision 3** leaves items with *k* answers and no resolution in older workspaces. `ClassificationWorkflow` can still seed that state, so it is left alone, and its Javadoc says so.

## Verification

Automated coverage goes in a new `ResolutionServiceTest`: unit tests of `decide` built from `Records.answer` and `Records.scaleAnswer`, and integration tests that seed a `ClassificationWorkflow` in a `TestWorkspace` with *k* − 1 answers and submit the *k*th through `AnnotationService.submit`.

| Acceptance criterion | How it is verified |
| --- | --- |
| SINGLE strict majorities resolve with `MAJORITY`; ties and no-majority cases stay disputes | Unit: 2 of 3, 3 of 4 and 1 of 1 resolve to that label; 1–1, 2–2 and 1–1–1 give nothing. Integration: a 2–1 submit stores `MAJORITY` with the label; a 1–1 submit stores nothing. Human: on a k = 2 SINGLE project, two annotators agreeing on one file and disagreeing on another lower the Unresolved count by one |
| SCALE resolves with `AUTO_SCALE` to the unrounded mean; 2 and 3 give 2.5, 1 and 5 give 3 | Unit: both examples, and a negative range. Integration: a 2 and 3 submit stores `AUTO_SCALE` 2.5. Human: two ratings on a SCALE file lower the Unresolved count |
| Fewer than *k* answers leave an item unresolved; duplicates and out-of-range answers stay refused | Unit: *k* − 1 answers give nothing. Integration: with *k* = 3, the second answer stores nothing; with two files, both annotators answering file 1 resolves it while their assignments are still `IN_PROGRESS`. [#17]'s refusal tests in `AnnotationServiceTest` still leave `arbiter.json` unchanged |
| The method persists, and contributors are identifiable after restart | Integration: stored resolutions are read after `JsonStore.open` on the same workspace. The resolution has its method, value, a null `decidedByUserId` and the *k*th answer's `submittedAt`, even when an earlier answer is stamped later, and `listByItem` returns the *k* answers with their annotators. Human: the Unresolved count holds after restarting Arbiter |
| Resolution is atomic with [#17] and never creates a second resolution | It runs inside `submit`'s one action, and `JsonStoreTransactionTest` shows a failing action publishes nothing. Integration: calling `resolveAutomatically` again leaves one unchanged resolution, as does the *k*th submit on an item given a resolution beforehand |
| No detection, flag, draft, seal, retirement or analytics | Review of the diff |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar`, including `AnnotatorBlindnessTest` |

[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
