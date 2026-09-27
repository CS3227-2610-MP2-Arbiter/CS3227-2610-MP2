# Manual resolution

**Issue:** [#34] - Manually resolve single-label disputes
**Branch:** `feat/manual-resolution`, from `main` after PR #85 merged
**Status:** Approved by the owner on 27 September 2026. Implemented with `ResolutionServiceDisputeTest`; review fixes applied and `check shadowJar` passes. Accepted by the owner on 27 September 2026, and the guides are updated.

What the feature does is in [#34], and the rules it relies on are rules 10, 13, 17 and 21 in [UserFlows](../docs/UserFlows.md#3-rules-both-tracks-share). This plan records how the code will meet them and what is decided.

## Goal and scope

From a project's page, let the adjudicator list its label disputes and `ADJUDICATED` items, compare one item's source text with its anonymous answers, and save or replace an `ADJUDICATED` decision.

Non-goals: everything under "Out" in [#34].

## Decided

Decided by the owner on 27 September 2026:

1. A **Disputes** button beside **Taxonomy** on the project page opens an on-page list, as **Taxonomy** opens `TaxonomyView`. Each row opens a compare-and-decide view.
2. Only an `ADJUDICATED` decision can be replaced, and it stays listed with its label. `MAJORITY` and `AUTO_SCALE` results never change, and no decision can be cleared.
3. If the source cannot be read (rule 21), the view shows why instead of the text, and the service refuses to save.
4. Leaving an item unresolved is **Back**; nothing is stored.

Assumptions accepted by the owner:

- The list holds a SINGLE project's items with exactly *k* submitted answers and either no resolution or an `ADJUDICATED` one. Each row shows the file path, split, position and "Unresolved" or "Decided: ⟨key⟩", undecided rows first, then by split and position. An empty list says "This project has no disputes."; on a SCALE project the button is disabled with a hint.
- The view shows one card per answer holding only its label, in taxonomy order, with no name, time or count. The shared `AnnotationEditor` offers every label, and an existing decision shows as "Current decision: ⟨key⟩".
- **Save** is enabled once a label is picked, asks for no confirmation, and returns to the reloaded list. The decider is the signed-in adjudicator and the time is when it is saved; a replacement overwrites the same record.
- A refused save stores nothing, shows an error dialog and reloads, as `ProjectPage.commit` does. Elsewhere, the only visible effect is a lower Unresolved count on the project list.

Also decided by the owner on 27 September 2026, after planning:

5. An item that reached *k* answers with a strict majority before [#27] has no resolution but is not listed, so the list and the save use the same dispute test as `decide`.
6. With an unreadable source, the view still shows the answer cards but hides the picker and disables **Save**, as the annotator's queue does.
7. After a refused save, the page returns to the reloaded list, where reopening the item shows why.

## Current state

- `ResolutionService` is package-private, with the static `resolveAutomatically` and `decide` that `AnnotationService.submit` calls ([#27]). Nothing stores `ADJUDICATED`, although `ClassificationWorkflow.adjudicated` seeds it.
- `ResolutionRepository.save` updates a record that has an identifier in place, so a replacement stays one record.
- `AnnotationEditor` and `ItemView` take plain values. `QueueScreen` shows an unreadable file's reason in place of its text and disables its button.
- `ProjectPage` opens `TaxonomyView` in place with **Back**, and `ProjectPage.commit` handles refused changes.

## Proposed changes

- **`ResolutionService`** becomes a public class built from the store, `AuthService` and `WorkspacePaths`, like `AnnotationService`. Its two static methods stay as they are. Each new method first requires the adjudicator, and all three share one private check of whether an item is listed: a SINGLE item with *k* answers whose resolution is `ADJUDICATED`, or that has no resolution and gets none from `decide`.
  - `disputes(projectId)` returns the project's listed items, in list order, as `DisputeSummary` records.
  - `comparison(itemId)` reads the item, its answers' label keys, its current decision and the taxonomy (`CorpusService.taxonomyOf`) in one read. It then reads the source outside that read, as `AnnotationService` does, and returns a `DisputeComparison`. It refuses an item that is not listed.
  - `adjudicate(itemId, labelId)` checks inside one `store.write` that the item is listed, the label is in its project's taxonomy (`TaxonomySummary.accepts`) and its source is readable. It then saves an `ADJUDICATED` resolution with that label, the adjudicator and the current time, updating the item's existing resolution if it has one. Every refusal is a `ProjectException` and stores nothing.
- **`DisputeSummary`** (item id, path, split name, position, decision key or null) and **`DisputeComparison`** (path, text or the resolver's failure message, answer keys, decision key or null, taxonomy) are new records in `arbiter.service`. Neither holds an annotator, a time or a `Resolution`. The failure message is `SourceException`'s own, which names the file, since the adjudicator may see it.
- **`DisputesView`** (new, `arbiter.ui.adjudicator`) shows the list and, for a chosen row, the comparison: `ItemView` or the failure message, the answer cards, the current decision, an `AnnotationEditor`, **Save** and **Back**. Paths and keys wrap.
- **`ProjectPage`** gains **Disputes**, and **`ProjectsScreen`** and **`Arbiter`** pass the new service through.
- **Wording:** the `AnnotationEditor` and `Answer` Javadoc and the shared-code bullet in `context/architecture.md` say the editor holds the current choice until it is submitted or saved, not only the annotator's. The `ResolutionService` Javadoc names manual resolution.
- **Documentation**, in the Accept step: the User Guide's Projects section drops "until manual resolution ([#34]) is released" and says how to resolve a dispute.

The model, repositories and `JsonIntegrity` do not change.

## Risks

- **Blindness.** The new reads return every annotator's answers, but only to the adjudicator and only from `arbiter.ui.adjudicator`, which `AnnotatorBlindnessTest` excludes. The records hold no `Resolution` or annotator, and the shared editor still takes only a `TaxonomySummary`, so the test should pass unchanged.
- **One resolution per item** still rests on checks inside the single-writer action, because `adjudicate` updates the existing record instead of adding one.
- **`adjudicate` reads the source inside its write action**, as `submit` does, so a large file holds the action briefly.
- **The view needs JavaFX, which CI lacks,** so it is covered by human checks.

## Verification

Automated coverage goes in a new `ResolutionServiceDisputeTest`, beside `ResolutionServiceTest` as `CorpusServiceTaxonomyTest` is beside `CorpusServiceTest`. It seeds `ClassificationWorkflow` projects, with `majority` and `adjudicated` items, in a `TestWorkspace`. Every new method refuses a signed-out or annotator caller, and every refusal leaves the data file's bytes unchanged.

| Acceptance criterion | How it is verified |
| --- | --- |
| The queue includes disputes and `ADJUDICATED` items, and excludes SCALE, `MAJORITY` and fewer-than-*k* items | Integration: on a *k* = 2 project with a 1–1 tie, a `majority` item, an `adjudicated` item and an item with one answer, `disputes` lists only the tie, as undecided, and then the adjudicated item with its key. A SCALE project with *k* ratings lists nothing. Per decision 5, a majority item with no resolution is left out. Human: the list, its order across two splits, the empty message, and the disabled button on a SCALE project |
| Comparison shows the source text, or why it cannot be read, and the *k* labels anonymously | Integration: `comparison` returns the text and the *k* keys in taxonomy order, whatever order they were submitted in. After the source is edited or deleted, it returns the resolver's message and no text. Review: neither record holds an annotator or a time. Human: the text, cards with no names, and a changed file's message with **Save** disabled |
| The adjudicator can select a submitted label or another label, or leave the item unresolved, which stores nothing | Integration: `adjudicate` stores a submitted label on one item and a label no one chose on another. `disputes` and `comparison` leave the data file unchanged. Human: **Back** from a comparison leaves the row "Unresolved" |
| Label, `ADJUDICATED`, decider and time persist after restart; contributors are identifiable | Integration: after `adjudicate`, a reopened store has one `ADJUDICATED` resolution with the label, the owner as decider and a time within the call. `listByItem` still returns the *k* answers with their annotators, and `ProjectService.list` counts one fewer Unresolved. Human: after a restart, the row and the project list's Unresolved count hold |
| Creating or replacing never edits or deletes a submitted answer and needs no history | Integration: a second `adjudicate` leaves one resolution, with the same identifier and the new label, decider and time. The item's answers are equal to those before either call |
| No detection, box editing, rationale, flags, return or provenance screen | Review of the diff |
| Only the adjudicator can open the queue or store a decision; the write re-checks item, label and source | Integration: annotator and signed-out callers of all three methods get `AuthException`. `adjudicate` refuses a `MAJORITY`, `AUTO_SCALE`, fewer-than-*k* or unknown item, another project's or an unknown label, and a source changed after `comparison` read it, each leaving the data file unchanged. Human: editing the file while its comparison is open, then choosing **Save**, shows an error and stores nothing |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar`, including `UiConventionTest` and `AnnotatorBlindnessTest` over the new view |
| No text is cut off (`review` skill) | Human: a 100-character label key, a long nested file path and a long file show in full in the list, the cards, the picker, "Current decision" and the error dialog |

[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
