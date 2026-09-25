# Project list and output format

**Issues:** [#24] - Create and list text-classification projects; [#30] - Choose CSV or JSON output
**Branch:** `feat/project-list` (from `main` after [#8] merged)
**Status:** Plan approved by the owner on 25 September 2026. Implemented with `ProjectServiceTest`, reviewed, and accepted by the owner on 25 September 2026; `check shadowJar` passes and the User Guide is updated. The pull request remains. Deviation: the UI kit gained a `page` style class and `Components.page`, because the list screen needs padding and spacing, which only the stylesheet may set.

What the features do is in [#24] and [#30], and the rules they rely on are rules 3, 4 and 5 in [UserFlows](../docs/UserFlows.md#3-rules-both-tracks-share). This plan records how the code will meet them and what is decided.

## Goal and scope

Replace the adjudicator's Projects placeholder with a screen that lists projects, creates a project with a fixed taxonomy kind and output format, and deletes a project before its first assignment. One PR closes both issues.

Non-goals, from [#24] and [#30]: opening a project page, renaming, editing the description, the SCALE range ([#26]), and writing export files ([#37]).

## Decided

Decided by the owner on 25 September 2026:

- [#24] and [#30] ship on this branch in one PR that closes both.
- The delete confirmation names only the project, as "Delete project X? This cannot be undone.", through `Dialogs.confirm`.
- Opening a project page, renaming and editing the description are out of scope.
- The screen uses the merged UI kit in `arbiter.ui.shared`, so no dialog is built outside `Dialogs` (`UiConventionTest`).
- The design choices and format descriptions below are approved as proposed.

Assumptions recorded during clarification:

- The unresolved count is the project's items with no `Resolution` record. The assignment count includes every status.
- Names are trimmed, compared ignoring case, and reusable after deletion. A blank description is stored as none.
- The list is in creation order and shows name, taxonomy kind, output format and the four counts. With no projects, it shows an empty state with a create action.
- Delete is disabled on a project with any assignment. The service enforces the guard regardless.

## Current state

- `Project`, `TaxonomySettings`, `OutputFormat` and `TaxonomyKind` exist. The SCALE range stays null until [#26].
- The project and taxonomy-settings repositories exist, and the JSON project delete already removes every project-owned record without touching `media/`. The item, split, assignment and resolution repositories can already supply the counts.
- There is no `ProjectService` and no `arbiter.ui.adjudicator` package. `Arbiter` registers a Projects placeholder.

## Proposed changes

- **`ProjectService`** in `arbiter.service` owns creating, listing and deleting projects. Each call first requires the signed-in adjudicator through `AuthService`.
  - Create validates the input, then in one store write checks the name is unused and saves the `Project` with its `TaxonomySettings`.
  - List reads each project's kind and counts in one store read and returns a small summary record per project.
  - Delete checks, inside the write action, that the project still exists and none of its splits has an assignment, then uses the existing cascade.
  - Rejections throw a new `ProjectException`, whose messages are shown to the user under [#8]'s convention.
  - It offers no way to change a project's kind or format.
- **A Projects screen** in a new `arbiter.ui.adjudicator` package, built from the UI kit: a table of summaries with a Delete action per row, a New project button, and the empty state. The create form replaces the table in the same content area. A rejected create shows inline on the form, and a rejected delete through `Dialogs.showError`. The screen reloads the list after every action.
- **The create form** has name, optional description, taxonomy kind and output format, and nothing else. The format choice lists `OutputFormat`'s values, each with a one-line description.
- **`Arbiter`** builds `ProjectService` from the open store and the signed-in `AuthService`, and registers the Projects screen in place of the placeholder.
- **Documentation**, in the Accept step: the User Guide stops calling Projects a placeholder and says how to create and delete a project.

Format descriptions:

- CSV: "A table that opens in a spreadsheet."
- JSON: "Structured records for scripts and other tools."

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| Counts computed in `ProjectService` from existing repository reads | New count queries on the repositories | What counts as unresolved is a business rule, which belongs in a service, and the existing reads suffice for a local snapshot |
| The create form inline in the Projects content area | A modal form dialog | `Dialogs` has no form, only it may build dialogs, and an inline form suits the convention's inline errors |
| The service re-checks the guard and the name inside the write action, and the screen reloads after each action | Version stamps on projects | Only one instance writes a workspace (rule 11), so a re-check inside the write action is enough |

## Risks

- **Blindness.** The list reads resolutions and every assignment on a split, so annotator-facing code must never call it. `AnnotatorBlindnessTest` fails if it does.
- **Kind stays fixed only if [#26] keeps kind changes out of scope** when it saves `TaxonomySettings` again.
- **The app cannot yet make items, splits or assignments** ([#25], [#28], [#32]), so non-zero counts and the assigned-project guard are covered by automated tests on seeded data only.
- **The screen needs JavaFX, which CI lacks,** so it is covered by human checks.

## Verification

Automated coverage goes in a new `ProjectServiceTest`, seeded with `TestWorkspace` and `ClassificationWorkflow`.

| Acceptance criterion | How it is verified |
| --- | --- |
| [#24] Creation requires a valid unique name, a kind and a format, and stores the description and creation time | Test: names blank, 1, 100 and 101 characters after trimming; a duplicate differing only in case; a name reused after deletion; no kind or no format; a blank description stored as none; the creation time set; the project and its settings both present after reopening. Human: a duplicate name shows an inline error |
| [#24] No task-type or source-type selector | Human: the form has only name, description, kind and format |
| [#24] Kind and format cannot change after creation; [#30] the stored format cannot change and export offers no override | Review: `ProjectService` has no update, and the screen shows both as text. Export is [#37]'s, and no export code exists yet |
| [#24] The list shows item, split, assignment and unresolved counts | Test: a seeded project with items, a split, assignments in each status and some resolved items gives the expected counts, unaffected by a second project. Human: a new project shows zeros, and with none the empty state appears |
| [#24] Before the first assignment, confirmed deletion removes project-owned records and no source files | Test: deleting a project with items, labels and a split removes all its records and leaves other projects, accounts and source files unchanged. Human: the confirmation reads "Delete project X? This cannot be undone."; Cancel keeps the project, and confirming removes it while its `media/` file remains |
| [#24] From the first assignment, the service rejects deletion after restart and from stale screens | Test: with an assignment that is not started, or whose annotator is disabled, deletion is rejected and nothing changes, including through a new service on a reopened store and after a list read taken before the assignment existed. A signed-out or annotator caller is rejected |
| [#30] Creation offers exactly CSV and JSON and stores the choice | Test: each value is stored and read back. Human: the choice lists exactly CSV and JSON |
| [#30] A concise description explains each choice | Human: each format shows its approved description |
| [#30] No COCO or task/source compatibility branch | Review: `OutputFormat` has only CSV and JSON, and `src` mentions no COCO |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar`, including `UiConventionTest` and `AnnotatorBlindnessTest` over the new screen |

[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
[#24]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/24
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
[#28]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/28
[#30]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/30
[#32]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/32
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
