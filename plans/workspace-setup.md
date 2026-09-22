# Workspace setup and file layout

**Issue:** [#9] - Workspace setup and file layout
**Branch:** `feat/workspace-setup`
**Status:** Revised after Whimsyturtle's review of the first revision; awaiting re-review.

What the feature does is in [#9], and what each class means is in its Javadoc. This plan records what was decided along the way and what is still open.

## Decisions

- **`WorkspacePaths` resolves every path inside a workspace**, so no feature joins folder or file names itself.
- **Two versions are recorded.** `workspaceVersion` covers the layout and `workspace.json`; `schemaVersion` covers the database and belongs to [#6]. Either can change without the other.
- **The wizard lives in `arbiter.ui.shared`**, as start-up shell behaviour. It cannot use [#8]'s UI kit, which has not shipped.
- **`Arbiter.start` is changed minimally**, because [#5] replaces it.

### Changed in review

- **No remembered workspace.** The user chooses to create or open a workspace at every launch, so the recent-workspace list and automatic reopening were removed, and [#9] was updated to match.
- **JSON through Jackson** instead of a hand-written codec. Unknown fields are ignored so that a newer workspace reaches the version check; missing fields are refused.
- **One exception type**, `WorkspaceException`, instead of a separate metadata exception.
- **`WorkspacePaths` and `WorkspaceMetadata` are records.**

## Open questions

- **UI ownership.** `arbiter.ui.shared` is zheng-jj's package ([#5]); the wizard is a new file there.
## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| The wizard creates the full layout, or opens a workspace the user chooses | Unit tests: `create` makes every folder and file; `open` reopens a workspace without rewriting it |
| Metadata records both versions | Unit test reads `workspace.json` back |
| Multiple workspaces can be created and opened | Unit test creates two workspaces and opens each |
| A newer workspace fails clearly and is left untouched | Unit test: a workspace stamped with a higher version and an extra field is refused by version and left byte-identical |
| Paths come from a single shared object | Unit test checks each path `WorkspacePaths` returns |
| A relative path still resolves after the workspace moves | Unit test: a moved workspace opens with its `media/` folder under the new root |
| `./gradlew check` passes | `./gradlew check shadowJar` |

The JavaFX wizard is not unit-tested: it needs a display, and it only calls `WorkspaceService`.

[#5]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5
[#6]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6
[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
[#9]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/9
