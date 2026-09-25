# Corpus registration

**Issue:** [#25] - Register plain-text corpus files
**Branch:** `feat/corpus-registration` (from `main` after [#24] merged)
**Status:** Plan approved by the owner on 25 September 2026. Implemented with `CorpusServiceTest` and reviewed; acceptance remains.

What the feature does is in [#25], and the rules it relies on are rules 3, 5, 14 and 21 in [UserFlows](../docs/UserFlows.md#3-rules-both-tracks-share). This plan records how the code will meet them and what is decided.

## Goal and scope

Let the adjudicator open a project from the Projects screen, see its registered items, register selected `.txt` files from `media/` as one atomic set, and unregister an item before the project's first assignment.

Non-goals, from [#25]: folder or recursive import, dry runs, background progress, import history and any change to a source file. Labels ([#26]) and splits ([#28]) join the project page later.

## Decided

Decided by the owner on 25 September 2026, and reflected in [#25]:

- An **Open** action on each project row leads to a project page with a table of registered paths, **Add files…**, **Unregister** per row and **Back**. This lifts [#24]'s non-goal of opening a project page.
- Any rejection fails the whole selected set, stores nothing and shows the first failure's message.
- Unregister handles one item at a time, after a `Dialogs.confirm` naming its file.
- Rule 14 gains an exception for unregistering an item under rule 5.
- Files are chosen with a JavaFX `FileChooser`: several at once, opening in `media/`, filtered to `*.txt`.

Assumptions:

- The page lists items in registration order and shows only the stored path.
- Cancelling the chooser does nothing. A registered set shares one import time, and success reports how many files were registered through `Dialogs.showSuccess`.
- A registered path is the same path only if the stored strings are identical.
- **Add files…** and **Unregister** are disabled on a project with any assignment. The service enforces the freeze regardless.

## Current state

- `Item` records path, hash and import time, which the store requires. `ItemRepository` can already save, list, find by hash and delete; its delete also removes answers and a resolution, though its Javadoc names only split membership.
- `SourceResolver.resolveForImport` applies every [#10] check and returns the hash to store, but nothing turns a chosen file into a stored path.
- `FirstAssignment` is the shared freeze check, and `ProjectService` shows the service pattern.
- There is no `CorpusService`, although the architecture context names it as the owner of import. `Arbiter` builds no resolver, and the Projects screen cannot open a project.
- `TestWorkspace` and `ClassificationWorkflow` can seed sources, items and a split with or without assignments.

## Proposed changes

- **`CorpusService`** in `arbiter.service` lists, registers and unregisters a project's items. Each call first requires the signed-in adjudicator through `AuthService`.
  - List returns the project's items.
  - Register takes the chosen files. In one store write it checks that the project exists and has no assignment (`FirstAssignment`), then checks each file through `resolveForImport` and rejects content already in the project or earlier in the selection, and a registered path whose bytes changed. The first failure throws, so the items are saved only if every file passes.
  - Unregister checks, inside the write action, that the item still exists and its project has no assignment, then uses the existing item delete.
  - Rejections throw `ProjectException`, and [#10] failures stay `SourceException`. Both are shown to the user under [#8]'s convention.
- **`SourceResolver.resolveForImport`** takes the chosen file and makes its stored path, relative to the workspace root with `/` between segments, before its existing checks. It also refuses a file outside `media/`, or one chosen by a name spelled differently from the one on disk, as a `SourceException`.
- **A project page** in `arbiter.ui.adjudicator` replaces the list in the Projects content area, as the create form does, with the controls decided above. A rejected set or unregister shows through `Dialogs.showError`, the page reloads after every action, and **Back** reloads the list.
- **The Projects screen** gains an **Open** action on each row.
- **`Arbiter`** builds `CorpusService` from the store, `AuthService` and the workspace's paths, and passes it to the Projects screen.
- **`ItemRepository.deleteById`'s Javadoc** names every record it removes and leaves when it is allowed to the services.
- **Rule 14** in UserFlows gains the exception decided above.
- **Documentation**, in the Accept step: the User Guide stops saying a project cannot be opened, and says how to open a project, add files and unregister one.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| The screen passes chosen files to the service, and `SourceResolver` makes the stored path | The screen or `WorkspacePaths` builds the stored path | The stored-path format keeps one home, and the conversion is tested without JavaFX |
| The project page swaps into the Projects content area | A new `ScreenRoute` | A route is a shell destination with no project, and the create form already swaps in this way |
| Rejections reuse `ProjectException` | A new `CorpusException` | The page handles both the same way, and a corpus belongs to its project |

## Risks

- **Stored-path case.** Registration refuses a name typed in a case other than the one on disk (`SourceResolver`'s Javadoc), but a file renamed to another case afterwards still reads on a case-insensitive disk and fails on a case-sensitive one.
- **Unregister can leave a split with a gap in its order, or empty.** [#28] must accept both.
- **The app cannot yet make splits or assignments** ([#28], [#32]), so unregistering from a split and the freeze are covered by automated tests on seeded data only.
- **[#10]'s messages say "recorded source" even at registration.** They still name the file and the reason, so they stay as they are.
- **The page needs JavaFX, which CI lacks,** so it is covered by human checks.

## Verification

Automated coverage goes in a new `CorpusServiceTest`, seeded with `TestWorkspace` and `ClassificationWorkflow`, and in `SourceResolverTest` for the stored path.

| Acceptance criterion | How it is verified |
| --- | --- |
| The adjudicator opens a project from the project list and sees its registered items | Test: list returns only that project's items, in registration order. Human: **Open** shows the project's paths, a new project shows an empty table, and **Back** returns to the list with the new item count |
| Selecting valid `.txt` files under `media/` registers one item per unique file with relative path and content hash | Test: nested files register with `media/` paths using `/`, the hash `resolveForImport` returns and one import time, and each reads back through `SourceResolver.resolve` after reopening the store. Human: **Add files…** opens in `media/`, filters to `.txt` and accepts several files |
| Outside-root, traversal, escaping link/junction, missing, unreadable, non-text or changed inputs fail clearly through [#10] | Test: a file outside the workspace or elsewhere in it, a `..` path, a link leaving `media/`, a missing file, a non-`.txt` file and invalid UTF-8 each fail the whole set with a `SourceException` naming the file, and a registered path whose bytes changed is rejected. `SourceResolverTest` already covers every reason, including unreadable files and junctions. Human: a file outside `media/` shows a clear error |
| Registration copies, moves, renames, edits or deletes no source file | Test: after registering and unregistering, `media/` holds the same files with the same bytes. Review: nothing in the change writes under `media/` |
| Re-registering the same content in one project is rejected and stores nothing; another project may register it | Test: the same file again, a copy at another path, and two identical files in one selection are each rejected with nothing stored; a second project registers the same file |
| The selected set commits atomically and synchronously with a concise result; no folder recursion, dry run, background progress or import history exists | Test: a set whose last file fails leaves `arbiter.json` unchanged byte for byte. Human: success reports the number of files registered. Review: the page has no folder chooser, background task or history |
| Before the first assignment, a confirmed unregister removes the database item and any never-assigned membership only | Test: unregistering an item in a seeded never-assigned split removes it and its membership, and leaves the other items, the split, the labels, other projects and the source file unchanged; an item already removed is rejected. Human: the confirmation names the file, Cancel keeps it, and confirming removes the row |
| After the first assignment, every corpus write is rejected after restart and from stale screens | Test: with an assignment that is not started, or whose annotator is disabled, register and unregister are rejected and nothing changes, including through a new service on a reopened store and after a list read taken before the assignment existed. A signed-out or annotator caller is rejected by every method |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar`, including `UiConventionTest` and `AnnotatorBlindnessTest` over the new page |

[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#24]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/24
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
[#28]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/28
[#32]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/32
