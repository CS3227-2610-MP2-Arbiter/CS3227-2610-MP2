# Basic project progress dashboard

**Issue:** [#33] - Basic project progress dashboard
**Branch:** `feat/project-progress`, from `main` at `4bb3b05`
**Status:** Implemented and accepted by the human; PR [#89] ready for review.

The behavior and exclusions live in [#33]. This plan records the implementation choice and verification, using rules 1, 2, 13 and 19 in [UserFlows](../docs/UserFlows.md#3-rules-both-tracks-share).

## Decision and scope

The owner chose **currently assigned work** as the denominator on 27 September 2026: for 10 files, *k* = 3 and two assigned annotators, show 20 answer opportunities, not 30. Show the one vacant annotator place separately. Before a split's first assignment, *k* is unset; show it as unassigned rather than treating the form's default as saved. Count all submitted answers from immutable records, including answers by disabled annotators.

Add a read-only **Progress** view on each adjudicator project page. Its project totals, split rows, and annotator rows include the #33 counts and assignment status; the view links to the existing assignment and dispute screens. A **Refresh** action reloads the snapshot after submissions or decisions. The project list keeps its existing item and unresolved columns. No analytics, activity history, or editing is in scope; follow [#33]'s Out section.

## Implemented changes

- Add `ProjectService.progress(projectId)` with an adjudicator check and an unknown-project error. One `store.read` obtains project items, splits, split memberships, assignments, accounts, submitted annotations and resolutions. Return immutable service records for project totals, per-split totals and per-annotator totals, with an assignment row for each annotator/split showing its stored status and submitted/total counts. Aggregate by account ID, and include disabled accounts with their current status. Keep all counting in the service, never the JavaFX view.
- For each assignment, total is its split's item count and submitted is its immutable annotations on those split items. Sum these assignment totals for split, annotator and project progress. Show split item count and assigned/*k* places separately. Reuse `ProjectService.unresolvedCount(session, items)` for all project items lacking a resolution, including unassigned and incomplete work.
- Reuse `ResolutionService`'s existing dispute predicate inside the same read session through a small package-private helper. Count only undecided SINGLE disputes; its public `disputes` list also includes decided rows and cannot be used as the count. Do not infer a dispute merely from *k* answers and no stored resolution.
- Add `ProgressView` under `arbiter.ui.adjudicator`, opened from `ProjectPage` like `DisputesView`. Show project totals above split and annotator tables; show per-assignment status in the split detail/assignment rows. **Assign** opens the existing form for a selected split and **Disputes** opens the existing view for SINGLE projects. Opening or refreshing Progress reads current repository state; returning from assignment or resolution and reopening it does too. Wire `ProjectService` through `ProjectsScreen` to `ProjectPage`. Use existing components, wrapping text and error handling.
- After owner acceptance, update the User Guide's Projects section and the architecture context with the navigation and the service enforcement location, linking to [#33] rather than copying its requirements. No persistence schema, model, annotation write flow or repository interface change is planned.

## Verification

| #33 acceptance area | Evidence |
| --- | --- |
| Project counts, split counts, assignment status and annotator progress | Service integration test with unassigned items, multiple splits, a partly filled *k*, and one annotator assigned to two splits. Check the 20/30 denominator example, vacant places, stored statuses and aggregation. Human check of the Progress tables and navigation to **Assign**. |
| Immutable submissions and disabled accounts | Submit through `AnnotationService`, disable an account, and read Progress again; its earlier answers and assignments remain counted and identified as disabled. Compare data before and after the read to confirm no write. |
| Unresolved items and SINGLE disputes | Mixed fixture: unsplit, incomplete, majority, undecided dispute and adjudicated items; compare unresolved with `ProjectService.list` and export semantics, and dispute count with the undecided subset of #34. Check SCALE shows zero disputes. Human check that the link reaches Disputes and unresolved non-disputes are labeled correctly. |
| Access and refresh | Service test rejects signed-out and annotator callers and unknown projects. `AnnotatorBlindnessTest` covers the new UI path. Human check that a successful submission or resolution is reflected when Progress is reopened or **Refresh** is used, with no stale cached number. |
| Excluded metrics and build | Diff review against [#33]'s Out section; JDK 25 `.\gradlew.bat check shadowJar`, including `UiConventionTest`. Human check empty data and long names/paths at narrow window widths. |

## Review and delivery notes

- The dashboard exposes cross-annotator counts. The service read is adjudicator-gated and the view is in `arbiter.ui.adjudicator`; no progress record passes through shared or annotator UI components.
- Automated verification passed under JDK 25: offline `check shadowJar` completed with 459 tests passed, six existing platform skips, clean Checkstyle reports and a rebuilt jar. All eight focused progress tests and three JavaFX layout regressions passed. See the [task log](../logs/Whimsyturtle/027-monitor-basic-project-progress.md) for the detailed run record.
- During acceptance, the project page's file and split tables collapsed at a small window height. The affected table pages now scroll, tables retain usable height, and wrapped annotation choices retain their full height. The task log records the reproduction.
- The owner requested a compact project-page action row. **Taxonomy**, **Progress**, **Export** and **Disputes** now share a wrapping row, which was included in human acceptance testing.
- The human reported acceptance testing passed on 27 September 2026. The User Guide, architecture context, plan and log updates were included in commit `53589d9` on PR [#89].

[#33]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/33
[#89]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/pull/89
